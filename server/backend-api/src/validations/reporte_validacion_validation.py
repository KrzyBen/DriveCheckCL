from typing import Optional
from pydantic import BaseModel, field_validator


class ReporteValidacionCallback(BaseModel):
    estado_analisis: str
    resultados_ia: Optional[dict] = None
    patente_confirmada: Optional[str] = None
    severidad_propuesta: Optional[str] = None
    notas_ia: Optional[str] = None

    @field_validator("estado_analisis")
    @classmethod
    def validar_estado(cls, v):
        if v not in ("analizando", "completado", "error"):
            raise ValueError("estado_analisis debe ser: analizando, completado o error.")
        return v
