import fcntl
import json
import os
import shutil
from contextlib import contextmanager
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
from helpers.anonimizador_client import SUFIJO_PARCIAL, AnonimizacionFallida, ServicioNoDisponible
from helpers.storage_helper import get_storage_root, get_ruta_absoluta

PREFIJO_PRUEBA = "[PRUEBA]"  # el mismo que usa seed_eventos_prueba.py

# Deja en destino el clip anonimizado: (video_path relativo al storage, ruta_destino,
# patente_evento o None) -> None. Lanza AnonimizacionFallida si falla ese clip y
# ServicioNoDisponible si no se puede anonimizar ninguno. Si no hay anonimizador no se
# copia ningún video: un video sin anonimizar nunca llega a clips/.
Anonimizador = Callable[[str, str, Optional[str]], None]

# Aviso de progreso: (clip actual desde 1, total de clips a anonimizar, clip_id) -> None.
# Se llama justo antes de anonimizar cada clip; solo cuando hay anonimizador.
AlAvanzar = Callable[[int, int, str], None]

# La exportación se arma en <destino>.nuevo y al terminar reemplaza a <destino>; durante el
# cambio el anterior pasa un instante por <destino>.anterior. <destino>.lock evita que dos
# exportaciones (panel y script de consola) trabajen a la vez. Los tres son privados en
# helpers/storage_static.py.
SUFIJO_NUEVO = ".nuevo"
SUFIJO_ANTERIOR = ".anterior"
SUFIJO_LOCK = ".lock"

# Con esta cantidad de clips fallidos SEGUIDOS se aborta: suele indicar que backend-ia está
# saturado o roto, y seguir significaría esperar horas para terminar omitiendo casi todo.
MAX_FALLOS_CONSECUTIVOS = 3


class ExportacionEnCurso(Exception):
    """Ya hay otra exportación trabajando sobre el mismo dataset."""


class ExportacionAbortada(Exception):
    """La exportación se detuvo sin tocar el dataset anterior. `motivo` es un código corto."""

    def __init__(self, motivo: str, mensaje: str):
        super().__init__(mensaje)
        self.motivo = motivo


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
                     secreto: Optional[str] = None,
                     al_avanzar: Optional[AlAvanzar] = None) -> dict:
    """
    Reconstruye por completo <storage>/dataset/ (metadata.jsonl, README.md, resumen.json y clips/).
    Cada corrida parte de cero: quien revocó su consentimiento desaparece del dataset, y los
    clips excluidos a mano no entran. Devuelve un resumen con conteos y motivos de exclusión.
    al_avanzar (opcional) se llama antes de anonimizar cada clip con (actual, total, clip_id).

    Todo se arma en <destino>.nuevo y solo al final reemplaza a <destino>: si la exportación
    se aborta o se corta (reinicio del contenedor), el dataset anterior queda intacto.
    Lanza ExportacionEnCurso si ya hay otra corriendo y ExportacionAbortada si backend-ia no
    está disponible o no se pudo anonimizar ningún clip.
    """
    secreto = secreto or ACCESS_TOKEN_SECRET
    if not secreto:
        raise RuntimeError("Falta ACCESS_TOKEN_SECRET: se usa para seudonimizar al conductor")
    destino = os.path.abspath(destino or carpeta_dataset())

    with _bloqueo_exportacion(destino):
        _limpiar_restos(destino)
        nuevo = destino + SUFIJO_NUEVO
        os.makedirs(os.path.join(nuevo, "clips"))
        try:
            resumen = _generar(db, nuevo, anonimizador, secreto, al_avanzar)
        except BaseException:
            # Abortada o con un error inesperado: se descarta lo armado, el anterior no se tocó
            shutil.rmtree(nuevo, ignore_errors=True)
            raise
        _reemplazar(nuevo, destino)

    resumen["destino"] = destino
    return resumen


def _clip_disponible(video, fila) -> Optional[str]:
    """Por qué un clip de un reporte elegible no entra ("excluido", "sin_archivo") o None si entra."""
    if fila is not None and fila.excluido:
        return "excluido"
    if not os.path.isfile(get_ruta_absoluta(video.video_path)):
        return "sin_archivo"
    return None


def _generar(db: Session, carpeta: str, anonimizador: Optional[Anonimizador], secreto: str,
             al_avanzar: Optional[AlAvanzar] = None) -> dict:
    """Escribe el dataset completo en `carpeta` (vacía, con clips/) y devuelve el resumen."""
    carpeta_clips = os.path.join(carpeta, "clips")
    filas = filas_por_video(db)
    reportes = reportes_con_todo(db)
    # Total para el aviso de progreso: los clips que de verdad se van a anonimizar, con la
    # misma regla del recorrido de abajo (_clip_disponible)
    total_a_anonimizar = 0
    if anonimizador is not None and al_avanzar is not None:
        total_a_anonimizar = sum(
            1 for r in reportes if motivo_exclusion(r) is None
            for v in r.videos if _clip_disponible(v, filas.get(v.id)) is None
        )
    excluidos = {"no_aprobado": 0, "prueba": 0, "sin_consentimiento": 0, "sin_video": 0}
    registros = []
    sin_anonimizar = 0
    clips_excluidos = 0
    candidatos = 0          # clips que se intentó anonimizar
    omitidos = []           # [{"clip_id", "motivo"}] de los que fallaron
    fallos_seguidos = 0     # se reinicia con cada clip anonimizado con éxito

    for reporte in reportes:
        motivo = motivo_exclusion(reporte)
        if motivo:
            excluidos[motivo] += 1
            continue

        ia = h.limpiar_ia(reporte.validacion.resultados_ia if reporte.validacion else None)
        # Placa del vehículo del evento: queda visible en el video. Sin ella se difuminan todas.
        patente_evento = reporte.patente_confirmada or None
        registros_reporte = []
        hubo_excluidos = False
        hubo_omitidos = False

        for video in sorted(reporte.videos, key=lambda v: v.orden):
            fila = filas.get(video.id)
            disponible = _clip_disponible(video, fila)
            if disponible == "excluido":
                clips_excluidos += 1
                hubo_excluidos = True
                continue
            if disponible == "sin_archivo":
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
                candidatos += 1
                if al_avanzar is not None:
                    al_avanzar(candidatos, total_a_anonimizar, registro["clip_id"])
                nombre = f"{registro['clip_id']}.mp4"
                try:
                    anonimizador(video.video_path, os.path.join(carpeta_clips, nombre), patente_evento)
                except AnonimizacionFallida as error:
                    # Nunca se copia el original: el clip simplemente no entra al dataset
                    print(f"Clip {registro['clip_id']} omitido del dataset: {error}")
                    omitidos.append({"clip_id": registro["clip_id"], "motivo": error.motivo})
                    hubo_omitidos = True
                    fallos_seguidos += 1
                    if fallos_seguidos >= MAX_FALLOS_CONSECUTIVOS:
                        raise ExportacionAbortada(
                            "fallos_consecutivos",
                            f"Fallaron {fallos_seguidos} clips seguidos (el último: {error.motivo}); "
                            "se conserva el dataset anterior",
                        ) from error
                    continue
                except ServicioNoDisponible as error:
                    raise ExportacionAbortada(
                        error.motivo, f"El servicio de anonimización (backend-ia) no está disponible: {error}"
                    ) from error
                fallos_seguidos = 0
                registro["archivo"] = f"clips/{nombre}"
                registro["anonimizado"] = True
            registros_reporte.append(registro)

        if not registros_reporte:
            # Si sus clips se omitieron por la anonimización no es un reporte "sin video"
            if not hubo_excluidos and not hubo_omitidos:
                excluidos["sin_video"] += 1
            continue
        registros.extend(registros_reporte)

    if candidatos and len(omitidos) == candidatos:
        raise ExportacionAbortada(
            "todos_fallaron",
            f"No se pudo anonimizar ninguno de los {candidatos} clips; se conserva el dataset anterior",
        )

    _escribir_metadata(carpeta, registros)

    motivos = {}
    for omitido in omitidos:
        motivos[omitido["motivo"]] = motivos.get(omitido["motivo"], 0) + 1
    resumen = {
        "clips": len(registros),
        "eventos": len({r["evento_id"] for r in registros}),
        "conductores": len({r["conductor_id"] for r in registros}),
        "sin_anonimizar": sin_anonimizar,
        "clips_excluidos": clips_excluidos,
        "excluidos": excluidos,
        "omitidos": {"total": len(omitidos), "motivos": motivos, "clips": omitidos},
    }
    _escribir_readme(carpeta, resumen)
    _escribir_resumen_json(carpeta, resumen)
    return resumen


@contextmanager
def _bloqueo_exportacion(destino: str):
    """Candado de archivo (sirve entre procesos: la API y el script de consola)."""
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    # El archivo .lock no se borra al terminar: borrarlo abriría una carrera entre procesos
    archivo = open(destino + SUFIJO_LOCK, "w")
    try:
        try:
            fcntl.flock(archivo, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ExportacionEnCurso("Ya hay una exportación del dataset en curso") from None
        try:
            yield
        finally:
            fcntl.flock(archivo, fcntl.LOCK_UN)
    finally:
        archivo.close()


def _limpiar_restos(destino: str) -> None:
    """
    Deja el disco listo para exportar, con el candado ya tomado (nadie más está escribiendo):
    - <destino>.nuevo de una exportación cortada (con sus .part huérfanos) se borra entero.
    - <destino>.anterior: si <destino> no existe, el corte ocurrió entre los dos renombres
      de _reemplazar y se restaura; si existe, sobra y se borra.
    - Cualquier .part que haya quedado dentro del dataset vigente se borra.
    """
    nuevo, anterior = destino + SUFIJO_NUEVO, destino + SUFIJO_ANTERIOR
    if os.path.lexists(nuevo):
        print(f"Se descarta una exportación anterior que quedó a medias: {nuevo}")
        shutil.rmtree(nuevo, ignore_errors=True)
    if os.path.isdir(anterior):
        if os.path.isdir(destino):
            shutil.rmtree(anterior, ignore_errors=True)
        else:
            print(f"Se restaura el dataset anterior desde {anterior}")
            os.rename(anterior, destino)
    carpeta_clips = os.path.join(destino, "clips")
    if os.path.isdir(carpeta_clips):
        for nombre in os.listdir(carpeta_clips):
            if nombre.endswith(SUFIJO_PARCIAL):
                os.remove(os.path.join(carpeta_clips, nombre))


def _reemplazar(nuevo: str, destino: str) -> None:
    """
    Pone el dataset recién armado en lugar del anterior. Son dos renombres en el mismo
    sistema de archivos (instantáneos); si el proceso muere entre ambos, _limpiar_restos
    restaura el anterior en la siguiente exportación. Mientras tanto la descarga responde
    "aún no se ha generado", nunca un dataset mezclado.
    """
    anterior = destino + SUFIJO_ANTERIOR
    if os.path.isdir(destino):
        os.rename(destino, anterior)
    os.rename(nuevo, destino)
    shutil.rmtree(anterior, ignore_errors=True)


def _escribir_metadata(destino: str, registros: list) -> None:
    temporal = os.path.join(destino, "metadata.jsonl.tmp")
    with open(temporal, "w", encoding="utf-8") as archivo:
        for registro in registros:
            archivo.write(json.dumps(registro, ensure_ascii=False) + "\n")
    os.replace(temporal, os.path.join(destino, "metadata.jsonl"))


def _escribir_resumen_json(destino: str, resumen: dict) -> None:
    contenido = {k: v for k, v in resumen.items() if k != "destino"}
    # La lista de clips omitidos solo le sirve al panel; en el archivo basta con los conteos
    contenido["omitidos"] = {k: v for k, v in resumen["omitidos"].items() if k != "clips"}
    contenido["generado"] = datetime.now(timezone.utc).isoformat()
    contenido["version_consentimiento"] = CONSENTIMIENTO_VERSION_VIGENTE
    temporal = os.path.join(destino, "resumen.json.tmp")
    with open(temporal, "w", encoding="utf-8") as archivo:
        json.dump(contenido, archivo, ensure_ascii=False, indent=2)
    os.replace(temporal, os.path.join(destino, "resumen.json"))


def _escribir_readme(destino: str, resumen: dict) -> None:
    ahora = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    if resumen["sin_anonimizar"]:
        estado_videos = (
            f"**Esta exportación no incluye videos**: {resumen['sin_anonimizar']} clips "
            "están a la espera del paso de anonimización (rostros y patentes de terceros, "
            "sin audio). Hasta entonces `clips/` queda vacía y `anonimizado` es `false`."
        )
    else:
        estado_videos = (
            "Todos los videos de `clips/` fueron anonimizados antes de copiarse: rostros y "
            "patentes de terceros difuminados, sin audio. La patente del vehículo del evento "
            "queda visible cuando el equipo validador la confirmó; si no, se difuminan todas."
        )
    omitidos = resumen["omitidos"]
    linea_omitidos = ""
    if omitidos["total"]:
        detalle = ", ".join(f"{motivo}: {n}" for motivo, n in sorted(omitidos["motivos"].items()))
        linea_omitidos = (
            f"\n- Clips omitidos porque no se pudieron anonimizar: {omitidos['total']} ({detalle}). "
            "No forman parte de esta exportación."
        )
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
- Clips: {resumen['clips']} · eventos (reportes): {resumen['eventos']} · conductores (seudonimizados): {resumen['conductores']}{linea_omitidos}

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
Nombre, correo, RUT, patentes como texto, comentarios, notas del administrador, infracciones
escritas a mano y cualquier texto libre. Ojo: la patente del vehículo del evento sí puede
leerse en el video cuando fue confirmada (ver "Alcance y uso").
"""
    with open(os.path.join(destino, "README.md"), "w", encoding="utf-8") as archivo:
        archivo.write(texto)
