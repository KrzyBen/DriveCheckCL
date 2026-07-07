from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from config.configEnv import DB_USERNAME, PASSWORD, DATABASE, DB_HOST, DB_PORT


DATABASE_URL = (
    f"postgresql://{DB_USERNAME}:{PASSWORD}"
    f"@{DB_HOST}:{DB_PORT}/{DATABASE}"
)

engine = create_engine(DATABASE_URL, echo=False)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def init_db():
    try:
        from entity.user_entity import User
        from entity.reporte_entity import Reporte, VideoReporte
        Base.metadata.create_all(bind=engine)
        print("=> Conexión exitosa a la base de datos!")
    except Exception as error:
        print(f"Error al conectar con la base de datos: {error}")
        raise


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()