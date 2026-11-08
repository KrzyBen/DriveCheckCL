from typing import List, Optional

from pydantic import BaseModel, ConfigDict, StrictBool, StrictInt, field_validator, model_validator

SEVERIDADES = ("leve", "moderada", "grave")


class ClipEtiquetasValidation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    infracciones: Optional[List[StrictInt]] = None   # ids del catálogo; sin texto libre
    severidad: Optional[str] = None                  # None explícito = sin severidad
    excluido: Optional[StrictBool] = None

    @field_validator("severidad")
    @classmethod
    def validar_severidad(cls, v):
        if v is not None and v not in SEVERIDADES:
            raise ValueError("La severidad debe ser: leve, moderada o grave.")
        return v

    @model_validator(mode="after")
    def validar_no_vacio(self):
        if not self.model_fields_set:
            raise ValueError("Indica al menos un campo: infracciones, severidad o excluido.")
        if "infracciones" in self.model_fields_set and self.infracciones is None:
            raise ValueError("infracciones debe ser una lista de ids (puede ir vacía).")
        if "excluido" in self.model_fields_set and self.excluido is None:
            raise ValueError("excluido debe ser verdadero o falso.")
        return self
