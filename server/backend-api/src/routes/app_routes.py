from fastapi import APIRouter
from sqlalchemy.orm import Session

from config.configDb import get_db
from controllers.auth_controller import login, register, logout, forgot_password, reset_password
from controllers.notificacion_controller import listar_notificaciones, marcar_notificaciones_leidas
from routes.reporte_routes import router as reporte_router

router = APIRouter()
#Rutas de autenticación
router.add_api_route("/login",            login,            methods=["POST"])
router.add_api_route("/register",         register,         methods=["POST"])
router.add_api_route("/logout",           logout,           methods=["POST"])
router.add_api_route("/forgot-password",  forgot_password,  methods=["POST"])
router.add_api_route("/reset-password",   reset_password,   methods=["POST"])

#Rutas de notificaciones
router.add_api_route("/notificaciones",              listar_notificaciones,          methods=["GET"])
router.add_api_route("/notificaciones/marcar-leidas", marcar_notificaciones_leidas,  methods=["PATCH"])

#Rutas de uso
#Añadir funciones de [Traer datos de usuario, traer reportes si es que hay
#descarga de reportes, subida de videos]
router.include_router(reporte_router, prefix="/reporte", tags=["Reportes"])