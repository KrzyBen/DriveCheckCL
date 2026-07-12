import httpx

from config import BACKEND_API_URL, INTERNAL_API_KEY


async def enviar_resultado(reporte_id: int, estado_analisis: str, resultados_ia: dict | None = None):
    url = f"{BACKEND_API_URL}/internal/reportes/{reporte_id}/validacion"
    body = {"estado_analisis": estado_analisis}
    if resultados_ia is not None:
        body["resultados_ia"] = resultados_ia

    try:
        async with httpx.AsyncClient(timeout=15) as client:
            respuesta = await client.patch(
                url, json=body, headers={"X-Internal-Key": INTERNAL_API_KEY or ""}
            )
            respuesta.raise_for_status()
    except httpx.HTTPError as error:
        # Si esto falla, backend-api se queda esperando y el admin puede
        # reintentar el analisis manualmente desde el frontend.
        print(f"No se pudo enviar el resultado a backend-api: {error}")
