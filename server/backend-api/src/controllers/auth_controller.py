from fastapi import Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from services.auth_service import login_service, register_service
from validations.auth_validation import LoginValidation, RegisterValidation
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def login(body: LoginValidation, db: Session = Depends(get_db)):
    try:
        access_token, error = await login_service(db, body.email, body.password)
        if error:
            return handle_error_client(400, "Error iniciando sesión", error)
        return handle_success(200, "Inicio de sesión exitoso", {"token": access_token})
    except Exception as error:
        return handle_error_server(500, str(error))


async def register(body: RegisterValidation, db: Session = Depends(get_db)):
    try:
        new_user, error = await register_service(
            db,
            nombre_completo=body.nombre_completo,
            rut=body.rut,
            email=body.email,
            password=body.password,
        )
        if error:
            return handle_error_client(400, "Error registrando al usuario", error)
        return handle_success(201, "Usuario registrado con éxito", new_user)
    except Exception as error:
        return handle_error_server(500, str(error))


async def logout():
    try:
        return handle_success(200, "Sesión cerrada exitosamente")
    except Exception as error:
        return handle_error_server(500, str(error))
