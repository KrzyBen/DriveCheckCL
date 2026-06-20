from fastapi import APIRouter
from sqlalchemy.orm import Session

from config.configDb import get_db
from controllers.auth_controller import login, register, logout
from routes.reporte_routes import router as reporte_router

router = APIRouter()
#Rutas de autenticación
router.add_api_route("/login",    login,    methods=["POST"])
router.add_api_route("/register", register, methods=["POST"])
router.add_api_route("/logout",   logout,   methods=["POST"])

#Rutas de uso
#Añadir funciones de [Traer datos de usuario, traer reportes si es que hay
#descarga de reportes, subida de videos]
router.include_router(reporte_router, prefix="/reporte", tags=["Reportes"])