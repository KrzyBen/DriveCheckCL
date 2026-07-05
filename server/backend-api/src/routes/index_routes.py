from fastapi import APIRouter
from routes.auth_routes import router as auth_router
from routes.admin_routes import router as user_router
from routes.app_routes import router as app_router

router = APIRouter()
 
router.include_router(auth_router, prefix="/auth", tags=["Auth"])
router.include_router(user_router, prefix="/admin", tags=["Admin"])
router.include_router(app_router, prefix="/app", tags=["App"])