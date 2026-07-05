from datetime import timedelta
from jose import jwt
from sqlalchemy.orm import Session

from config.configEnv import settings
from entity.user_entity import User
from helpers.bcrypt_helper import encrypt_password, compare_password


async def login_service(db: Session, email: str, password: str):
    try:
        def create_error(field: str, message: str):
            return {"dataInfo": field, "message": message}

        user = db.query(User).filter(User.email == email).first()
        if not user:
            return [None, create_error("email", "El correo electrónico es incorrecto")]

        is_match = await compare_password(password, user.password)
        if not is_match:
            return [None, create_error("password", "La contraseña es incorrecta")]

        payload = {
            "nombre_completo": user.nombre_completo,
            "email": user.email,
            "rut": user.rut,
            "rol": user.rol,
        }

        access_token = jwt.encode(
            payload,
            settings.ACCESS_TOKEN_SECRET,
            algorithm="HS256"
        )

        user_data = {
            "id": user.id,
            "nombre_completo": user.nombre_completo,
            "rut": user.rut,
            "email": user.email,
            "rol": user.rol,
            "created_at": str(user.created_at),
        }
        return [{"token": access_token, "user": user_data}, None]

    except Exception as error:
        print(f"Error al iniciar sesión: {error}")
        return [None, "Error interno del servidor"]


async def register_service(db: Session, nombre_completo: str, rut: str, email: str, password: str):
    try:
        def create_error(field: str, message: str):
            return {"dataInfo": field, "message": message}

        existing_email = db.query(User).filter(User.email == email).first()
        if existing_email:
            return [None, create_error("email", "Correo electrónico en uso")]

        existing_rut = db.query(User).filter(User.rut == rut).first()
        if existing_rut:
            return [None, create_error("rut", "RUT ya asociado a una cuenta")]

        new_user = User(
            nombre_completo=nombre_completo,
            rut=rut,
            email=email,
            password=await encrypt_password(password),
            rol="conductor",
        )

        db.add(new_user)
        db.commit()
        db.refresh(new_user)

        user_data = {
            "id": new_user.id,
            "nombre_completo": new_user.nombre_completo,
            "rut": new_user.rut,
            "email": new_user.email,
            "rol": new_user.rol,
            "created_at": str(new_user.created_at),
        }

        return [user_data, None]

    except Exception as error:
        print(f"Error al registrar usuario: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]

async def create_user_admin_service(db: Session, nombre_completo: str, rut: str, email: str, password: str, rol: str = "conductor"):
    try:
        def create_error(field: str, message: str):
            return {"dataInfo": field, "message": message}

        if rol not in ("administrador", "validador", "conductor"):
            return [None, create_error("rol", "Rol inválido")]

        if db.query(User).filter(User.email == email).first():
            return [None, create_error("email", "Correo electrónico en uso")]

        if db.query(User).filter(User.rut == rut).first():
            return [None, create_error("rut", "RUT ya asociado a una cuenta")]

        new_user = User(
            nombre_completo=nombre_completo,
            rut=rut,
            email=email,
            password=await encrypt_password(password),
            rol=rol,
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)

        return [{
            "id": new_user.id,
            "nombre_completo": new_user.nombre_completo,
            "rut": new_user.rut,
            "email": new_user.email,
            "rol": new_user.rol,
            "created_at": str(new_user.created_at),
        }, None]

    except Exception as error:
        print(f"Error al crear usuario (admin): {error}")
        db.rollback()
        return [None, "Error interno del servidor"]
