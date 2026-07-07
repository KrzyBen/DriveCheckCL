from fastapi import APIRouter, Depends

from middlewares.authentication_middleware import authenticate_jwt
from controllers.reporte_controller import (
    crear_reporte,
    listar_mis_reportes,
    get_reporte,
    eliminar_reporte,
    contar_reportes,
)

router = APIRouter(dependencies=[Depends(authenticate_jwt)])

router.add_api_route("/contar",        contar_reportes,     methods=["GET"])
router.add_api_route("/crear",         crear_reporte,       methods=["POST"])
router.add_api_route("/mis-reportes",  listar_mis_reportes, methods=["GET"])
router.add_api_route("/{reporte_id}",  get_reporte,         methods=["GET"])
router.add_api_route("/{reporte_id}",  eliminar_reporte,    methods=["DELETE"])
