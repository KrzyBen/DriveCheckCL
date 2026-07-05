from fastapi import Depends, Query
from sqlalchemy.orm import Session
from typing import Optional

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authorization_middleware import is_admin
from validations.reporte_validation import ReporteValidarValidation, ReporteRechazarValidation
from services.reporte_service import (
    listar_reportes_admin_service,
    get_reporte_admin_service,
    validar_reporte_service,
    rechazar_reporte_service,
    eliminar_reporte_admin_service,
)
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def listar_reportes_admin(
    estado: Optional[str] = Query(None),
    usuario_id: Optional[int] = Query(None),
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await listar_reportes_admin_service(db, estado=estado, usuario_id=usuario_id)
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Reportes encontrados", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def get_reporte_admin(
    reporte_id: int,
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await get_reporte_admin_service(db, reporte_id)
        if error:
            return handle_error_client(404, error)
        return handle_success(200, "Reporte encontrado", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def validar_reporte(
    reporte_id: int,
    body: ReporteValidarValidation,
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await validar_reporte_service(
            db, current_admin, reporte_id,
            severidad_validada=body.severidad_validada,
            infraccion_ids=body.infraccion_ids,
            infracciones_manuales=body.infracciones_manuales,
            notas_admin=body.notas_admin,
        )
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Reporte aprobado correctamente", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def rechazar_reporte(
    reporte_id: int,
    body: ReporteRechazarValidation,
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await rechazar_reporte_service(db, current_admin, reporte_id, notas_admin=body.notas_admin)
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Reporte rechazado", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def eliminar_reporte_admin(
    reporte_id: int,
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        data, error = await eliminar_reporte_admin_service(db, reporte_id)
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Reporte eliminado correctamente", data)
    except Exception as error:
        return handle_error_server(500, str(error))