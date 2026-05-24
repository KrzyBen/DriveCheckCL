from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from config.configEnv import settings


# URL de conexión a PostgreSQL
DATABASE_URL = (
    f"postgresql://{settings.DB_USERNAME}:{settings.PASSWORD}"
    f"@{settings.DB_HOST}:{settings.DB_PORT}/{settings.DATABASE}"
)

engine = create_engine(DATABASE_URL, echo=False)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def init_db():
    """Inicializa la base de datos — equivalente a AppDataSource.initialize()"""
    try:
        from entity.user_entity import User  # noqa: F401 — importar para registrar modelo
        Base.metadata.create_all(bind=engine)
        print("=> Conexión exitosa a la base de datos!")
    except Exception as error:
        print(f"Error al conectar con la base de datos: {error}")
        raise


def get_db():
    """Dependency de FastAPI para inyectar sesión DB en cada request"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
