from fastapi import APIRouter, Depends
from middlewares.internal_auth_middleware import verify_internal_key
from controllers.internal_controller import recibir_resultado_validacion

router = APIRouter(dependencies=[Depends(verify_internal_key)])

router.add_api_route(
    "/reportes/{reporte_id}/validacion",
    recibir_resultado_validacion,
    methods=["PATCH"],
)
