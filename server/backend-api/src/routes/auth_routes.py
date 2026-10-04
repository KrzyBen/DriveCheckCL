from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from controllers.auth_controller import login, register, logout, forgot_password, reset_password

router = APIRouter()

router.add_api_route("/login",            login,            methods=["POST"])
router.add_api_route("/register",         register,         methods=["POST"])
router.add_api_route("/logout",           logout,           methods=["POST"])
router.add_api_route("/forgot-password",  forgot_password,  methods=["POST"])
router.add_api_route("/reset-password",   reset_password,   methods=["POST"])