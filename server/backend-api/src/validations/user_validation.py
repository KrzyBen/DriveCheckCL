import re
from typing import Optional
from pydantic import BaseModel, EmailStr, field_validator
from helpers.rut_helper import normalizar_rut


class UserQueryValidation(BaseModel):
    id: Optional[int] = None
    email: Optional[EmailStr] = None
    rut: Optional[str] = None

    @field_validator("rut")
    @classmethod
    def validate_rut(cls, v):
        if v is None:
            return v
        # Valida formato y dígito verificador, y guarda siempre el formato xx.xxx.xxx-x
        return normalizar_rut(v, "Formato rut inválido, debe ser xx.xxx.xxx-x o xxxxxxxx-x.")

    def at_least_one(self):
        return any([self.id, self.email, self.rut])


class UserBodyValidation(BaseModel):
    nombre_completo: Optional[str] = None
    email: Optional[EmailStr] = None
    rut: Optional[str] = None
    rol: Optional[str] = None
    password: Optional[str] = None
    new_password: Optional[str] = None

    @field_validator("nombre_completo")
    @classmethod
    def validate_nombre(cls, v):
        if v is None:
            return v
        if len(v) < 10 or len(v) > 50:
            raise ValueError("El nombre debe tener entre 10 y 50 caracteres.")
        if not re.match(r'^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$', v):
            raise ValueError("El nombre solo puede contener letras y espacios.")
        return v

    @field_validator("rut")
    @classmethod
    def validate_rut(cls, v):
        if v is None:
            return v
        # Valida formato y dígito verificador, y guarda siempre el formato xx.xxx.xxx-x
        return normalizar_rut(v, "Formato rut inválido.")

    @field_validator("password", "new_password")
    @classmethod
    def validate_password(cls, v):
        if v is None or v == "":
            return v
        if len(v) < 8 or len(v) > 26:
            raise ValueError("La contraseña debe tener entre 8 y 26 caracteres.")
        if not re.match(r'^[a-zA-Z0-9]+$', v):
            raise ValueError("La contraseña solo puede contener letras y números.")
        return v

    @field_validator("rol")
    @classmethod
    def validate_rol(cls, v):
        if v is None:
            return v
        roles_validos = ["administrador", "validador", "conductor"]
        if v not in roles_validos:
            raise ValueError(f"El rol debe ser uno de: {', '.join(roles_validos)}.")
        return v
    

class UserCreateValidation(BaseModel):
    nombre_completo: str
    email: EmailStr
    rut: str
    password: str
    rol: Optional[str] = "conductor"

    @field_validator("nombre_completo")
    @classmethod
    def validate_nombre(cls, v):
        if len(v) < 10 or len(v) > 50:
            raise ValueError("El nombre debe tener entre 10 y 50 caracteres.")
        if not re.match(r'^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$', v):
            raise ValueError("El nombre solo puede contener letras y espacios.")
        return v

    @field_validator("rut")
    @classmethod
    def validate_rut(cls, v):
        # Valida formato y dígito verificador, y guarda siempre el formato xx.xxx.xxx-x
        return normalizar_rut(v, "Formato rut inválido.")

    @field_validator("password")
    @classmethod
    def validate_password(cls, v):
        if len(v) < 8 or len(v) > 26:
            raise ValueError("La contraseña debe tener entre 8 y 26 caracteres.")
        if not re.match(r'^[a-zA-Z0-9]+$', v):
            raise ValueError("La contraseña solo puede contener letras y números.")
        return v

    @field_validator("rol")
    @classmethod
    def validate_rol(cls, v):
        if v is None:
            return "conductor"
        roles_validos = ["administrador", "validador", "conductor"]
        if v not in roles_validos:
            raise ValueError(f"El rol debe ser uno de: {', '.join(roles_validos)}.")
        return v
