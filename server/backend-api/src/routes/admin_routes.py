from fastapi import APIRouter, Depends
from middlewares.authentication_middleware import authenticate_jwt
from middlewares.authorization_middleware import is_admin
from controllers.user_controller import create_user_admin, get_users, get_user, update_user, delete_user
from routes.admin_reporte_routes import router as admin_reporte_router

router = APIRouter()

_solo_admin = [Depends(authenticate_jwt), Depends(is_admin)]

# Rutas del administrador para la gestión de usuarios (exclusivas de rol administrador)
router.add_api_route("/",        get_users,        methods=["GET"],    dependencies=_solo_admin)
router.add_api_route("/",        create_user_admin, methods=["POST"],   dependencies=_solo_admin)
router.add_api_route("/detail",  get_user,         methods=["GET"],    dependencies=_solo_admin)
router.add_api_route("/detail",  update_user,      methods=["PATCH"],  dependencies=_solo_admin)
router.add_api_route("/detail",  delete_user,      methods=["DELETE"], dependencies=_solo_admin)

# Rutas de reportes, con su propio control de acceso definido en admin_reporte_routes.py
router.include_router(admin_reporte_router, prefix="/reportes", tags=["Admin - Reportes"])