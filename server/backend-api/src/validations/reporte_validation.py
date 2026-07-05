from typing import Optional
from pydantic import BaseModel, field_validator


class ReporteValidarValidation(BaseModel):
    severidad_validada: str
    infraccion_ids: Optional[list[int]] = []
    infracciones_manuales: Optional[list[str]] = []
    notas_admin: Optional[str] = None

    @field_validator("severidad_validada")
    @classmethod
    def validate_severidad(cls, v):
        if v not in ("leve", "moderada", "grave"):
            raise ValueError("La severidad debe ser: leve, moderada o grave.")
        return v

    @field_validator("notas_admin")
    @classmethod
    def validate_notas(cls, v):
        if v is None:
            return v
        if len(v) > 1000:
            raise ValueError("Las notas no pueden superar los 1000 caracteres.")
        return v


class ReporteRechazarValidation(BaseModel):
    notas_admin: str  

    @field_validator("notas_admin")
    @classmethod
    def validate_notas(cls, v):
        if len(v.strip()) < 10:
            raise ValueError("Debes indicar un motivo de al menos 10 caracteres.")
        if len(v) > 1000:
            raise ValueError("Las notas no pueden superar los 1000 caracteres.")
        return v