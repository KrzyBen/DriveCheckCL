from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from config.configDb import init_db
from config.initialSetup import create_users
from config.configEnv import settings
from routes.index_routes import router


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    await create_users()
    print(f"=> Servidor corriendo en {settings.HOST}:{settings.PORT}/api")
    yield


app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/api")


@app.get("/health")
async def health_check():
    return {"status": "ok"}