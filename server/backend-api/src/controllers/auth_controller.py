from fastapi import Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from services.auth_service import login_service, register_service
from services.password_reset_service import (request_password_reset_service,confirm_password_reset_service,)
from validations.auth_validation import (LoginValidation,RegisterValidation,ForgotPasswordValidation,ResetPasswordValidation,)
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def login(body: LoginValidation, db: Session = Depends(get_db)):
    try:
        access_token, error = await login_service(db, body.email, body.password)
        if error:
            return handle_error_client(400, "Error iniciando sesión", error)
        return handle_success(200, "Inicio de sesión exitoso", access_token)
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

async def forgot_password(body: ForgotPasswordValidation, db: Session = Depends(get_db)):
    try:
        _, error = await request_password_reset_service(db, body.email)
        if error:
            return handle_error_client(400, "Error solicitando recuperación de contraseña", error)
        # Mensaje genérico a propósito: no confirma ni desmiente si el correo
        # está registrado, para no facilitar enumeración de usuarios.
        return handle_success(
            200,
            "Si el correo está registrado, te enviamos un código de recuperación"
        )
    except Exception as error:
        return handle_error_server(500, str(error))


async def reset_password(body: ResetPasswordValidation, db: Session = Depends(get_db)):
    try:
        _, error = await confirm_password_reset_service(db, body.email, body.code, body.new_password)
        if error:
            return handle_error_client(400, "Error al restablecer la contraseña", error)
        return handle_success(200, "Contraseña actualizada con éxito")
    except Exception as error:
        return handle_error_server(500, str(error))
