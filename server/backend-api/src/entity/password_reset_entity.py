from sqlalchemy import Column, Integer, String, DateTime, Boolean
from sqlalchemy.sql import func
from config.configDb import Base


class PasswordResetCode(Base):
    __tablename__ = "password_reset_codes"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)

    email = Column(String(255), nullable=False, index=True)

    # Hash del código de 6 dígitos (bcrypt, igual que las contraseñas). Nunca
    # se guarda el código en texto plano.
    code_hash = Column(String(255), nullable=False)

    expires_at = Column(DateTime(timezone=True), nullable=False)

    used = Column(Boolean, nullable=False, default=False)

    created_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False
    )

    def __repr__(self):
        return f"<PasswordResetCode email={self.email} used={self.used}>"
