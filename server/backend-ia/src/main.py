import asyncio
import os
import secrets
import tempfile
from typing import Optional

from fastapi import FastAPI, BackgroundTasks, Header, HTTPException, status
from fastapi.responses import FileResponse
from pydantic import BaseModel
from starlette.background import BackgroundTask

from config import INTERNAL_API_KEY, MODELOS_DIR
from orquestador import ejecutar_pipeline, CAPAS, _resolver_ruta_video
from anonimizador import anonimizar_clip, cargar_detectores, ModeloNoDisponible
from contrato import hay_capa_exitosa
from callback_client import enviar_resultado

app = FastAPI(title="DriveCheckCL - backend-ia")

# Reportes que se estan procesando ahora mismo. Evita correr dos pipelines en
# paralelo para el mismo reporte si /analizar se llama dos veces. Vive en
# memoria: vale mientras uvicorn corra con un solo proceso (como hoy).
_EN_PROCESO: set[int] = set()


class SolicitudAnalisis(BaseModel):
    reporte_id: int
    video_paths: list[str]


class SolicitudAnonimizacion(BaseModel):
    video_path: str
    patente_evento: Optional[str] = None


# Detectores del anonimizador (rostros, placas, OCR). Se cargan la primera vez que se
# usan y se reutilizan en las siguientes llamadas; si la carga falla no se guarda nada
# y se reintenta en la próxima solicitud.
_DETECTORES = None

# Solo se anonimiza un clip a la vez: el proceso es pesado en CPU y memoria.
_SEMAFORO_ANONIMIZAR = asyncio.Semaphore(1)


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


def _borrar_temporales(destino: str) -> None:
    # Borra el archivo de salida y el intermedio que anonimizar_clip crea junto a él
    for ruta in (destino, destino + ".tmp.mp4"):
        try:
            os.remove(ruta)
        except FileNotFoundError:
            pass
        except OSError as error:
            print(f"No se pudo borrar el archivo temporal {ruta}: {error}")


def _obtener_detectores():
    global _DETECTORES
    if _DETECTORES is None:
        _DETECTORES = cargar_detectores(MODELOS_DIR)
    return _DETECTORES


def _anonimizar_en_hilo(origen: str, destino: str, patente_evento: Optional[str]) -> dict:
    # Se ejecuta dentro de asyncio.to_thread: la carga de modelos y el procesamiento
    # del video son bloqueantes y no deben frenar el event loop
    rostros, placas, lector = _obtener_detectores()
    return anonimizar_clip(origen, destino, patente_evento, rostros, placas, lector)


@app.post("/anonimizar")
async def anonimizar(solicitud: SolicitudAnonimizacion, x_internal_key: str = Header(default=None)):
    verificar_clave_interna(x_internal_key)

    # La ruta debe quedar dentro de STORAGE_ROOT (se resuelven "..", rutas absolutas y enlaces)
    origen = _resolver_ruta_video(solicitud.video_path)
    if origen is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST,
                            detail="Ruta de video inválida: debe estar dentro del almacenamiento.")
    if not os.path.isfile(origen):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="El video no existe.")

    # /storage está montado en solo lectura, así que el resultado se escribe en /tmp
    descriptor, destino = tempfile.mkstemp(suffix=".mp4", prefix="anonimizado_", dir="/tmp")
    os.close(descriptor)

    try:
        async with _SEMAFORO_ANONIMIZAR:
            estadisticas = await asyncio.to_thread(
                _anonimizar_en_hilo, origen, destino, solicitud.patente_evento
            )
    except ModeloNoDisponible as error:
        _borrar_temporales(destino)
        print(f"Anonimización no disponible: {error}")
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail=f"Modelos de anonimización no disponibles: {error}")
    except Exception as error:
        _borrar_temporales(destino)
        print(f"Error anonimizando {solicitud.video_path}: {error}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                            detail=f"No se pudo anonimizar el video: {error}")

    print(f"Video anonimizado {solicitud.video_path}: {estadisticas}")
    # El temporal se borra cuando termina de enviarse la respuesta
    return FileResponse(destino, media_type="video/mp4", filename="anonimizado.mp4",
                        background=BackgroundTask(_borrar_temporales, destino))


async def _procesar_en_segundo_plano(reporte_id: int, video_paths: list[str]):
    try:
        resultados_ia = await asyncio.to_thread(ejecutar_pipeline, video_paths)
        if not hay_capa_exitosa(resultados_ia):
            # Un contrato lleno de errores NO es un analisis completado. Se envia igual
            # para que el panel pueda mostrar el codigo de error de cada capa.
            print(f"Reporte {reporte_id}: ninguna capa produjo resultado, se informa error.")
            await enviar_resultado(reporte_id, estado_analisis="error", resultados_ia=resultados_ia)
            return
        await enviar_resultado(reporte_id, estado_analisis="completado", resultados_ia=resultados_ia)
    except Exception as error:
        print(f"Error procesando el reporte {reporte_id}: {error}")
        await enviar_resultado(reporte_id, estado_analisis="error", resultados_ia=None)
    finally:
        _EN_PROCESO.discard(reporte_id)