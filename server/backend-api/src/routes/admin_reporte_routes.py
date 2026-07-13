from fastapi import APIRouter, Depends
from middlewares.authentication_middleware import authenticate_jwt
from middlewares.authorization_middleware import is_admin
from controllers.admin_reporte_controller import (
    listar_reportes_admin, analizar_reporte, get_reporte_admin, validar_reporte, rechazar_reporte, eliminar_reporte_admin, listar_infracciones_catalogo,
)

router = APIRouter(dependencies=[Depends(authenticate_jwt), Depends(is_admin)])

router.add_api_route("/",                      listar_reportes_admin,  methods=["GET"])
router.add_api_route("/infracciones",          listar_infracciones_catalogo, methods=["GET"])
router.add_api_route("/{reporte_id}",          get_reporte_admin,      methods=["GET"])
router.add_api_route("/{reporte_id}/analizar", analizar_reporte,       methods=["POST"])
router.add_api_route("/{reporte_id}/validar",  validar_reporte,        methods=["PATCH"])
router.add_api_route("/{reporte_id}/rechazar", rechazar_reporte,       methods=["PATCH"])
router.add_api_route("/{reporte_id}",          eliminar_reporte_admin, methods=["DELETE"])