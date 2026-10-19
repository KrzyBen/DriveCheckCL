from fastapi import APIRouter, Depends
from middlewares.authentication_middleware import authenticate_jwt
from middlewares.authorization_middleware import is_admin
from controllers.mapa_controller import obtener_mapa

router = APIRouter(dependencies=[Depends(authenticate_jwt), Depends(is_admin)])

router.add_api_route("/", obtener_mapa, methods=["GET"])
