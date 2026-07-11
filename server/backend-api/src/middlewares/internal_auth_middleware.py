from fastapi import Header, HTTPException, status
from config.configEnv import INTERNAL_API_KEY


def verify_internal_key(x_internal_key: str = Header(default=None)):
    if not INTERNAL_API_KEY:
        # Si no se configuró el secreto, se bloquea por seguridad en vez de dejar pasar todo.
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Canal interno no configurado (falta INTERNAL_API_KEY)."
        )
    if x_internal_key != INTERNAL_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Clave interna inválida."
        )
    return True
