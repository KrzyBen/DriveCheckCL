from fastapi import Depends, Query
from sqlalchemy.orm import Session
from typing import Optional

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authorization_middleware import is_admin
from services.user_service import (
    get_user_service, get_users_service, create_user_admin_service,
    update_user_admin_service, delete_user_service,
)
from validations.user_validation import UserBodyValidation, UserCreateValidation
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server

async def create_user_admin(
    body: UserCreateValidation,
    db: Session = Depends(get_db),
):
    try:
        data = body.model_dump()
        user, error = await create_user_admin_service(db, **data)
        if error:
            return handle_error_client(400, "Error creando usuario", error)
        return handle_success(201, "Usuario creado correctamente", user)
    except Exception as error:
        return handle_error_server(500, str(error))

async def get_users(current_admin: User = Depends(is_admin), db: Session = Depends(get_db)):
    try:
        users, error = await get_users_service(db, excluir_id=current_admin.id)
        if error:
            return handle_error_client(404, error)
        if not users:
            return handle_success(200, "Sin otros usuarios registrados", [])
        return handle_success(200, "Usuarios encontrados", users)
    except Exception as error:
        return handle_error_server(500, str(error))


async def get_user(
    id:    Optional[int] = Query(None),
    rut:   Optional[str] = Query(None),
    email: Optional[str] = Query(None),
    db:    Session = Depends(get_db)
):
    try:
        if not any([id, rut, email]):
            return handle_error_client(400, "Debes proporcionar al menos un parámetro: id, email o rut.")
        user, error = await get_user_service(db, id=id, rut=rut, email=email)
        if error:
            return handle_error_client(404, error)
        return handle_success(200, "Usuario encontrado", user)
    except Exception as error:
        return handle_error_server(500, str(error))


async def update_user(
    body: UserBodyValidation,
    id: Optional[int] = Query(None),
    db: Session = Depends(get_db)
):
    try:
        if not id:
            return handle_error_client(400, "Debes proporcionar el id del usuario a modificar.")
        body_dict = body.model_dump(exclude_none=True)
        user, error = await update_user_admin_service(db, id, body_dict)
        if error:
            return handle_error_client(400, "Error modificando al usuario", error)
        return handle_success(200, "Usuario modificado correctamente", user)
    except Exception as error:
        return handle_error_server(500, str(error))

async def delete_user(
    id: Optional[int] = Query(None),
    rut: Optional[str] = Query(None),
    email: Optional[str] = Query(None),
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db)
):
    try:
        if not any([id, rut, email]):
            return handle_error_client(400, "Debes proporcionar al menos un parámetro: id, email o rut.")
        user, error = await delete_user_service(db, current_admin, id=id, rut=rut, email=email)
        if error:
            return handle_error_client(400, "Error eliminando al usuario", error)
        return handle_success(200, "Usuario eliminado correctamente", user)
    except Exception as error:
        return handle_error_server(500, str(error))