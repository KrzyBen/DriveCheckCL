from sqlalchemy.orm import Session
from config.configDb import SessionLocal
from entity.user_entity import User
from entity.reporte_entity import Infraccion
from helpers.bcrypt_helper import encrypt_password


async def create_users():
    try:
        db: Session = SessionLocal()

        count = db.query(User).count()
        if count > 0:
            db.close()
            return

        users_to_create = [
            {
                "nombre_completo": "Administrador DriveCheckCL",
                "rut": "21.308.770-3",
                "email": "admin@drivecheckcl.cl",
                "password": await encrypt_password("Admin1234"),
                "rol": "administrador",
            },
            {
                "nombre_completo": "Validador DriveCheckCL",
                "rut": "22.111.222-1",
                "email": "validador@drivecheckcl.cl",
                "password": await encrypt_password("Valid1234"),
                "rol": "validador",
            },
            {
                "nombre_completo": "Usuario Prueba",
                "rut": "20.630.735-8",
                "email": "usuario@drivecheckcl.cl",
                "password": await encrypt_password("User1234"),
                "rol": "conductor",
            },
        ]

        for u in users_to_create:
            user = User(**u)
            db.add(user)

        db.commit()
        print("* => Usuarios iniciales creados exitosamente")

    except Exception as error:
        print(f"Error al crear usuarios: {error}")
        db.rollback()
    finally:
        db.close()

async def create_infracciones_catalogo():
    try:
        db: Session = SessionLocal()
 
        count = db.query(Infraccion).count()
        if count > 0:
            db.close()
            return
 
        infracciones_iniciales = [
            {"articulo": "Art. 141", "descripcion": "No respetar luz roja"},
            {"articulo": "Art. 144", "descripcion": "Exceso de velocidad"},
            {"articulo": "Art. 118", "descripcion": "No mantener distancia de seguimiento"},
            {"articulo": "Art. 114", "descripcion": "Adelantamiento indebido"},
            {"articulo": "Art. 197", "descripcion": "Conducción en estado de ebriedad"},
        ]
 
        for i in infracciones_iniciales:
            db.add(Infraccion(**i))
 
        db.commit()
        print("* => Catálogo de infracciones creado exitosamente")
 
    except Exception as error:
        print(f"Error al crear catálogo de infracciones: {error}")
        db.rollback()
    finally:
        db.close()