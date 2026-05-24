from fastapi import APIRouter, Depends
from middlewares.authentication_middleware import authenticate_jwt
from middlewares.authorization_middleware import is_admin
from controllers.user_controller import get_users, get_user, update_user, delete_user
 
router = APIRouter(dependencies=[Depends(authenticate_jwt), Depends(is_admin)])
 
router.add_api_route("/",        get_users,   methods=["GET"])
router.add_api_route("/detail",  get_user,    methods=["GET"])
router.add_api_route("/detail",  update_user, methods=["PATCH"])
router.add_api_route("/detail",  delete_user, methods=["DELETE"])