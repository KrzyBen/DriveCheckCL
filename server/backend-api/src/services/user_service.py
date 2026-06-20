from sqlalchemy.orm import Session

from entity.user_entity import User
from helpers.bcrypt_helper import compare_password, encrypt_password


async def get_user_service(db: Session, id: int = None, rut: str = None, email: str = None):
    try:
        filters = []
        if id:    filters.append(User.id == id)
        if rut:   filters.append(User.rut == rut)
        if email: filters.append(User.email == email)

        if not filters:
            return [None, "Debes proporcionar al menos un parámetro: id, email o rut."]

        from sqlalchemy import or_
        user = db.query(User).filter(or_(*filters)).first()

        if not user:
            return [None, "Usuario no encontrado"]

        user_data = {
            "id": user.id,
            "nombre_completo": user.nombre_completo,
            "rut": user.rut,
            "email": user.email,
            "rol": user.rol,
            "created_at": str(user.created_at),
            "updated_at": str(user.updated_at),
        }

        return [user_data, None]

    except Exception as error:
        print(f"Error al obtener usuario: {error}")
        return [None, "Error interno del servidor"]


async def get_users_service(db: Session):
    try:
        users = db.query(User).all()

        if not users:
            return [None, "No hay usuarios"]

        users_data = [
            {
                "id": u.id,
                "nombre_completo": u.nombre_completo,
                "rut": u.rut,
                "email": u.email,
                "rol": u.rol,
                "created_at": str(u.created_at),
                "updated_at": str(u.updated_at),
            }
            for u in users
        ]

        return [users_data, None]

    except Exception as error:
        print(f"Error al obtener usuarios: {error}")
        return [None, "Error interno del servidor"]


async def update_user_service(db: Session, query: dict, body: dict):
    try:
        from sqlalchemy import or_
        filters = []
        if query.get("id"):    filters.append(User.id == query["id"])
        if query.get("rut"):   filters.append(User.rut == query["rut"])
        if query.get("email"): filters.append(User.email == query["email"])

        user = db.query(User).filter(or_(*filters)).first()
        if not user:
            return [None, "Usuario no encontrado"]

        # Verificar duplicados de rut/email
        if body.get("rut") or body.get("email"):
            dup_filters = []
            if body.get("rut"):   dup_filters.append(User.rut == body["rut"])
            if body.get("email"): dup_filters.append(User.email == body["email"])
            existing = db.query(User).filter(or_(*dup_filters)).first()
            if existing and existing.id != user.id:
                return [None, "Ya existe un usuario con el mismo rut o email"]

        # Verificar contraseña actual si se quiere cambiar
        if body.get("password"):
            match = await compare_password(body["password"], user.password)
            if not match:
                return [None, "La contraseña no coincide"]

        # Actualizar campos
        if body.get("nombre_completo"): user.nombre_completo = body["nombre_completo"]
        if body.get("rut"):             user.rut = body["rut"]
        if body.get("email"):           user.email = body["email"]
        if body.get("rol"):             user.rol = body["rol"]
        if body.get("new_password"):    user.password = await encrypt_password(body["new_password"])

        db.commit()
        db.refresh(user)

        user_data = {
            "id": user.id,
            "nombre_completo": user.nombre_completo,
            "rut": user.rut,
            "email": user.email,
            "rol": user.rol,
            "updated_at": str(user.updated_at),
        }

        return [user_data, None]

    except Exception as error:
        print(f"Error al modificar usuario: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def delete_user_service(db: Session, id: int = None, rut: str = None, email: str = None):
    try:
        from sqlalchemy import or_
        filters = []
        if id:    filters.append(User.id == id)
        if rut:   filters.append(User.rut == rut)
        if email: filters.append(User.email == email)

        user = db.query(User).filter(or_(*filters)).first()
        if not user:
            return [None, "Usuario no encontrado"]

        if user.rol == "administrador":
            return [None, "No se puede eliminar un usuario con rol de administrador"]

        user_data = {
            "id": user.id,
            "nombre_completo": user.nombre_completo,
            "rut": user.rut,
            "email": user.email,
            "rol": user.rol,
        }

        db.delete(user)
        db.commit()

        return [user_data, None]

    except Exception as error:
        print(f"Error al eliminar usuario: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]
