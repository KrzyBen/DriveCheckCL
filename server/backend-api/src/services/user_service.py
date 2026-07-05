from sqlalchemy.orm import Session

from entity.user_entity import User
from helpers.bcrypt_helper import compare_password, encrypt_password

async def create_user_admin_service(db: Session, nombre_completo: str, rut: str, email: str, password: str, rol: str = "conductor"):
    try:
        if db.query(User).filter(User.email == email).first():
            return [None, {"dataInfo": "email", "message": "Correo electrónico en uso"}]
        if db.query(User).filter(User.rut == rut).first():
            return [None, {"dataInfo": "rut", "message": "RUT ya asociado a una cuenta"}]

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


async def get_users_service(db: Session, excluir_id: int):
    try:
        users = db.query(User).filter(User.id != excluir_id).all()

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


async def update_user_admin_service(db: Session, target_id: int, body: dict):
    """Edición hecha por un admin sobre OTRO usuario: no exige la contraseña actual."""
    try:
        user = db.query(User).filter(User.id == target_id).first()
        if not user:
            return [None, "Usuario no encontrado"]

        if body.get("rut") or body.get("email"):
            from sqlalchemy import or_
            dup_filters = []
            if body.get("rut"):   dup_filters.append(User.rut == body["rut"])
            if body.get("email"): dup_filters.append(User.email == body["email"])
            existing = db.query(User).filter(or_(*dup_filters)).first()
            if existing and existing.id != user.id:
                return [None, "Ya existe un usuario con el mismo rut o email"]

        if body.get("nombre_completo"): user.nombre_completo = body["nombre_completo"]
        if body.get("rut"):             user.rut = body["rut"]
        if body.get("email"):           user.email = body["email"]
        if body.get("rol"):             user.rol = body["rol"]
        if body.get("new_password"):    user.password = await encrypt_password(body["new_password"])

        db.commit()
        db.refresh(user)

        return [{
            "id": user.id, "nombre_completo": user.nombre_completo,
            "rut": user.rut, "email": user.email, "rol": user.rol,
            "updated_at": str(user.updated_at),
        }, None]

    except Exception as error:
        print(f"Error al modificar usuario (admin): {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def delete_user_service(db: Session, current_admin: User, id: int = None, rut: str = None, email: str = None):
    try:
        from sqlalchemy import or_
        filters = []
        if id:    filters.append(User.id == id)
        if rut:   filters.append(User.rut == rut)
        if email: filters.append(User.email == email)

        user = db.query(User).filter(or_(*filters)).first()
        if not user:
            return [None, "Usuario no encontrado"]

        if user.id == current_admin.id:
            return [None, "No puedes eliminar tu propia cuenta desde aquí, usa tu perfil"]

        if user.rol == "administrador":
            total_admins = db.query(User).filter(User.rol == "administrador").count()
            if total_admins <= 1:
                return [None, "Debe quedar al menos un administrador activo"]

        user_data = {
            "id": user.id, "nombre_completo": user.nombre_completo,
            "rut": user.rut, "email": user.email, "rol": user.rol,
        }
        db.delete(user)
        db.commit()
        return [user_data, None]

    except Exception as error:
        print(f"Error al eliminar usuario: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]
