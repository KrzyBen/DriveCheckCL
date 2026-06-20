import re
from pydantic import BaseModel, EmailStr, field_validator


class LoginValidation(BaseModel):
    email: EmailStr
    password: str

    @field_validator("email")
    @classmethod
    def validate_email_domain(cls, v):
        if not v.endswith("@drivecheckcl.cl") and not v.endswith("@gmail.com"):
            raise ValueError("El correo debe ser válido")
        if len(v) < 10 or len(v) > 60:
            raise ValueError("El correo debe tener entre 10 y 60 caracteres.")
        return v

    @field_validator("password")
    @classmethod
    def validate_password(cls, v):
        if len(v) < 8:
            raise ValueError("La contraseña debe tener al menos 8 caracteres.")
        if len(v) > 26:
            raise ValueError("La contraseña debe tener como máximo 26 caracteres.")
        if not re.match(r'^[a-zA-Z0-9]+$', v):
            raise ValueError("La contraseña solo puede contener letras y números.")
        return v


class RegisterValidation(BaseModel):
    nombre_completo: str
    rut: str
    email: EmailStr
    password: str

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
        pattern = r'^(?:(?:[1-9]\d{0}|[1-2]\d{1})(\.\d{3}){2}|[1-9]\d{6}|[1-2]\d{7}|29\.999\.999|29999999)-[\dkK]$'
        if not re.match(pattern, v):
            raise ValueError("Formato rut inválido, debe ser xx.xxx.xxx-x o xxxxxxxx-x.")
        if len(v) < 9 or len(v) > 12:
            raise ValueError("El rut debe tener entre 9 y 12 caracteres.")
        return v

    @field_validator("email")
    @classmethod
    def validate_email(cls, v):
        if len(v) < 10 or len(v) > 60:
            raise ValueError("El correo debe tener entre 10 y 60 caracteres.")
        return v

    @field_validator("password")
    @classmethod
    def validate_password(cls, v):
        if len(v) < 8:
            raise ValueError("La contraseña debe tener al menos 8 caracteres.")
        if len(v) > 26:
            raise ValueError("La contraseña debe tener como máximo 26 caracteres.")
        if not re.match(r'^[a-zA-Z0-9]+$', v):
            raise ValueError("La contraseña solo puede contener letras y números.")
        return v
