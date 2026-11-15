"""
URLs firmadas para los archivos de storage/ (videos y PDF de los reportes).

El backend firma la URL solo al devolver un reporte a quien puede verlo; quien la tenga puede
descargar ese archivo hasta que venza. La firma (HMAC-SHA256) cubre la ruta y la expiración:
cambiar cualquiera de las dos la invalida.

Formato: /storage/<ruta>?exp=<unix>&sig=<base64url sin relleno>
"""
import base64
import hashlib
import hmac
import math
import time
from urllib.parse import quote

from config.configEnv import ACCESS_TOKEN_SECRET

# Carpetas de storage/ que NUNCA se sirven por HTTP ni se firman (el dataset solo lo lee el
# administrador en el servidor). dataset.nuevo, dataset.anterior y dataset.lock los usa la
# exportación (dataset_service): contienen lo mismo que dataset y tampoco pueden quedar expuestos.
CARPETAS_PRIVADAS = ("dataset", "dataset.nuevo", "dataset.anterior", "dataset.lock")

DURACION_S = 3600  # vigencia mínima de una URL
TRAMO_S = 600      # la expiración se redondea a tramos de 10 min: durante el tramo el mismo
                   # archivo tiene la misma URL (el polling del panel no recarga el <video>
                   # y el navegador puede reutilizar lo que ya tiene en caché)

if not ACCESS_TOKEN_SECRET:
    raise RuntimeError("Falta ACCESS_TOKEN_SECRET: sin él no se pueden firmar las URLs de storage")

# Clave propia derivada del secreto del JWT, con una etiqueta: una firma de URL nunca vale
# como firma de un token ni al revés. Si se rota ACCESS_TOKEN_SECRET, las URLs vigentes vencen.
_CLAVE = hmac.new(ACCESS_TOKEN_SECRET.encode(), b"drivecheck:storage-urls:v1", hashlib.sha256).digest()


def ruta_permitida(ruta) -> bool:
    """Relativa, sin '..', '.', segmentos vacíos, '\\' ni NUL, y fuera de las carpetas privadas."""
    if not isinstance(ruta, str) or not ruta or ruta.startswith("/") or "\\" in ruta or "\x00" in ruta:
        return False
    partes = ruta.split("/")
    if any(parte in ("", ".", "..") for parte in partes):
        return False
    return partes[0] not in CARPETAS_PRIVADAS


def _firma(ruta: str, exp: int) -> str:
    # El salto de línea separa ruta y expiración: ninguna ruta permitida lo contiene
    mac = hmac.new(_CLAVE, f"{ruta}\n{exp}".encode(), hashlib.sha256).digest()
    return base64.urlsafe_b64encode(mac).rstrip(b"=").decode()


def url_firmada(ruta, ahora=None):
    """'/storage/<ruta>?exp=..&sig=..' o None si no hay ruta o no se permite servirla."""
    if not ruta_permitida(ruta):
        return None
    ahora = time.time() if ahora is None else ahora
    exp = math.ceil((ahora + DURACION_S) / TRAMO_S) * TRAMO_S
    return f"/storage/{quote(ruta)}?exp={exp}&sig={_firma(ruta, exp)}"


def firma_valida(ruta, exp, sig, ahora=None) -> bool:
    """True si la firma corresponde a la ruta y la expiración, y todavía no vence."""
    if not ruta_permitida(ruta) or not isinstance(exp, str) or not isinstance(sig, str):
        return False
    # Solo dígitos ASCII y un largo razonable (un timestamp tiene 10)
    if not (exp.isascii() and exp.isdigit() and len(exp) <= 12):
        return False
    ahora = time.time() if ahora is None else ahora
    if int(exp) <= ahora:
        return False
    # Comparación en tiempo constante; en bytes para que una firma con caracteres raros
    # devuelva False en vez de lanzar TypeError
    return hmac.compare_digest(_firma(ruta, int(exp)).encode(), sig.encode())
