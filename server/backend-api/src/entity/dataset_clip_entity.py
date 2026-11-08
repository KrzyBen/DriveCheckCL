from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, Table, text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from config.configDb import Base


class DatasetClip(Base):
    __tablename__ = "dataset_clips"

    id = Column(Integer, primary_key=True, autoincrement=True)
    video_id = Column(Integer, ForeignKey("reporte_videos.id", ondelete="CASCADE"),
                      nullable=False, unique=True, index=True)
    etiquetas_propias = Column(Boolean, nullable=False, default=False, server_default=text("false"))
    severidad = Column(String(20), nullable=True)
    excluido = Column(Boolean, nullable=False, default=False, server_default=text("false"))
    editado_por_id = Column(Integer, ForeignKey("usuarios.id", ondelete="SET NULL"), nullable=True)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    infracciones = relationship("Infraccion", secondary="dataset_clip_infracciones",
                                order_by="Infraccion.id")


dataset_clip_infracciones = Table(
    "dataset_clip_infracciones", Base.metadata,
    Column("clip_id", Integer, ForeignKey("dataset_clips.id", ondelete="CASCADE"), primary_key=True),
    Column("infraccion_id", Integer, ForeignKey("infracciones_catalogo.id", ondelete="CASCADE"), primary_key=True),
)
