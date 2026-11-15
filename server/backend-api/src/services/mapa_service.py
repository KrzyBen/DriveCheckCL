from sqlalchemy.orm import joinedload

from entity.reporte_entity import Reporte, EstadoReporte
from helpers.reporte_serializer import ubicacion_del_evento, grabado_en_del_evento
from helpers.mapa_helper import agregar

PREFIJO_PRUEBA = "[PRUEBA]"  # el mismo que usa seed_eventos_prueba.py


async def obtener_mapa_service(db, desde=None, hasta=None, hora_desde: int = 0, hora_hasta: int = 24,
                               dias=None, severidades=None, infraccion_id=None):
    """Eventos = reportes aprobados. Punto = inicio del primer clip con GPS; hora = grabado_en
    (si falta, la fecha de envío). La agregación se hace en Python: el volumen de la tesis es chico."""
    try:
        reportes = (
            db.query(Reporte)
            .options(joinedload(Reporte.videos), joinedload(Reporte.infracciones))
            .filter(Reporte.estado == EstadoReporte.aprobado)
            .all()
        )
        eventos = []
        for r in reportes:
            ubicacion = ubicacion_del_evento(r)
            catalogo = [i for i in r.infracciones if i.articulo != "Manual"]
            # las filas "Manual" son del catálogo antiguo: se muestran como texto libre
            textos = [f"{i.articulo} - {i.descripcion}" for i in catalogo]
            textos += [i.descripcion for i in r.infracciones if i.articulo == "Manual"]
            textos += list(r.infracciones_manuales or [])
            eventos.append({
                "id": r.id,
                "momento": grabado_en_del_evento(r) or r.created_at,
                "lat": ubicacion["lat"] if ubicacion else None,
                "lon": ubicacion["lon"] if ubicacion else None,
                "severidad": r.severidad_validada,
                "infraccion_ids": [i.id for i in catalogo],
                "infracciones": textos,
                "titulo": r.titulo,
                "prueba": (r.titulo or "").startswith(PREFIJO_PRUEBA),
            })
        return [agregar(eventos, desde, hasta, hora_desde, hora_hasta, dias, severidades, infraccion_id), None]
    except Exception as error:
        print(f"Error al obtener los datos del mapa: {error}")
        return [None, "Error interno del servidor"]
