from datetime import date
from typing import Optional

from fastapi import Depends, Query
from sqlalchemy.orm import Session

from config.configDb import get_db
from entity.user_entity import User
from middlewares.authorization_middleware import is_admin
from services.mapa_service import obtener_mapa_service
from handlers.response_handlers import handle_success, handle_error_client, handle_error_server

SEVERIDADES = {"leve", "moderada", "grave"}


async def obtener_mapa(
    desde: Optional[date] = Query(None, description="Fecha inicial inclusiva (YYYY-MM-DD), hora de Chile"),
    hasta: Optional[date] = Query(None, description="Fecha final inclusiva (YYYY-MM-DD), hora de Chile"),
    hora_desde: int = Query(0, ge=0, le=23, description="Hora inicial inclusiva"),
    hora_hasta: int = Query(24, ge=1, le=24, description="Hora final exclusiva; si es menor que hora_desde cruza la medianoche"),
    dias: Optional[str] = Query(None, description="Días ISO separados por coma: 1=lunes ... 7=domingo"),
    severidad: Optional[str] = Query(None, description="leve, moderada y/o grave, separadas por coma"),
    infraccion_id: Optional[int] = Query(None, ge=1, description="Id de la infracción del catálogo"),
    current_admin: User = Depends(is_admin),
    db: Session = Depends(get_db),
):
    try:
        if desde and hasta and desde > hasta:
            return handle_error_client(400, "La fecha 'desde' no puede ser posterior a 'hasta'")
        dias_set = None
        if dias:
            try:
                dias_set = {int(d) for d in dias.split(",") if d.strip()} or None
            except ValueError:
                return handle_error_client(400, "'dias' debe ser una lista de números del 1 al 7, por ejemplo 1,2,3")
            if dias_set and not dias_set <= {1, 2, 3, 4, 5, 6, 7}:
                return handle_error_client(400, "'dias' solo admite números del 1 (lunes) al 7 (domingo)")
        sev_set = None
        if severidad:
            sev_set = {s.strip().lower() for s in severidad.split(",") if s.strip()} or None
            if sev_set and not sev_set <= SEVERIDADES:
                return handle_error_client(400, "'severidad' solo admite: leve, moderada, grave")
        data, error = await obtener_mapa_service(db, desde, hasta, hora_desde, hora_hasta, dias_set, sev_set, infraccion_id)
        if error:
            return handle_error_client(400, error)
        return handle_success(200, "Datos del mapa de riesgo", data)
    except Exception as error:
        return handle_error_server(500, str(error))
