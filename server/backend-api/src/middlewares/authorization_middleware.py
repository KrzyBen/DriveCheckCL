from fastapi import Depends, HTTPException, status
from sqlalchemy.orm import Session

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authentication_middleware import authenticate_jwt


def is_admin(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db)
) -> User:
    user = db.query(User).filter(User.email == current_user.email).first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuario no encontrado en la base de datos"
        )

    if user.rol != "administrador":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Se requiere un rol de administrador para realizar esta acción."
        )

    return user


def is_validador(
    current_user: User = Depends(authenticate_jwt),
    db: Session = Depends(get_db)
) -> User:
    user = db.query(User).filter(User.email == current_user.email).first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuario no encontrado en la base de datos"
        )

    if user.rol != "validador":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Se requiere un rol de validador para realizar esta acción."
        )

    return user
