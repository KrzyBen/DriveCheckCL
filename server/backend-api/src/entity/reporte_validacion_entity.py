from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum as SqlEnum
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from config.configDb import Base
import enum


class EstadoAnalisis(str, enum.Enum):
    pendiente  = "pendiente"   # aún no se ha presionado "Analizar"
    analizando = "analizando"  # backend-ia está procesando el video
    completado = "completado"  # backend-ia ya devolvió resultados
    error      = "error"       # el análisis falló (ver detalle en resultados_ia)


class ReporteValidacion(Base):
    __tablename__ = "reporte_validacion"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    reporte_id = Column(
        Integer, ForeignKey("reportes.id", ondelete="CASCADE"),
        nullable=False, unique=True, index=True
    )

    estado_analisis = Column(SqlEnum(EstadoAnalisis), nullable=False, default=EstadoAnalisis.pendiente)

    # Resultados crudos de la IA, por capa. Ej:
    # {"accidentes": {"confianza": 0.87, "severidad": "grave"},
    #  "patente": {"texto": "ABCD12", "confianza": 0.91}}
    resultados_ia = Column(JSONB, nullable=True)

    # Campos "de trabajo": la IA propone un valor inicial, el admin puede editarlos
    # libremente antes de aprobar/rechazar el reporte
    patente_confirmada = Column(String(20), nullable=True)
    severidad_propuesta = Column(String(20), nullable=True)
    notas_ia = Column(String(1000), nullable=True)

    actualizado_en = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
    actualizado_por_id = Column(Integer, ForeignKey("usuarios.id"), nullable=True)

    reporte = relationship("Reporte", backref="validacion", uselist=False)
    actualizado_por = relationship("User", foreign_keys=[actualizado_por_id])

    def __repr__(self):
        return f"<ReporteValidacion reporte_id={self.reporte_id} estado_analisis={self.estado_analisis}>"
