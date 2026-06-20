from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    HOST: str = "localhost"
    PORT: int = 8000
    DB_USERNAME: str
    PASSWORD: str
    DATABASE: str
    DB_HOST: str = "localhost"
    DB_PORT: int = 5432
    ACCESS_TOKEN_SECRET: str
    COOKIE_KEY: str
    FRONTEND_URL: str = "http://localhost:5173"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
