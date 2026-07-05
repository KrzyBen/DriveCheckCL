import os
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

PORT = int(os.getenv("PORT", 8000))
HOST = os.getenv("HOST", "localhost")
DB_USERNAME = os.getenv("DB_USERNAME")
PASSWORD = os.getenv("PASSWORD")
DATABASE = os.getenv("DATABASE")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", 5432))
ACCESS_TOKEN_SECRET = os.getenv("ACCESS_TOKEN_SECRET")
COOKIE_KEY = os.getenv("COOKIE_KEY")
FRONTEND_URL = os.getenv("FRONTEND_URL", "http://localhost:5173")
STORAGE_PATH = os.getenv("STORAGE_PATH", "/data/reportes")
CORS_ORIGINS = os.getenv("CORS_ORIGINS", "http://localhost:5173").split(",")