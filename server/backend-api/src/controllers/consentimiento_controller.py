from fastapi import Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authentication_middleware import authenticate_jwt
from services.consentimiento_service import (
    obtener_consentimiento_service,
    guardar_consentimiento_service,
)
from validations.consentimiento_validation import ConsentimientoValidation
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def obtener_consentimiento(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        data, error = await obtener_consentimiento_service(db, current_user.id)
        if error:
            return handle_error_client(400, "Error obteniendo el consentimiento", error)
        return handle_success(200, "Consentimiento encontrado", data)
    except Exception as error:
        return handle_error_server(500, str(error))


async def guardar_consentimiento(
    body: ConsentimientoValidation,
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db),
):
    try:
        data, error = await guardar_consentimiento_service(
            db, current_user.id, body.acepto_estudio, body.version
        )
        if error:
            # 409: la versión del texto que vio el usuario ya no es la vigente.
            status = 409 if isinstance(error, dict) and error.get("dataInfo") == "version" else 400
            return handle_error_client(status, "Error guardando el consentimiento", error)
        return handle_success(200, "Consentimiento guardado", data)
    except Exception as error:
        return handle_error_server(500, str(error))
