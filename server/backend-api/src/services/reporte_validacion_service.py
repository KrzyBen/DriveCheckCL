import httpx

from entity.reporte_entity import Reporte, EstadoReporte
from entity.reporte_validacion_entity import ReporteValidacion, EstadoAnalisis
from config.configEnv import BACKEND_IA_URL, INTERNAL_API_KEY
from helpers.reporte_serializer import serializar_reporte_admin
from services.reporte_service import puede_transicionar


async def iniciar_analisis_service(db, admin, reporte_id: int):
    try:
        reporte = (
            db.query(Reporte)
            .filter(Reporte.id == reporte_id)
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
        if not validacion:
            validacion = ReporteValidacion(reporte_id=reporte_id)
            db.add(validacion)

        validacion.estado_analisis = EstadoAnalisis.analizando
        validacion.actualizado_por_id = admin.id

        reporte.estado = EstadoReporte.analizando

        db.commit()
        db.refresh(reporte)

        # Aviso a backend-ia. Si falla (por ejemplo, aún no está levantado),
        # el reporte queda igual en 'analizando' y el admin puede reintentar.
        video_paths = [v.video_path for v in reporte.videos]
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                await client.post(
                    f"{BACKEND_IA_URL}/analizar",
                    json={"reporte_id": reporte_id, "video_paths": video_paths},
                    headers={"X-Internal-Key": INTERNAL_API_KEY or ""},
                )
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
