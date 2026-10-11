import asyncio
import secrets

from fastapi import FastAPI, BackgroundTasks, Header, HTTPException, status
from pydantic import BaseModel

from config import INTERNAL_API_KEY
from orquestador import ejecutar_pipeline, CAPAS
from callback_client import enviar_resultado

app = FastAPI(title="DriveCheckCL - backend-ia")

# Reportes que se estan procesando ahora mismo. Evita correr dos pipelines en
# paralelo para el mismo reporte si /analizar se llama dos veces. Vive en
# memoria: vale mientras uvicorn corra con un solo proceso (como hoy).
_EN_PROCESO: set[int] = set()


class SolicitudAnalisis(BaseModel):
    reporte_id: int
    video_paths: list[str]


def verificar_clave_interna(x_internal_key: str = Header(default=None)):
    if not INTERNAL_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Canal interno no configurado (falta INTERNAL_API_KEY).",
        )
    # Comparación en tiempo constante (y en bytes, para tolerar headers no ASCII).
    if not secrets.compare_digest(
        (x_internal_key or "").encode("utf-8"), INTERNAL_API_KEY.encode("utf-8")
    ):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Clave interna invalida.")
    return True


@app.get("/health")
def health():
    return {
        "status": "ok",
        "capas_disponibles": [c.nombre for c in CAPAS if c.disponible],
        "capas_pendientes": [c.nombre for c in CAPAS if not c.disponible],
    }


@app.post("/analizar")
async def analizar(solicitud: SolicitudAnalisis, background_tasks: BackgroundTasks,
                    x_internal_key: str = Header(default=None)):
    verificar_clave_interna(x_internal_key)
    if solicitud.reporte_id in _EN_PROCESO:
        # 200 y no 409: backend-api trata cualquier respuesta no exitosa como
        # error de analisis, y aqui el primer analisis sigue en marcha.
        return {"status": "ya_en_proceso", "reporte_id": solicitud.reporte_id}
    _EN_PROCESO.add(solicitud.reporte_id)
    background_tasks.add_task(_procesar_en_segundo_plano, solicitud.reporte_id, solicitud.video_paths)
    return {"status": "recibido", "reporte_id": solicitud.reporte_id}


def _hay_capa_exitosa(resultados_ia: dict) -> bool:
    """
    True si al menos una capa devolvio un resultado real. El orquestador deja
    {"error": ...} en las capas que fallan, asi que un diccionario lleno de
    errores NO es un analisis completado.
    """
    return any("error" not in resultado for resultado in resultados_ia.values())


async def _procesar_en_segundo_plano(reporte_id: int, video_paths: list[str]):
    try:
        resultados_ia = await asyncio.to_thread(ejecutar_pipeline, video_paths)
        if not _hay_capa_exitosa(resultados_ia):
            print(f"Reporte {reporte_id}: ninguna capa produjo resultado, se informa error.")
            await enviar_resultado(reporte_id, estado_analisis="error", resultados_ia=None)
            return
        await enviar_resultado(reporte_id, estado_analisis="completado", resultados_ia=resultados_ia)
    except Exception as error:
        print(f"Error procesando el reporte {reporte_id}: {error}")
        await enviar_resultado(reporte_id, estado_analisis="error", resultados_ia=None)
    finally:
        _EN_PROCESO.discard(reporte_id)