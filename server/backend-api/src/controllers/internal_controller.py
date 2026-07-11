from fastapi import Depends
from sqlalchemy.orm import Session

from config.configDb import get_db
from validations.reporte_validacion_validation import ReporteValidacionCallback
from services.reporte_validacion_service import guardar_resultado_validacion_service
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server


async def recibir_resultado_validacion(
    reporte_id: int,
    body: ReporteValidacionCallback,
    db: Session = Depends(get_db),
):
    try:
        data, error = await guardar_resultado_validacion_service(
            db, reporte_id,
            estado_analisis=body.estado_analisis,
            resultados_ia=body.resultados_ia,
            patente_confirmada=body.patente_confirmada,
            severidad_propuesta=body.severidad_propuesta,
            notas_ia=body.notas_ia,
        )
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Resultado de validación guardado", data)
    except Exception as error:
        return handle_error_server(500, str(error))
