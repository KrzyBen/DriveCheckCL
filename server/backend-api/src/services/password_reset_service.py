import random
from datetime import datetime, timedelta, timezone

from sqlalchemy.orm import Session

from entity.user_entity import User
from entity.password_reset_entity import PasswordResetCode
from helpers.bcrypt_helper import encrypt_password, compare_password
from helpers.email_helper import send_email
from config.configEnv import PASSWORD_RESET_CODE_EXPIRE_MINUTES


def _generar_codigo() -> str:
    return f"{random.randint(0, 999999):06d}"


async def request_password_reset_service(db: Session, email: str):
    try:
        user = db.query(User).filter(User.email == email).first()

        if not user:
            return [True, None]

        # Invalida cualquier código anterior sin usar de este correo, para
        # que solo el último código enviado sea válido.
        db.query(PasswordResetCode).filter(
            PasswordResetCode.email == email,
            PasswordResetCode.used == False,
        ).update({"used": True})

        codigo = _generar_codigo()
        code_hash = await encrypt_password(codigo)

        reset_entry = PasswordResetCode(
            email=email,
            code_hash=code_hash,
            expires_at=datetime.now(timezone.utc) + timedelta(minutes=PASSWORD_RESET_CODE_EXPIRE_MINUTES),
        )
        db.add(reset_entry)
        db.commit()

        await send_email(
            to=email,
            subject="Código de recuperación - DriveCheckCL",
            body=(
                f"Tu código de recuperación es: {codigo}\n\n"
                f"Vence en {PASSWORD_RESET_CODE_EXPIRE_MINUTES} minutos. "
                f"Si tú no solicitaste esto, puedes ignorar este correo."
            ),
        )

        return [True, None]
    except Exception as error:
        return [None, {"dataInfo": "email", "message": str(error)}]


async def confirm_password_reset_service(db: Session, email: str, code: str, new_password: str):
    try:
        def create_error(field: str, message: str):
            return {"dataInfo": field, "message": message}

        reset_entry = (
            db.query(PasswordResetCode)
            .filter(PasswordResetCode.email == email, PasswordResetCode.used == False)
            .order_by(PasswordResetCode.created_at.desc())
            .first()
        )

        if not reset_entry:
            return [None, create_error("code", "El código es inválido o ya fue utilizado")]

        if reset_entry.expires_at < datetime.now(timezone.utc):
            return [None, create_error("code", "El código ha expirado, solicita uno nuevo")]

        is_match = await compare_password(code, reset_entry.code_hash)
        if not is_match:
            return [None, create_error("code", "El código es incorrecto")]

        user = db.query(User).filter(User.email == email).first()
        if not user:
            return [None, create_error("email", "El correo electrónico es incorrecto")]

        user.password = await encrypt_password(new_password)
        reset_entry.used = True
        db.commit()

        return [True, None]
    except Exception as error:
        return [None, {"dataInfo": "password", "message": str(error)}]
