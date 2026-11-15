import posixpath
from urllib.parse import parse_qs

from fastapi.staticfiles import StaticFiles
from starlette.exceptions import HTTPException

# Las carpetas privadas (dataset*) se definen en url_firmada_helper, que tampoco las firma.
from helpers.url_firmada_helper import CARPETAS_PRIVADAS, firma_valida


def _parametro_unico(consulta, nombre):
    """Valor del parámetro si aparece exactamente una vez; None si falta o viene repetido."""
    valores = consulta.get(nombre, [])
    return valores[0] if len(valores) == 1 else None


class StorageFiles(StaticFiles):
    """
    StaticFiles que solo sirve archivos con una URL firmada vigente (ver url_firmada_helper):
    - carpetas privadas (dataset*): 404, también con rutas disfrazadas;
    - ruta con un segmento "..": 403;
    - sin firma, firma inválida o vencida: 403 (incluye los video_N.json, que nunca se firman).
    Con una URL válida se responde como StaticFiles, con soporte de Range (206).
    """

    async def get_response(self, path, scope):
        # 1) Carpetas privadas, como antes. Se normaliza igual que lo hace StaticFiles al buscar
        # el archivo, para que "./dataset/x", "usuario_1/../dataset/x" o "//dataset/x" no
        # esquiven el bloqueo. Va primero: el dataset responde 404 con o sin firma.
        limpio = posixpath.normpath("/" + path.replace("\\", "/")).lstrip("/")
        if limpio.split("/")[0] in CARPETAS_PRIVADAS:
            raise HTTPException(status_code=404)

        # 2) `path` ya viene normalizado por StaticFiles, así que el ".." se busca en la ruta
        # original de la petición (ya decodificada: "%2e%2e" también llega como "..").
        if ".." in scope["path"].replace("\\", "/").split("/"):
            raise HTTPException(status_code=403)

        # 3) Firma: cubre la ruta y la expiración (firma_valida compara en tiempo constante).
        # Se responde 403 antes de mirar el disco: sin firma no se revela si el archivo existe.
        consulta = parse_qs(scope.get("query_string", b"").decode("latin-1"))
        exp = _parametro_unico(consulta, "exp")
        sig = _parametro_unico(consulta, "sig")
        if not firma_valida(limpio, exp, sig):
            raise HTTPException(status_code=403)

        return await super().get_response(path, scope)
