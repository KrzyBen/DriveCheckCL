from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from config.configDb import init_db
from config.initialSetup import create_users, create_infracciones_catalogo
from config.configEnv import HOST, PORT, CORS_ORIGINS
from routes.index_routes import router

import os
from fastapi.staticfiles import StaticFiles
from config.configEnv import STORAGE_PATH

@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    await create_users()
    await create_infracciones_catalogo()
    print(f"=> Servidor corriendo en {HOST}:{PORT}/api")
    yield


app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/api")

os.makedirs(STORAGE_PATH, exist_ok=True)
app.mount("/storage", StaticFiles(directory=STORAGE_PATH), name="storage")


@app.get("/health")
async def health_check():
    return {"status": "ok"}