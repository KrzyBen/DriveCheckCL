from sqlalchemy.orm import Session, joinedload

from entity.reporte_entity import Reporte, VideoReporte, EstadoReporte
from helpers.storage_helper import guardar_video, eliminar_carpeta_reporte

MAX_REPORTES_POR_USUARIO = 10


async def contar_reportes_service(db: Session, usuario_id: int):
    try:
        total = db.query(Reporte).filter(Reporte.usuario_id == usuario_id).count()
        return [{"total": total, "limite": MAX_REPORTES_POR_USUARIO}, None]
    except Exception as error:
        print(f"Error al contar reportes: {error}")
        return [None, "Error interno del servidor"]


async def crear_reporte_service(
    db: Session,
    usuario_id: int,
    titulo: str,
    comentario: str,
    archivos: list,
):
    try:
        total_actual = db.query(Reporte).filter(Reporte.usuario_id == usuario_id).count()
        if total_actual >= MAX_REPORTES_POR_USUARIO:
            return [None, f"Has alcanzado el límite de {MAX_REPORTES_POR_USUARIO} reportes. Elimina uno para continuar."]

        if not archivos or len(archivos) == 0:
            return [None, "Debes adjuntar al menos un video"]

        if len(archivos) > 3:
            return [None, "Máximo 3 videos por reporte"]

        nuevo_reporte = Reporte(
            usuario_id=usuario_id,
            titulo=titulo,
            comentario=comentario,
            estado=EstadoReporte.enviado,
        )
        db.add(nuevo_reporte)
        db.flush()  # para obtener el id antes del commit

        for index, archivo in enumerate(archivos, start=1):
            ruta_relativa = guardar_video(usuario_id, nuevo_reporte.id, index, archivo)
            video = VideoReporte(
                reporte_id=nuevo_reporte.id,
                video_path=ruta_relativa,
                orden=index,
            )
            db.add(video)

        db.commit()
        db.refresh(nuevo_reporte)

        reporte_data = _serializar_reporte(nuevo_reporte)
        return [reporte_data, None]

    except Exception as error:
        print(f"Error al crear reporte: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def listar_reportes_service(db: Session, usuario_id: int):
    try:
        reportes = (
            db.query(Reporte)
            .options(joinedload(Reporte.videos))
            .filter(Reporte.usuario_id == usuario_id)
            .order_by(Reporte.created_at.desc())
            .all()
        )

        if not reportes:
            return [[], None]

        data = [_serializar_reporte(r) for r in reportes]
        return [data, None]

    except Exception as error:
        print(f"Error al listar reportes: {error}")
        return [None, "Error interno del servidor"]


async def get_reporte_service(db: Session, usuario_id: int, reporte_id: int):
    try:
        reporte = (
            db.query(Reporte)
            .options(joinedload(Reporte.videos))
            .filter(Reporte.id == reporte_id, Reporte.usuario_id == usuario_id)
            .first()
        )

        if not reporte:
            return [None, "Reporte no encontrado"]

        return [_serializar_reporte(reporte), None]

    except Exception as error:
        print(f"Error al obtener reporte: {error}")
        return [None, "Error interno del servidor"]


async def actualizar_estado_service(db: Session, usuario_id: int, reporte_id: int, nuevo_estado: str, pdf_path: str = None):
    try:
        reporte = (
            db.query(Reporte)
            .filter(Reporte.id == reporte_id, Reporte.usuario_id == usuario_id)
            .first()
        )
        if not reporte:
            return [None, "Reporte no encontrado"]

        if nuevo_estado not in EstadoReporte.__members__:
            return [None, "Estado inválido"]

        reporte.estado = EstadoReporte[nuevo_estado]
        if pdf_path:
            reporte.pdf_path = pdf_path

        db.commit()
        db.refresh(reporte)

        return [_serializar_reporte(reporte), None]

    except Exception as error:
        print(f"Error al actualizar estado: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def eliminar_reporte_service(db: Session, usuario_id: int, reporte_id: int):
    try:
        reporte = (
            db.query(Reporte)
            .filter(Reporte.id == reporte_id, Reporte.usuario_id == usuario_id)
            .first()
        )
        if not reporte:
            return [None, "Reporte no encontrado"]

        eliminar_carpeta_reporte(usuario_id, reporte_id)

        db.delete(reporte)
        db.commit()

        return [{"id": reporte_id}, None]

    except Exception as error:
        print(f"Error al eliminar reporte: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


# ── Helper de serialización ───────────────────────────────────────────────────

def _serializar_reporte(reporte: Reporte) -> dict:
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