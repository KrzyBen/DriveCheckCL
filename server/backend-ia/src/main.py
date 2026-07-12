import asyncio

from fastapi import FastAPI, BackgroundTasks, Header, HTTPException, status
from pydantic import BaseModel

from config import INTERNAL_API_KEY
from orquestador import ejecutar_pipeline, CAPAS
from callback_client import enviar_resultado

app = FastAPI(title="DriveCheckCL - backend-ia")


class SolicitudAnalisis(BaseModel):
    reporte_id: int
    video_paths: list[str]


def verificar_clave_interna(x_internal_key: str = Header(default=None)):
    if not INTERNAL_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Canal interno no configurado (falta INTERNAL_API_KEY).",
        )
    if x_internal_key != INTERNAL_API_KEY:
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
    background_tasks.add_task(_procesar_en_segundo_plano, solicitud.reporte_id, solicitud.video_paths)
    return {"status": "recibido", "reporte_id": solicitud.reporte_id}


async def _procesar_en_segundo_plano(reporte_id: int, video_paths: list[str]):
    try:
        resultados_ia = await asyncio.to_thread(ejecutar_pipeline, video_paths)
        if not resultados_ia:
            await enviar_resultado(reporte_id, estado_analisis="error", resultados_ia=None)
            return
        await enviar_resultado(reporte_id, estado_analisis="completado", resultados_ia=resultados_ia)
    except Exception as error:
        print(f"Error procesando el reporte {reporte_id}: {error}")
        await enviar_resultado(reporte_id, estado_analisis="error", resultados_ia=None)
