from entity.reporte_entity import Reporte


def serializar_reporte(reporte: Reporte) -> dict:
    return {
        "id": reporte.id,
        "titulo": reporte.titulo,
        "comentario": reporte.comentario,
        "estado": reporte.estado.value if hasattr(reporte.estado, "value") else reporte.estado,
        "pdf_disponible": reporte.pdf_path is not None,
        "created_at": str(reporte.created_at),
        "updated_at": str(reporte.updated_at),
        "videos": [
            {"id": v.id, "orden": v.orden, "path": v.video_path}
            for v in reporte.videos
        ],
    }


def serializar_reporte_admin(reporte: Reporte) -> dict:
    base = serializar_reporte(reporte)
    base.update({
        "usuario_id": reporte.usuario_id,
        "severidad_ia": reporte.severidad_ia,
        "confianza_ia": reporte.confianza_ia,
        "severidad_validada": reporte.severidad_validada,
        "notas_admin": reporte.notas_admin,
        "finalizado_at": str(reporte.finalizado_at) if reporte.finalizado_at else None,
        "infracciones": [
            {"id": i.id, "articulo": i.articulo, "descripcion": i.descripcion}
            for i in reporte.infracciones
        ],
    })
    return base