from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from controllers.auth_controller import login, register, logout

router = APIRouter()

router.add_api_route("/login",    login,    methods=["POST"])
router.add_api_route("/register", register, methods=["POST"])
router.add_api_route("/logout",   logout,   methods=["POST"])