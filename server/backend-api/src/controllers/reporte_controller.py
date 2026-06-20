from fastapi import Depends, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List, Optional

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authentication_middleware import authenticate_jwt
from services.reporte_service import (
    crear_reporte_service,
    listar_reportes_service,
    get_reporte_service,
    actualizar_estado_service,
    eliminar_reporte_service,
    contar_reportes_service,
)
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def contar_reportes(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        data, error = await contar_reportes_service(db, current_user.id)
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Conteo obtenido", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def crear_reporte(
    titulo: str = Form(...),
    comentario: Optional[str] = Form(None),
    videos: List[UploadFile] = File(...),
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        reporte, error = await crear_reporte_service(
            db,
            usuario_id=current_user.id,
            titulo=titulo,
            comentario=comentario,
            archivos=videos,
        )
        if error:
            return handle_error_client(400, error)
        return handle_success(201, "Reporte creado exitosamente", reporte)
    except Exception as error:
        return handle_error_server(500, str(error))


async def listar_mis_reportes(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        reportes, error = await listar_reportes_service(db, current_user.id)
        if error:
            return handle_error_client(404, error)
        if not reportes:
            return handle_success(200, "Sin reportes aún", [])
        return handle_success(200, "Reportes encontrados", reportes)
    except Exception as error:
        return handle_error_server(500, str(error))


async def get_reporte(
    reporte_id: int,
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        reporte, error = await get_reporte_service(db, current_user.id, reporte_id)
        if error:
            return handle_error_client(404, error)
        return handle_success(200, "Reporte encontrado", reporte)
    except Exception as error:
        return handle_error_server(500, str(error))


async def eliminar_reporte(
    reporte_id: int,
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        data, error = await eliminar_reporte_service(db, current_user.id, reporte_id)
        if error:
            return handle_error_client(404, error)
        return handle_success(200, "Reporte eliminado correctamente", data)
    except Exception as error:
        return handle_error_server(500, str(error))