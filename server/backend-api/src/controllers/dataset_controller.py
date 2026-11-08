import os
from datetime import datetime, timezone

from fastapi import Depends, Query
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from starlette.background import BackgroundTask

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authorization_middleware import is_admin
from services.dataset_admin_service import (
    resumen_service, listar_clips_service, actualizar_clip_service,
    restablecer_clip_service, exportar_service, crear_zip_dataset,
)
from validations.dataset_validation import ClipEtiquetasValidation
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


def _responder_error(error, mensaje):
    status = error.get("status", 400)
    if status >= 500:
        return handle_error_server(status, error.get("message", mensaje))
    return handle_error_client(status, mensaje, error.get("message"))


async def obtener_resumen_dataset(
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await resumen_service(db)
        if error:
            return _responder_error(error, "Error obteniendo el resumen del dataset")
        return handle_success(200, "Resumen del dataset", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def listar_clips_dataset(
    pagina: int = Query(1, ge=1),
    limite: int = Query(25, ge=1, le=100),
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await listar_clips_service(db, pagina, limite)
        if error:
            return _responder_error(error, "Error listando los clips")
        return handle_success(200, "Clips del dataset", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def actualizar_clip_dataset(
    video_id: int,
    body: ClipEtiquetasValidation,
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        datos = {campo: getattr(body, campo) for campo in body.model_fields_set}
        data, error = await actualizar_clip_service(db, current_admin, video_id, datos)
        if error:
            return _responder_error(error, "No se pudo actualizar el clip")
        return handle_success(200, "Clip actualizado", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def restablecer_clip_dataset(
    video_id: int,
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await restablecer_clip_service(db, current_admin, video_id)
        if error:
            return _responder_error(error, "No se pudieron restablecer las etiquetas")
        return handle_success(200, "Etiquetas restablecidas a las del reporte", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def exportar_dataset_admin(
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await exportar_service(db)
        if error:
            return _responder_error(error, "No se pudo generar el dataset")
        return handle_success(200, "Dataset generado", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def descargar_dataset(current_admin: User = Depends(is_admin)):
    try:
        ruta = crear_zip_dataset()
        if ruta is None:
            return handle_error_client(404, "Aún no se ha generado el dataset")
        nombre = f"dataset_drivecheckcl_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M')}.zip"
        return FileResponse(ruta, media_type="application/zip", filename=nombre,
                            background=BackgroundTask(os.remove, ruta))
    except Exception as error:
        return handle_error_server(500, str(error))
