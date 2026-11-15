from fastapi import Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authentication_middleware import authenticate_jwt
from services.notificacion_service import (
    listar_notificaciones_service,
    marcar_notificaciones_leidas_service,
)
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def listar_notificaciones(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        data, error = await listar_notificaciones_service(db, current_user.id)
        if error:
            return handle_error_client(400, "Error obteniendo notificaciones", error)
        return handle_success(200, "Notificaciones encontradas", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def marcar_notificaciones_leidas(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        _, error = await marcar_notificaciones_leidas_service(db, current_user.id)
        if error:
            return handle_error_client(400, "Error actualizando notificaciones", error)
        return handle_success(200, "Notificaciones marcadas como leídas")
    except Exception as error:
        return handle_error_server(500, str(error))
