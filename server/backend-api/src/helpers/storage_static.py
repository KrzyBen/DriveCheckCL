import posixpath

from fastapi.staticfiles import StaticFiles
from starlette.exceptions import HTTPException

# Carpetas de storage/ que NUNCA se sirven por HTTP (el dataset solo lo lee el administrador
# en el servidor). Hoy el resto de storage/ sigue público: ver "E6 · Seguridad" en el estado.
# dataset.nuevo, dataset.anterior y dataset.lock los usa la exportación (dataset_service):
# el dataset se arma en dataset.nuevo y al final reemplaza a dataset, así que contienen
# lo mismo que él y tampoco pueden quedar expuestos.
CARPETAS_PRIVADAS = ("dataset", "dataset.nuevo", "dataset.anterior", "dataset.lock")


class StorageFiles(StaticFiles):
    """StaticFiles que responde 404 para las carpetas privadas, también con rutas disfrazadas."""

    async def get_response(self, path, scope):
        # Se normaliza igual que lo hace StaticFiles al buscar el archivo, para que
        # "./dataset/x", "usuario_1/../dataset/x" o "//dataset/x" no esquiven el bloqueo.
        limpio = posixpath.normpath("/" + path.replace("\\", "/")).lstrip("/")
        if limpio.split("/")[0] in CARPETAS_PRIVADAS:
            raise HTTPException(status_code=404)
        return await super().get_response(path, scope)
