from entity.reporte_entity import Reporte


def _videos_ordenados(reporte: Reporte) -> list:
    return sorted(reporte.videos, key=lambda v: v.orden)


def ubicacion_del_evento(reporte: Reporte):
    """Punto focal del evento: el inicio del primer clip que tenga GPS. None si ninguno lo tiene."""
    for video in _videos_ordenados(reporte):
        if video.lat is not None and video.lon is not None:
            return {"lat": video.lat, "lon": video.lon}
    return None


def grabado_en_del_evento(reporte: Reporte):
    """Hora de grabación del evento: la del primer clip que la tenga. None si ninguno la tiene."""
    for video in _videos_ordenados(reporte):
        if video.grabado_en is not None:
            return video.grabado_en
    return None


def serializar_reporte(reporte: Reporte) -> dict:
    return {
        "id": reporte.id,
        "titulo": reporte.titulo,
        "comentario": reporte.comentario,
        "estado": reporte.estado.value if hasattr(reporte.estado, "value") else reporte.estado,
        "pdf_disponible": reporte.pdf_path is not None,
        "pdf_path": reporte.pdf_path,   # <-- Agregar esta línea
        "notas_admin": reporte.notas_admin if reporte.estado.value == "rechazado" else None,
        "created_at": str(reporte.created_at),
        "updated_at": str(reporte.updated_at),
        "videos": [
            {
                "id": v.id,
                "orden": v.orden,
                "path": v.video_path,
                "lat": v.lat,
                "lon": v.lon,
                "grabado_en": v.grabado_en.isoformat() if v.grabado_en else None,
            }
            for v in reporte.videos
        ],
    }


def serializar_reporte_admin(reporte: Reporte) -> dict:
    base = serializar_reporte(reporte)
    validacion = getattr(reporte, "validacion", None)
    grabado_en = grabado_en_del_evento(reporte)
    base.update({
        "grabado_en": grabado_en.isoformat() if grabado_en else None,
        "ubicacion": ubicacion_del_evento(reporte),
        "usuario_id": reporte.usuario_id,
        "usuario_nombre": reporte.usuario.nombre_completo if reporte.usuario else None,
        "severidad_validada": reporte.severidad_validada,
        "patente_confirmada": reporte.patente_confirmada,
        "infracciones_manuales": reporte.infracciones_manuales or [],
        "notas_admin": reporte.notas_admin,
        "finalizado_at": str(reporte.finalizado_at) if reporte.finalizado_at else None,
        "infracciones": [
            {"id": i.id, "articulo": i.articulo, "descripcion": i.descripcion}
            for i in reporte.infracciones
        ],
        "validacion": {
            "estado_analisis": validacion.estado_analisis.value,
            "resultados_ia": validacion.resultados_ia,
            "notas_ia": validacion.notas_ia,
            "actualizado_en": str(validacion.actualizado_en) if validacion.actualizado_en else None,
        } if validacion else None,
    })
    return base