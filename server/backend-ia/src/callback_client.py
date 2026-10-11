import asyncio

import httpx

from config import BACKEND_API_URL, INTERNAL_API_KEY

# Si backend-api esta reiniciando o ocupado justo cuando termina el analisis,
# un solo intento perdia el resultado. Se reintenta unas pocas veces.
INTENTOS = 3
ESPERA_ANTES_DEL_REINTENTO = (2, 5)  # segundos antes del 2do y del 3er intento


def _es_reintentable(error: httpx.HTTPError) -> bool:
    """
    Reintentar sirve para fallos pasajeros: timeouts, conexion rechazada y
    errores 5xx (mas 408/429). Un 401 o un 400 va a fallar igual la proxima
    vez, asi que no se reintenta.
    """
    if isinstance(error, httpx.HTTPStatusError):
        codigo = error.response.status_code
        return codigo >= 500 or codigo in (408, 429)
    return True


async def enviar_resultado(reporte_id: int, estado_analisis: str, resultados_ia: dict | None = None) -> bool:
    """Devuelve True si backend-api recibio el resultado, False si no se logro."""
    url = f"{BACKEND_API_URL}/internal/reportes/{reporte_id}/validacion"
    body = {"estado_analisis": estado_analisis}
    if resultados_ia is not None:
        body["resultados_ia"] = resultados_ia

    for intento in range(1, INTENTOS + 1):
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                respuesta = await client.patch(
                    url, json=body, headers={"X-Internal-Key": INTERNAL_API_KEY or ""}
                )
                respuesta.raise_for_status()
            return True
        except httpx.HTTPError as error:
            print(f"[callback] reporte {reporte_id}: intento {intento}/{INTENTOS} fallo: {error}")
            if intento == INTENTOS or not _es_reintentable(error):
                break
            await asyncio.sleep(ESPERA_ANTES_DEL_REINTENTO[intento - 1])

    # backend-api se queda esperando; el admin puede reintentar el analisis
    # manualmente desde el panel.
    print(f"No se pudo enviar el resultado del reporte {reporte_id} a backend-api.")
    return False