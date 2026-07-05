from sqlalchemy.orm import Session, joinedload

from entity.reporte_entity import Reporte, VideoReporte, EstadoReporte, Infraccion
from helpers.storage_helper import guardar_video, eliminar_carpeta_reporte
from datetime import datetime, timezone, timedelta
from helpers.reporte_serializer import serializar_reporte, serializar_reporte_admin

MAX_REPORTES_POR_USUARIO = 10

DIAS_MINIMOS_ELIMINACION = 60

TRANSICIONES_VALIDAS = {
    EstadoReporte.enviado:    {EstadoReporte.recibido, EstadoReporte.aprobado, EstadoReporte.rechazado},
    EstadoReporte.recibido:   {EstadoReporte.analizando, EstadoReporte.aprobado, EstadoReporte.rechazado},
    EstadoReporte.analizando: {EstadoReporte.aprobado, EstadoReporte.rechazado},
    EstadoReporte.aprobado:   set(),
    EstadoReporte.rechazado:  set(),
}


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

        nuevo = EstadoReporte[nuevo_estado]

        if nuevo in (EstadoReporte.aprobado, EstadoReporte.rechazado):
            return [None, "Usa los endpoints de validar/rechazar para este cambio de estado"]

        if not puede_transicionar(reporte.estado, nuevo):
            return [None, f"No se puede pasar de '{reporte.estado.value}' a '{nuevo.value}'"]

        reporte.estado = nuevo
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


def puede_transicionar(actual: EstadoReporte, nuevo: EstadoReporte) -> bool:
    return nuevo in TRANSICIONES_VALIDAS.get(actual, set())


async def listar_reportes_admin_service(db, estado: str = None, usuario_id: int = None):
    try:
        query = db.query(Reporte).options(joinedload(Reporte.videos), joinedload(Reporte.infracciones))

        if estado:
            if estado not in EstadoReporte.__members__:
                return [None, "Estado inválido"]
            query = query.filter(Reporte.estado == EstadoReporte[estado])
        if usuario_id:
            query = query.filter(Reporte.usuario_id == usuario_id)

        reportes = query.order_by(Reporte.created_at.desc()).all()
        return [[_serializar_reporte_admin(r) for r in reportes], None]

    except Exception as error:
        print(f"Error al listar reportes (admin): {error}")
        return [None, "Error interno del servidor"]


async def get_reporte_admin_service(db, reporte_id: int):
    try:
        reporte = (
            db.query(Reporte)
            .options(joinedload(Reporte.videos), joinedload(Reporte.infracciones))
            .filter(Reporte.id == reporte_id)
            .first()
        )
        if not reporte:
            return [None, "Reporte no encontrado"]
        return [_serializar_reporte_admin(reporte), None]

    except Exception as error:
        print(f"Error al obtener reporte (admin): {error}")
        return [None, "Error interno del servidor"]


async def validar_reporte_service(
    db, admin, reporte_id: int, severidad_validada: str,
    infraccion_ids: list[int], infracciones_manuales: list[str], notas_admin: str | None,
):
    try:
        reporte = db.query(Reporte).filter(Reporte.id == reporte_id).first()
        if not reporte:
            return [None, "Reporte no encontrado"]

        if not puede_transicionar(reporte.estado, EstadoReporte.aprobado):
            return [None, f"No se puede aprobar un reporte en estado '{reporte.estado.value}'"]

        infracciones = []
        if infraccion_ids:
            infracciones += db.query(Infraccion).filter(Infraccion.id.in_(infraccion_ids)).all()
        for texto in (infracciones_manuales or []):
            manual = Infraccion(articulo="Manual", descripcion=texto)
            db.add(manual)
            db.flush()  # para tener el id antes del commit
            infracciones.append(manual)

        reporte.severidad_validada = severidad_validada
        reporte.notas_admin = notas_admin
        reporte.infracciones = infracciones
        reporte.estado = EstadoReporte.aprobado
        reporte.finalizado_at = datetime.now(timezone.utc)
        reporte.validado_por_id = admin.id
        reporte.pdf_path = _generar_pdf_placeholder(reporte)

        db.commit()
        db.refresh(reporte)
        return [_serializar_reporte_admin(reporte), None]

    except Exception as error:
        print(f"Error al validar reporte: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def rechazar_reporte_service(db, admin, reporte_id: int, notas_admin: str):
    try:
        reporte = db.query(Reporte).filter(Reporte.id == reporte_id).first()
        if not reporte:
            return [None, "Reporte no encontrado"]

        if not puede_transicionar(reporte.estado, EstadoReporte.rechazado):
            return [None, f"No se puede rechazar un reporte en estado '{reporte.estado.value}'"]

        reporte.notas_admin = notas_admin
        reporte.estado = EstadoReporte.rechazado
        reporte.finalizado_at = datetime.now(timezone.utc)
        reporte.validado_por_id = admin.id

        db.commit()
        db.refresh(reporte)
        return [_serializar_reporte_admin(reporte), None]

    except Exception as error:
        print(f"Error al rechazar reporte: {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


async def eliminar_reporte_admin_service(db, reporte_id: int):
    try:
        reporte = db.query(Reporte).filter(Reporte.id == reporte_id).first()
        if not reporte:
            return [None, "Reporte no encontrado"]

        if reporte.estado == EstadoReporte.aprobado:
            if not reporte.finalizado_at:
                return [None, "El reporte no tiene fecha de finalización registrada"]
            transcurrido = datetime.now(timezone.utc) - reporte.finalizado_at
            if transcurrido < timedelta(days=DIAS_MINIMOS_ELIMINACION):
                faltan = DIAS_MINIMOS_ELIMINACION - transcurrido.days
                return [None, f"Este reporte solo puede eliminarse {DIAS_MINIMOS_ELIMINACION} días después de ser aprobado (faltan {faltan} días)"]

        eliminar_carpeta_reporte(reporte.usuario_id, reporte.id)
        db.delete(reporte)
        db.commit()
        return [{"id": reporte_id}, None]

    except Exception as error:
        print(f"Error al eliminar reporte (admin): {error}")
        db.rollback()
        return [None, "Error interno del servidor"]


def _generar_pdf_placeholder(reporte) -> str:
    # TODO: reemplazar por generación real una vez elijas la librería (reportlab / weasyprint)
    return f"pendiente_generar/{reporte.id}.pdf"


def _serializar_reporte_admin(r: Reporte) -> dict:
    base = _serializar_reporte(r)  # reutiliza el helper que ya tienes en este archivo
    base.update({
        "usuario_id": r.usuario_id,
        "severidad_ia": r.severidad_ia,
        "confianza_ia": r.confianza_ia,
        "severidad_validada": r.severidad_validada,
        "notas_admin": r.notas_admin,
        "finalizado_at": str(r.finalizado_at) if r.finalizado_at else None,
        "infracciones": [{"id": i.id, "articulo": i.articulo, "descripcion": i.descripcion} for i in r.infracciones],
    })
    return base