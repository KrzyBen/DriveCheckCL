from pydantic import BaseModel, StrictBool, StrictInt


class ConsentimientoValidation(BaseModel):
    # Estrictos a propósito: un consentimiento no debe salir de una conversión
    # implícita ("true", 1, etc.); tiene que llegar un booleano real.
    acepto_estudio: StrictBool
    # Versión del texto que el usuario vio al decidir.
    version: StrictInt
