from entity.reporte_entity import Reporte


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
                "path": v.video_path
            }
            for v in reporte.videos
        ],
    }


def serializar_reporte_admin(reporte: Reporte) -> dict:
    base = serializar_reporte(reporte)
    validacion = getattr(reporte, "validacion", None)
    base.update({
        "usuario_id": reporte.usuario_id,
        "usuario_nombre": reporte.usuario.nombre_completo if reporte.usuario else None,
        "severidad_ia": reporte.severidad_ia,
        "confianza_ia": reporte.confianza_ia,
        "severidad_validada": reporte.severidad_validada,
        "notas_admin": reporte.notas_admin,
        "finalizado_at": str(reporte.finalizado_at) if reporte.finalizado_at else None,
        "infracciones": [
            {"id": i.id, "articulo": i.articulo, "descripcion": i.descripcion}
            for i in reporte.infracciones
        ],
        "validacion": {
            "estado_analisis": validacion.estado_analisis.value,
            "resultados_ia": validacion.resultados_ia,
            "patente_confirmada": validacion.patente_confirmada,
            "severidad_propuesta": validacion.severidad_propuesta,
            "notas_ia": validacion.notas_ia,
            "actualizado_en": str(validacion.actualizado_en) if validacion.actualizado_en else None,
        } if validacion else None,
    })
    return base