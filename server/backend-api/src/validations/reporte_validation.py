import re
from typing import Optional
from pydantic import BaseModel, field_validator

MAX_INFRACCIONES_MANUALES = 10
MAX_LARGO_INFRACCION_MANUAL = 255
MAX_LARGO_PATENTE = 8


class ReporteValidarValidation(BaseModel):
    severidad_validada: str
    infraccion_ids: Optional[list[int]] = []
    infracciones_manuales: Optional[list[str]] = []
    patente_confirmada: Optional[str] = None
    notas_admin: Optional[str] = None

    @field_validator("infracciones_manuales")
    @classmethod
    def validate_manuales(cls, v):
        limpias = [t.strip() for t in (v or []) if t and t.strip()]
        if len(limpias) > MAX_INFRACCIONES_MANUALES:
            raise ValueError(f"Puedes agregar hasta {MAX_INFRACCIONES_MANUALES} infracciones manuales.")
        if any(len(t) > MAX_LARGO_INFRACCION_MANUAL for t in limpias):
            raise ValueError(
                f"Cada infracción manual puede tener hasta {MAX_LARGO_INFRACCION_MANUAL} caracteres."
            )
        return limpias

    @field_validator("patente_confirmada")
    @classmethod
    def validate_patente(cls, v):
        if v is None:
            return None
        limpia = re.sub(r"[^A-Z0-9]", "", v.upper())
        if not limpia:
            return None
        if len(limpia) > MAX_LARGO_PATENTE:
            raise ValueError(f"La patente no puede tener más de {MAX_LARGO_PATENTE} caracteres.")
        return limpia

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