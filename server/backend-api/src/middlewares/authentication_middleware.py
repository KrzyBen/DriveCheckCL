from fastapi import Depends
from auth.passport_auth import verify_token
from entity.user_entity import User


def authenticate_jwt(current_user: User = Depends(verify_token)) -> User:
    return current_user
