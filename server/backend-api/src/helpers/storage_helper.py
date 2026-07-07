import os
import shutil
from config.configEnv import STORAGE_PATH

STORAGE_ROOT = STORAGE_PATH

def get_storage_root() -> str:
    os.makedirs(STORAGE_ROOT, exist_ok=True)
    return STORAGE_ROOT


def get_reporte_dir(usuario_id: int, reporte_id: int) -> str:
    path = os.path.join(
        get_storage_root(),
        f"usuario_{usuario_id}",
        f"reporte_{reporte_id}"
    )
    os.makedirs(path, exist_ok=True)
    return path


def guardar_video(usuario_id: int, reporte_id: int, orden: int, archivo) -> str:
    dir_path = get_reporte_dir(usuario_id, reporte_id)
    nombre_archivo = f"video_{orden}.mp4"
    ruta_absoluta = os.path.join(dir_path, nombre_archivo)

    with open(ruta_absoluta, "wb") as buffer:
        shutil.copyfileobj(archivo.file, buffer)

    ruta_relativa = os.path.join(
        f"usuario_{usuario_id}", f"reporte_{reporte_id}", nombre_archivo
    )
    return ruta_relativa


def get_ruta_absoluta(ruta_relativa: str) -> str:
    return os.path.join(get_storage_root(), ruta_relativa)


def eliminar_carpeta_reporte(usuario_id: int, reporte_id: int) -> bool:
    path = os.path.join(get_storage_root(), f"usuario_{usuario_id}", f"reporte_{reporte_id}")
    if os.path.exists(path):
        shutil.rmtree(path)
        return True
    return False