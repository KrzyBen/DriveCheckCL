import json
import os
import shutil
from datetime import datetime, timezone
from typing import Callable, Optional

from sqlalchemy.orm import Session, joinedload

from config.configEnv import ACCESS_TOKEN_SECRET
from config.consentimiento import CONSENTIMIENTO_VERSION_VIGENTE
# Todas las entidades se importan aquí a propósito: SQLAlchemy resuelve las relaciones por
# nombre (Reporte -> User, etc.) y fuera de la API (el script de exportación) nadie más las importa.
from entity.user_entity import User  # noqa: F401
from entity.reporte_entity import Reporte, VideoReporte, EstadoReporte  # noqa: F401
from entity.reporte_validacion_entity import ReporteValidacion  # noqa: F401
from entity.password_reset_entity import PasswordResetCode  # noqa: F401
from entity.notificacion_entity import Notificacion  # noqa: F401
from entity.dataset_clip_entity import DatasetClip
from helpers import dataset_helper as h
from helpers.storage_helper import get_storage_root, get_ruta_absoluta

PREFIJO_PRUEBA = "[PRUEBA]"  # el mismo que usa seed_eventos_prueba.py

# Copia un clip ya anonimizado: (ruta_origen, ruta_destino) -> None. Si no hay
# anonimizador no se copia ningún video: un video sin anonimizar nunca llega a clips/.
Anonimizador = Callable[[str, str], None]


def carpeta_dataset() -> str:
    return os.path.join(get_storage_root(), "dataset")


def motivo_exclusion(reporte: Reporte) -> Optional[str]:
    """Por qué un reporte no puede formar parte del dataset (None si puede)."""
    if reporte.estado != EstadoReporte.aprobado:
        return "no_aprobado"
    if (reporte.titulo or "").startswith(PREFIJO_PRUEBA):
        return "prueba"
    usuario = reporte.usuario
    if (usuario is None or not usuario.consentimiento_estudio
            or usuario.consentimiento_version != CONSENTIMIENTO_VERSION_VIGENTE):
        return "sin_consentimiento"
    return None


def reportes_con_todo(db: Session) -> list:
    return (
        db.query(Reporte)
        .options(joinedload(Reporte.videos), joinedload(Reporte.infracciones),
                 joinedload(Reporte.usuario), joinedload(Reporte.validacion))
        .order_by(Reporte.id)
        .all()
    )


def filas_por_video(db: Session) -> dict:
    filas = db.query(DatasetClip).options(joinedload(DatasetClip.infracciones)).all()
    return {f.video_id: f for f in filas}


def leer_exportados(destino: Optional[str] = None):
    """(registros por clip_id, resumen.json o None) de la última exportación."""
    destino = destino or carpeta_dataset()
    registros = {}
    try:
        with open(os.path.join(destino, "metadata.jsonl"), encoding="utf-8") as archivo:
            for linea in archivo:
                if linea.strip():
                    registro = json.loads(linea)
                    registros[registro["clip_id"]] = registro
    except (OSError, ValueError, KeyError):
        registros = {}
    try:
        with open(os.path.join(destino, "resumen.json"), encoding="utf-8") as archivo:
            resumen = json.load(archivo)
    except (OSError, ValueError):
        resumen = None
    return registros, resumen


def exportar_dataset(db: Session, destino: Optional[str] = None,
                     anonimizador: Optional[Anonimizador] = None,
                     secreto: Optional[str] = None) -> dict:
    """
    Reconstruye por completo <storage>/dataset/ (metadata.jsonl, README.md, resumen.json y clips/).
    Cada corrida parte de cero: quien revocó su consentimiento desaparece del dataset, y los
    clips excluidos a mano no entran. Devuelve un resumen con conteos y motivos de exclusión.
    """
    secreto = secreto or ACCESS_TOKEN_SECRET
    if not secreto:
        raise RuntimeError("Falta ACCESS_TOKEN_SECRET: se usa para seudonimizar al conductor")
    destino = destino or carpeta_dataset()
    carpeta_clips = os.path.join(destino, "clips")
    os.makedirs(carpeta_clips, exist_ok=True)

    filas = filas_por_video(db)
    excluidos = {"no_aprobado": 0, "prueba": 0, "sin_consentimiento": 0, "sin_video": 0}
    registros = []
    sin_anonimizar = 0
    clips_excluidos = 0

    for reporte in reportes_con_todo(db):
        motivo = motivo_exclusion(reporte)
        if motivo:
            excluidos[motivo] += 1
            continue

        ia = h.limpiar_ia(reporte.validacion.resultados_ia if reporte.validacion else None)
        registros_reporte = []
        hubo_excluidos = False

        for video in sorted(reporte.videos, key=lambda v: v.orden):
            fila = filas.get(video.id)
            if fila is not None and fila.excluido:
                clips_excluidos += 1
                hubo_excluidos = True
                continue
            origen = get_ruta_absoluta(video.video_path)
            if not os.path.isfile(origen):
                continue
            documento = h.leer_metadatos_clip(get_ruta_absoluta(h.ruta_json_de(video.video_path)))
            severidad, infracciones, _ = h.etiquetas_efectivas(reporte, fila)
            registro = h.armar_registro(
                reporte=reporte, video=video, usuario_id=reporte.usuario_id, secreto=secreto,
                documento=documento, ia=ia, severidad=severidad, infracciones=infracciones,
            )
            if anonimizador is None:
                sin_anonimizar += 1
            else:
                nombre = f"{registro['clip_id']}.mp4"
                anonimizador(origen, os.path.join(carpeta_clips, nombre))
                registro["archivo"] = f"clips/{nombre}"
                registro["anonimizado"] = True
            registros_reporte.append(registro)

        if not registros_reporte:
            if not hubo_excluidos:
                excluidos["sin_video"] += 1
            continue
        registros.extend(registros_reporte)

    _escribir_metadata(destino, registros)
    _podar_clips(carpeta_clips, {r["archivo"].split("/")[-1] for r in registros if r["archivo"]})

    resumen = {
        "clips": len(registros),
        "eventos": len({r["evento_id"] for r in registros}),
        "conductores": len({r["conductor_id"] for r in registros}),
        "sin_anonimizar": sin_anonimizar,
        "clips_excluidos": clips_excluidos,
        "excluidos": excluidos,
        "destino": destino,
    }
    _escribir_readme(destino, resumen)
    _escribir_resumen_json(destino, resumen)
    return resumen


def _escribir_metadata(destino: str, registros: list) -> None:
    temporal = os.path.join(destino, "metadata.jsonl.tmp")
    with open(temporal, "w", encoding="utf-8") as archivo:
        for registro in registros:
            archivo.write(json.dumps(registro, ensure_ascii=False) + "\n")
    os.replace(temporal, os.path.join(destino, "metadata.jsonl"))


def _escribir_resumen_json(destino: str, resumen: dict) -> None:
    contenido = {k: v for k, v in resumen.items() if k != "destino"}
    contenido["generado"] = datetime.now(timezone.utc).isoformat()
    contenido["version_consentimiento"] = CONSENTIMIENTO_VERSION_VIGENTE
    temporal = os.path.join(destino, "resumen.json.tmp")
    with open(temporal, "w", encoding="utf-8") as archivo:
        json.dump(contenido, archivo, ensure_ascii=False, indent=2)
    os.replace(temporal, os.path.join(destino, "resumen.json"))


def _podar_clips(carpeta_clips: str, vigentes: set) -> None:
    """Borra de clips/ todo lo que no esté en el manifest actual (p. ej. quien revocó)."""
    for nombre in os.listdir(carpeta_clips):
        if nombre not in vigentes:
            ruta = os.path.join(carpeta_clips, nombre)
            if os.path.isdir(ruta):
                shutil.rmtree(ruta)
            else:
                os.remove(ruta)


def _escribir_readme(destino: str, resumen: dict) -> None:
    ahora = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    if resumen["sin_anonimizar"]:
        estado_videos = (
            f"**Esta exportación no incluye videos**: {resumen['sin_anonimizar']} clips "
            "están a la espera del paso de anonimización (rostros y patentes de terceros, "
            "sin audio). Hasta entonces `clips/` queda vacía y `anonimizado` es `false`."
        )
    else:
        estado_videos = "Todos los videos de `clips/` fueron anonimizados antes de copiarse."
    texto = f"""# Dataset DriveCheckCL: infracciones e imprudencias de tránsito

Generado: {ahora} · versión del consentimiento: {CONSENTIMIENTO_VERSION_VIGENTE} · esquema: {h.ESQUEMA}

## Alcance y uso
- **Uso académico y acceso restringido.** No redistribuir ni publicar.
- Solo contiene clips de conductores que autorizaron expresamente su uso en un estudio
  (casilla opcional de la app), con reporte aprobado por el equipo validador. Quien revoca
  su consentimiento sale del dataset en la siguiente exportación.
- Las coordenadas son exactas. El inicio de un clip puede estar cerca de un lugar sensible.
- {estado_videos}

## Contenido de esta exportación
- Clips: {resumen['clips']} · eventos (reportes): {resumen['eventos']} · conductores (seudonimizados): {resumen['conductores']}

## Formato (`metadata.jsonl`, un registro JSON por clip)
| Campo | Significado |
|---|---|
| `clip_id`, `evento_id`, `orden` | Identificador del clip, reporte al que pertenece y posición dentro de él |
| `conductor_id` | Seudónimo estable del conductor; no permite identificarlo |
| `archivo`, `anonimizado` | Ruta dentro de `clips/` (o `null`) y si el clip pasó por la anonimización |
| `grabado_inicio`, `grabado_fin` | Fecha y hora de la grabación (ISO-8601 con zona horaria) |
| `inicio`, `fin` | GPS del comienzo y del fin del clip: `lat`, `lon`, `precision_m` |
| `severidad` | Severidad del clip (`leve`, `moderada`, `grave`) |
| `infracciones` | Etiquetas del clip, del catálogo de infracciones (`id`, `articulo`, `descripcion`) |
| `ia` | Resultados automáticos por capa (`estado`, `hallazgo`, `confianza`, `video`). Sin patentes |

## Qué no se exporta
Nombre, correo, RUT, patentes, comentarios, notas del administrador, infracciones escritas
a mano y cualquier texto libre.
"""
    with open(os.path.join(destino, "README.md"), "w", encoding="utf-8") as archivo:
        archivo.write(texto)
