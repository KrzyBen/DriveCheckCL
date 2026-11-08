import os
import tempfile
import zipfile
from typing import Optional

from sqlalchemy.orm import Session

from entity.reporte_entity import Reporte, VideoReporte, Infraccion
from entity.dataset_clip_entity import DatasetClip
from helpers import dataset_helper as h
from helpers.storage_helper import get_ruta_absoluta
from services import dataset_service as ds


def _error(status: int, message: str) -> list:
    return [None, {"status": status, "message": message}]


def _ids(infracciones) -> list:
    return sorted(i["id"] for i in infracciones)


def _item(reporte, video, fila, exportados) -> dict:
    severidad, infracciones, origen = h.etiquetas_efectivas(reporte, fila)
    cid = h.clip_id(reporte.id, video.orden)
    excluido = bool(fila and fila.excluido)
    registro = exportados.get(cid)
    desactualizado = False
    if registro is not None:
        desactualizado = (
            excluido
            or registro.get("severidad") != severidad
            or _ids(registro.get("infracciones", [])) != _ids(infracciones)
        )
    return {
        "video_id": video.id,
        "clip_id": cid,
        "evento_id": reporte.id,
        "orden": video.orden,
        "grabado_en": video.grabado_en.isoformat() if video.grabado_en else None,
        "severidad": severidad,
        "infracciones": infracciones,
        "etiquetas_origen": origen,
        "excluido": excluido,
        "archivo_ok": os.path.isfile(get_ruta_absoluta(video.video_path)),
        "en_dataset": registro is not None,
        "anonimizado": bool(registro and registro.get("anonimizado")),
        "cambios_sin_exportar": desactualizado,
    }


def _clips_elegibles(db: Session):
    """(reporte, video, fila) de cada clip de un reporte que puede formar parte del dataset."""
    filas = ds.filas_por_video(db)
    salida = []
    for reporte in ds.reportes_con_todo(db):
        if ds.motivo_exclusion(reporte) is None:
            for video in sorted(reporte.videos, key=lambda v: v.orden):
                salida.append((reporte, video, filas.get(video.id)))
    return salida


async def resumen_service(db: Session):
    try:
        exportados, resumen_json = ds.leer_exportados()
        conteo = {"total": 0, "no_aprobados": 0, "prueba": 0, "sin_consentimiento": 0, "elegibles": 0}
        claves = {"no_aprobado": "no_aprobados", "prueba": "prueba", "sin_consentimiento": "sin_consentimiento"}
        for reporte in ds.reportes_con_todo(db):
            conteo["total"] += 1
            motivo = ds.motivo_exclusion(reporte)
            conteo[claves[motivo] if motivo else "elegibles"] += 1

        items = [_item(r, v, f, exportados) for r, v, f in _clips_elegibles(db)]
        listos = {i["clip_id"] for i in items if i["archivo_ok"] and not i["excluido"]}
        desactualizado = (set(exportados) != listos) or any(i["cambios_sin_exportar"] for i in items)
        return [{
            "reportes": conteo,
            "clips": {
                "elegibles": len(items),
                "con_archivo": sum(i["archivo_ok"] for i in items),
                "excluidos": sum(i["excluido"] for i in items),
                "listos": len(listos),
            },
            "dataset": {
                "generado": resumen_json.get("generado") if resumen_json else None,
                "clips": len(exportados),
                "sin_anonimizar": sum(1 for r in exportados.values() if not r.get("anonimizado")),
                "desactualizado": desactualizado,
            },
        }, None]
    except Exception as error:
        print(f"Error en resumen del dataset: {error}")
        return [None, {"status": 500, "message": "Error interno del servidor"}]


async def listar_clips_service(db: Session, pagina: int, limite: int):
    try:
        exportados, _ = ds.leer_exportados()
        items = [_item(r, v, f, exportados) for r, v, f in _clips_elegibles(db)]
        items.sort(key=lambda i: (-i["evento_id"], i["orden"]))
        inicio = (pagina - 1) * limite
        return [{"total": len(items), "pagina": pagina, "limite": limite,
                 "clips": items[inicio:inicio + limite]}, None]
    except Exception as error:
        print(f"Error al listar clips del dataset: {error}")
        return [None, {"status": 500, "message": "Error interno del servidor"}]


def _clip_elegible(db: Session, video_id: int):
    """(reporte, video, error). Solo se curan clips de reportes que pueden estar en el dataset."""
    video = db.query(VideoReporte).filter(VideoReporte.id == video_id).first()
    if video is None:
        return None, None, _error(404, "Clip no encontrado")[1]
    reporte = db.query(Reporte).filter(Reporte.id == video.reporte_id).first()
    motivo = ds.motivo_exclusion(reporte)
    if motivo:
        textos = {
            "no_aprobado": "El reporte de este clip no está aprobado",
            "prueba": "Es un reporte de prueba",
            "sin_consentimiento": "El conductor no tiene un consentimiento vigente para el estudio",
        }
        return None, None, {"status": 409, "message": f"{textos[motivo]}: no puede formar parte del dataset"}
    return reporte, video, None


def _respuesta(db: Session, reporte, video):
    fila = db.query(DatasetClip).filter(DatasetClip.video_id == video.id).first()
    exportados, _ = ds.leer_exportados()
    return _item(reporte, video, fila, exportados)


async def actualizar_clip_service(db: Session, admin, video_id: int, datos: dict):
    """datos: solo las claves enviadas (infracciones, severidad, excluido)."""
    try:
        reporte, video, error = _clip_elegible(db, video_id)
        if error:
            return [None, error]

        catalogo = []
        if "infracciones" in datos:
            ids = sorted(set(datos["infracciones"]))
            catalogo = db.query(Infraccion).filter(Infraccion.id.in_(ids)).all() if ids else []
            if len(catalogo) != len(ids):
                return _error(400, "Alguna infracción no existe en el catálogo")
            if any(i.articulo == "Manual" for i in catalogo):
                return _error(400, "Las infracciones manuales (texto libre) no se usan en el dataset")

        fila = db.query(DatasetClip).filter(DatasetClip.video_id == video_id).first()
        if fila is None:
            fila = DatasetClip(video_id=video_id, etiquetas_propias=False, excluido=False)
            db.add(fila)

        if "infracciones" in datos or "severidad" in datos:
            if not fila.etiquetas_propias:
                # Primera edición: el clip parte de lo que validó el reporte.
                fila.infracciones = [i for i in reporte.infracciones if i.articulo != "Manual"]
                fila.severidad = reporte.severidad_validada
                fila.etiquetas_propias = True
            if "infracciones" in datos:
                fila.infracciones = catalogo
            if "severidad" in datos:
                fila.severidad = datos["severidad"]
        if "excluido" in datos:
            fila.excluido = datos["excluido"]

        fila.editado_por_id = admin.id
        db.commit()
        return [_respuesta(db, reporte, video), None]
    except Exception as error:
        db.rollback()
        print(f"Error al actualizar clip del dataset: {error}")
        return [None, {"status": 500, "message": "Error interno del servidor"}]


async def restablecer_clip_service(db: Session, admin, video_id: int):
    """Vuelve a las etiquetas del reporte. Si el clip estaba excluido, sigue excluido."""
    try:
        reporte, video, error = _clip_elegible(db, video_id)
        if error:
            return [None, error]
        fila = db.query(DatasetClip).filter(DatasetClip.video_id == video_id).first()
        if fila is not None:
            if fila.excluido:
                fila.etiquetas_propias = False
                fila.infracciones = []
                fila.severidad = None
                fila.editado_por_id = admin.id
            else:
                db.delete(fila)
            db.commit()
        return [_respuesta(db, reporte, video), None]
    except Exception as error:
        db.rollback()
        print(f"Error al restablecer etiquetas del clip: {error}")
        return [None, {"status": 500, "message": "Error interno del servidor"}]


async def exportar_service(db: Session):
    try:
        resumen = ds.exportar_dataset(db)
        resumen.pop("destino", None)
        return [resumen, None]
    except Exception as error:
        print(f"Error al exportar el dataset: {error}")
        return [None, {"status": 500, "message": "No se pudo generar el dataset"}]


def crear_zip_dataset() -> Optional[str]:
    """Empaqueta storage/dataset/ en un .zip temporal. None si todavía no se ha generado."""
    carpeta = ds.carpeta_dataset()
    if not os.path.isfile(os.path.join(carpeta, "metadata.jsonl")):
        return None
    temporal = tempfile.NamedTemporaryFile(delete=False, suffix=".zip")
    temporal.close()
    with zipfile.ZipFile(temporal.name, "w") as zf:
        for raiz, _, archivos in os.walk(carpeta):
            for nombre in sorted(archivos):
                if nombre.endswith(".tmp"):
                    continue
                ruta = os.path.join(raiz, nombre)
                compresion = zipfile.ZIP_STORED if nombre.endswith(".mp4") else zipfile.ZIP_DEFLATED
                zf.write(ruta, os.path.relpath(ruta, carpeta), compress_type=compresion)
    return temporal.name
