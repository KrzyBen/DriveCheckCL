from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.sql import func
from config.configDb import Base


class User(Base):
    __tablename__ = "usuarios"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)

    nombre_completo = Column(String(255), nullable=False)

    rut = Column(String(12), nullable=False, unique=True, index=True)

    email = Column(String(255), nullable=False, unique=True, index=True)

    rol = Column(String(50), nullable=False, default="usuario")

    password = Column(String(255), nullable=False)

    created_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False
    )

    updated_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False
    )

    def __repr__(self):
        return f"<User id={self.id} email={self.email} rol={self.rol}>"
