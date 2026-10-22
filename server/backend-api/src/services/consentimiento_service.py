from datetime import datetime, timezone
from sqlalchemy.orm import Session

from config.consentimiento import CONSENTIMIENTO_VERSION_VIGENTE
from entity.user_entity import User


def _estado(user: User) -> dict:
    version_aceptada = user.consentimiento_version
    return {
        "acepto_estudio": bool(user.consentimiento_estudio),
        "version_aceptada": version_aceptada,
        "fecha": user.consentimiento_fecha.isoformat() if user.consentimiento_fecha else None,
        "version_vigente": CONSENTIMIENTO_VERSION_VIGENTE,
        # True si nunca respondió o respondió a una versión anterior del texto.
        "requiere_decision": version_aceptada is None
        or version_aceptada < CONSENTIMIENTO_VERSION_VIGENTE,
    }


async def obtener_consentimiento_service(db: Session, usuario_id: int):
    try:
        user = db.query(User).filter(User.id == usuario_id).first()
        if not user:
            return [None, {"dataInfo": "usuario", "message": "Usuario no encontrado"}]
        return [_estado(user), None]
    except Exception as error:
        print(f"Error al obtener consentimiento: {error}")
        return [None, "Error interno del servidor"]


async def guardar_consentimiento_service(
    db: Session, usuario_id: int, acepto_estudio: bool, version: int
):
    try:
        # El usuario decidió sobre un texto que ya no es el vigente: no se guarda.
        if version != CONSENTIMIENTO_VERSION_VIGENTE:
            return [None, {
                "dataInfo": "version",
                "message": "El texto de consentimiento cambió; vuelve a revisarlo",
                "version_vigente": CONSENTIMIENTO_VERSION_VIGENTE,
            }]

        user = db.query(User).filter(User.id == usuario_id).first()
        if not user:
            return [None, {"dataInfo": "usuario", "message": "Usuario no encontrado"}]

        user.consentimiento_estudio = acepto_estudio
        user.consentimiento_version = version
        user.consentimiento_fecha = datetime.now(timezone.utc)
        db.commit()
        db.refresh(user)
        return [_estado(user), None]
    except Exception as error:
        db.rollback()
        print(f"Error al guardar consentimiento: {error}")
        return [None, "Error interno del servidor"]
