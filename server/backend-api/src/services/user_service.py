import os

from sqlalchemy.orm import Session

from entity.user_entity import User
from entity.reporte_entity import Reporte
from entity.reporte_validacion_entity import ReporteValidacion
from entity.password_reset_entity import PasswordResetCode
from helpers.bcrypt_helper import compare_password, encrypt_password
from helpers.storage_helper import eliminar_carpeta_reporte, get_storage_root

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
    try:
        user = db.query(User).filter(User.id == target_id).first()
        if not user:
            return [None, "Usuario no encontrado"]

        # Evita que se quite el rol al último administrador del sistema
        nuevo_rol = body.get("rol")
        if user.rol == "administrador" and nuevo_rol and nuevo_rol != "administrador":
            total_admins = db.query(User).filter(User.rol == "administrador").count()
            if total_admins <= 1:
                return [None, "Debe quedar al menos un administrador activo"]

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
        # Sin ningún criterio, or_() vacío no filtra y se tomaría el primer usuario de la tabla
        if not filters:
            return [None, "Debes indicar id, rut o email"]

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

        # Reportes de otros usuarios que este validó: se conservan, pero sin referencia a él
        db.query(Reporte).filter(Reporte.validado_por_id == user.id).update(
            {Reporte.validado_por_id: None}, synchronize_session=False
        )
        db.query(ReporteValidacion).filter(ReporteValidacion.actualizado_por_id == user.id).update(
            {ReporteValidacion.actualizado_por_id: None}, synchronize_session=False
        )

        # Reportes propios: se borran sin aplicar la regla de los 7 días. El ORM elimina en
        # cascada videos, validación e infracciones; la base de datos, notificaciones y clips del dataset
        reportes = db.query(Reporte).filter(Reporte.usuario_id == user.id).all()
        carpetas = [(reporte.usuario_id, reporte.id) for reporte in reportes]
        for reporte in reportes:
            db.delete(reporte)

        # Códigos de recuperación de contraseña asociados a su correo
        db.query(PasswordResetCode).filter(PasswordResetCode.email == user.email).delete(
            synchronize_session=False
        )

        db.delete(user)
        # flush antes del commit para que cualquier error de la base de datos ocurra
        # antes de tocar archivos; si falla, el except hace rollback y no se borra nada
        db.flush()
        db.commit()

        # Los archivos se borran solo después del commit: si falla alguno, el usuario ya
        # quedó eliminado y se informa qué carpetas quedaron pendientes en vez de ocultarlo
        archivos_no_eliminados = []
        for usuario_id, reporte_id in carpetas:
            try:
                eliminar_carpeta_reporte(usuario_id, reporte_id)
            except Exception as error_archivo:
                print(f"No se pudo eliminar la carpeta del reporte {reporte_id} "
                      f"del usuario {usuario_id}: {error_archivo}")
                archivos_no_eliminados.append(reporte_id)

        # Si la carpeta del usuario quedó vacía, también se elimina
        carpeta_usuario = os.path.join(get_storage_root(), f"usuario_{user_data['id']}")
        try:
            if os.path.isdir(carpeta_usuario) and not os.listdir(carpeta_usuario):
                os.rmdir(carpeta_usuario)
        except OSError as error_carpeta:
            print(f"No se pudo eliminar la carpeta vacía del usuario {user_data['id']}: {error_carpeta}")

        if archivos_no_eliminados:
            print(f"Usuario {user_data['id']} eliminado, pero quedaron archivos sin borrar "
                  f"de los reportes: {archivos_no_eliminados}")

        user_data["archivos_no_eliminados"] = archivos_no_eliminados
        return [user_data, None]

    except Exception as error:
        print(f"Error al eliminar usuario: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]

async def get_perfil_service(db: Session, current_user_id: int):
    try:
        user = db.query(User).filter(User.id == current_user_id).first()
        if not user:
            return [None, "Usuario no encontrado"]
        return [{
            "id": user.id, "nombre_completo": user.nombre_completo,
            "rut": user.rut, "email": user.email, "rol": user.rol,
        }, None]
    except Exception as error:
        print(f"Error al obtener perfil: {error}")
        return [None, "Error interno del servidor"]


async def update_perfil_service(db: Session, current_user_id: int, body: dict):
    try:
        user = db.query(User).filter(User.id == current_user_id).first()
        if not user:
            return [None, "Usuario no encontrado"]

        if body.get("new_password"):
            if not body.get("password"):
                return [None, "Debes ingresar tu contraseña actual para cambiarla"]
            match = await compare_password(body["password"], user.password)
            if not match:
                return [None, "La contraseña actual no coincide"]
            user.password = await encrypt_password(body["new_password"])

        if body.get("nombre_completo"): user.nombre_completo = body["nombre_completo"]
        if body.get("email"): user.email = body["email"]

        db.commit()
        db.refresh(user)
        return [{
            "id": user.id, "nombre_completo": user.nombre_completo,
            "rut": user.rut, "email": user.email, "rol": user.rol,
        }, None]
    except Exception as error:
        print(f"Error al actualizar perfil: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]
