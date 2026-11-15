from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey
from sqlalchemy.sql import func
from config.configDb import Base


class Notificacion(Base):
    __tablename__ = "notificaciones"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)

    usuario_id = Column(
        Integer, ForeignKey("usuarios.id", ondelete="CASCADE"),
        nullable=False, index=True
    )

    # Nullable porque a futuro podríamos querer notificaciones que no
    # apunten a un reporte específico.
    reporte_id = Column(
        Integer, ForeignKey("reportes.id", ondelete="CASCADE"),
        nullable=True
    )

    mensaje = Column(String(255), nullable=False)
    leida = Column(Boolean, nullable=False, default=False)

    created_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False
    )

    def __repr__(self):
        return f"<Notificacion usuario_id={self.usuario_id} leida={self.leida}>"
