from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum as SqlEnum
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from config.configDb import Base
import enum


class EstadoReporte(str, enum.Enum):
    enviado    = "enviado"
    recibido   = "recibido"
    analizando = "analizando"
    validando  = "validando"
    resultados = "resultados"


class Reporte(Base):
    __tablename__ = "reportes"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)

    usuario_id = Column(Integer, ForeignKey("usuarios.id", ondelete="CASCADE"), nullable=False, index=True)

    titulo = Column(String(255), nullable=False)

    comentario = Column(String(1000), nullable=True)

    estado = Column(SqlEnum(EstadoReporte), nullable=False, default=EstadoReporte.enviado)

    pdf_path = Column(String(500), nullable=True)

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

    videos = relationship(
        "VideoReporte",
        back_populates="reporte",
        cascade="all, delete-orphan",
        order_by="VideoReporte.orden"
    )

    def __repr__(self):
        return f"<Reporte id={self.id} usuario_id={self.usuario_id} estado={self.estado}>"


class VideoReporte(Base):
    __tablename__ = "reporte_videos"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)

    reporte_id = Column(Integer, ForeignKey("reportes.id", ondelete="CASCADE"), nullable=False, index=True)

    video_path = Column(String(500), nullable=False)

    orden = Column(Integer, nullable=False, default=1)

    reporte = relationship("Reporte", back_populates="videos")

    def __repr__(self):
        return f"<VideoReporte id={self.id} reporte_id={self.reporte_id} orden={self.orden}>"