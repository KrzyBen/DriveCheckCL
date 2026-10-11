import httpx
from sqlalchemy import func

from entity.reporte_entity import Reporte, EstadoReporte
from entity.reporte_validacion_entity import ReporteValidacion, EstadoAnalisis
from config.configEnv import BACKEND_IA_URL, INTERNAL_API_KEY
from helpers.reporte_serializer import serializar_reporte_admin
from services.reporte_service import puede_transicionar, analisis_en_curso


async def iniciar_analisis_service(db, admin, reporte_id: int):
    try:
        reporte = (
            db.query(Reporte)
            .filter(Reporte.id == reporte_id)
            # Bloquea la fila hasta el commit: dos clics seguidos en "Analizar"
            # se atienden de a uno y el segundo ve el estado ya actualizado.
            .with_for_update()
            .first()
        )
        if not reporte:
            return [None, "Reporte no encontrado"]

        ya_analizando = reporte.estado == EstadoReporte.analizando
        if not ya_analizando and not puede_transicionar(reporte.estado, EstadoReporte.analizando):
            return [None, f"No se puede analizar un reporte en estado '{reporte.estado.value}'"]

        if not reporte.videos:
            return [None, "El reporte no tiene videos para analizar"]

        validacion = (
            db.query(ReporteValidacion)
            .filter(ReporteValidacion.reporte_id == reporte_id)
            .first()
        )
        if analisis_en_curso(validacion):
            return [None, "El análisis de este reporte ya está en curso"]
        if not validacion:
            validacion = ReporteValidacion(reporte_id=reporte_id)
            db.add(validacion)

        validacion.estado_analisis = EstadoAnalisis.analizando
        validacion.actualizado_por_id = admin.id
        validacion.resultados_ia = None
        validacion.notas_ia = None
        validacion.actualizado_en = func.now()

        reporte.estado = EstadoReporte.analizando

        db.commit()
        db.refresh(reporte)

        # Aviso a backend-ia. Si falla (por ejemplo, aún no está levantado),
        # el reporte queda igual en 'analizando' y el admin puede reintentar.
        video_paths = [v.video_path for v in reporte.videos]
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                respuesta = await client.post(
                    f"{BACKEND_IA_URL}/analizar",
                    json={"reporte_id": reporte_id, "video_paths": video_paths},
                    headers={"X-Internal-Key": INTERNAL_API_KEY or ""},
                )
                # Sin esto, un 401/422/500 de backend-ia pasaba como éxito y el
                # reporte quedaba en 'analizando' para siempre.
                respuesta.raise_for_status()
        except httpx.HTTPStatusError as error:
            print(f"backend-ia rechazó la solicitud de análisis: {error}")
            validacion.estado_analisis = EstadoAnalisis.error
            validacion.notas_ia = (
                f"El servicio de análisis (backend-ia) rechazó la solicitud "
                f"(HTTP {error.response.status_code})."
            )
            db.commit()
        except httpx.HTTPError as error:
            print(f"No se pudo contactar a backend-ia: {error}")
            validacion.estado_analisis = EstadoAnalisis.error
            validacion.notas_ia = "No se pudo contactar al servicio de análisis (backend-ia)."
            db.commit()

        db.refresh(reporte)
        return [serializar_reporte_admin(reporte), None]

    except Exception as error:
        print(f"Error al iniciar análisis: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def guardar_resultado_validacion_service(
    db, reporte_id: int, estado_analisis: str,
    resultados_ia: dict | None, patente_confirmada: str | None,
    severidad_propuesta: str | None, notas_ia: str | None,
):
    try:
        validacion = (
            db.query(ReporteValidacion)
            .filter(ReporteValidacion.reporte_id == reporte_id)
            .first()
        )
        if not validacion:
            return [None, "No existe un análisis iniciado para este reporte"]

        reporte = db.query(Reporte).filter(Reporte.id == reporte_id).first()
        if reporte and reporte.estado in (EstadoReporte.aprobado, EstadoReporte.rechazado):
            return [None, f"El reporte ya fue {reporte.estado.value}; se descarta el resultado del análisis"]

        validacion.estado_analisis = EstadoAnalisis[estado_analisis]
        if resultados_ia is not None:
            validacion.resultados_ia = resultados_ia
        if patente_confirmada is not None:
            validacion.patente_confirmada = patente_confirmada
        if severidad_propuesta is not None:
            validacion.severidad_propuesta = severidad_propuesta
        if notas_ia is not None:
            validacion.notas_ia = notas_ia

        db.commit()
        db.refresh(validacion)
        return [{"reporte_id": reporte_id, "estado_analisis": validacion.estado_analisis.value}, None]

    except Exception as error:
        print(f"Error al guardar resultado de validación: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]