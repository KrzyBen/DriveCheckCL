from sqlalchemy.orm import Session, joinedload
from datetime import datetime, timezone, timedelta

from entity.reporte_entity import Reporte, VideoReporte, EstadoReporte, Infraccion
from entity.user_entity import User
from entity.reporte_validacion_entity import EstadoAnalisis
from helpers.storage_helper import guardar_video, guardar_metadatos, eliminar_carpeta_reporte
from helpers.metadatos_helper import parsear_metadatos
from helpers.reporte_serializer import serializar_reporte, serializar_reporte_admin
from helpers.pdf_generator_helper import generar_pdf_informe
from services.notificacion_service import crear_notificacion

MAX_REPORTES_POR_USUARIO = 10
DIAS_MINIMOS_ELIMINACION = 7

# Un análisis que lleva más que esto sin novedades se considera estancado
# (backend-ia caído o callback perdido) y se permite relanzarlo.
ANALISIS_VIGENTE = timedelta(minutes=10)

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
    metadatos: str | None = None,
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
        db.flush()

        # Ubicación y hora de cada clip (opcional). Nunca hace fallar la creación.
        datos_clips = parsear_metadatos(metadatos, len(archivos))

        for index, archivo in enumerate(archivos, start=1):
            datos = datos_clips[index - 1]
            ruta_relativa = guardar_video(usuario_id, nuevo_reporte.id, index, archivo)
            video = VideoReporte(
                reporte_id=nuevo_reporte.id,
                video_path=ruta_relativa,
                orden=index,
                lat=datos["lat"],
                lon=datos["lon"],
                grabado_en=datos["grabado_en"],
            )
            db.add(video)

            if datos["documento"] is not None:
                try:
                    guardar_metadatos(usuario_id, nuevo_reporte.id, index, datos["documento"])
                except OSError as error:
                    # El JSON es un respaldo para el dataset: si falla, el reporte se crea igual.
                    print(f"No se pudo guardar el JSON del video {index}: {error}")

        db.commit()
        db.refresh(nuevo_reporte)

        reporte_data = serializar_reporte(nuevo_reporte)
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

        data = [serializar_reporte(r) for r in reportes]
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

        return [serializar_reporte(reporte), None]

    except Exception as error:
        print(f"Error al obtener reporte: {error}")
        return [None, "Error interno del servidor"]


async def actualizar_estado_service(db: Session, usuario_id: int, reporte_id: int, nuevo_estado: str):
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

        return [serializar_reporte(reporte), None]

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
        query = (
            db.query(Reporte)
            .join(User, Reporte.usuario_id == User.id)
            .options(joinedload(Reporte.videos), joinedload(Reporte.infracciones), joinedload(Reporte.validacion))
        )
        if estado:
            if estado not in EstadoReporte.__members__:
                return [None, "Estado inválido"]
            query = query.filter(Reporte.estado == EstadoReporte[estado])
        if usuario_id:
            query = query.filter(Reporte.usuario_id == usuario_id)

        reportes = query.order_by(Reporte.created_at.desc()).all()
        return [[serializar_reporte_admin(r) for r in reportes], None]
    except Exception as error:
        print(f"Error al listar reportes (admin): {error}")
        return [None, "Error interno del servidor"]


def analisis_en_curso(validacion) -> bool:
    """True si hay un análisis corriendo y no se ha estancado todavía."""
    if validacion is None or validacion.estado_analisis != EstadoAnalisis.analizando:
        return False
    ultima_novedad = validacion.actualizado_en
    if ultima_novedad is None:
        return True
    if ultima_novedad.tzinfo is None:
        ultima_novedad = ultima_novedad.replace(tzinfo=timezone.utc)
    return datetime.now(timezone.utc) - ultima_novedad < ANALISIS_VIGENTE


def marcar_analisis_estancado(db, reporte) -> None:
    """
    Si el análisis lleva más de ANALISIS_VIGENTE sin novedades, lo pasa a 'error'.
    El panel ya ofrece "Reintentar" para ese estado y deja de consultar solo, así
    que un callback perdido ya no deja el reporte esperando para siempre.
    """
    validacion = reporte.validacion
    if (
        reporte.estado != EstadoReporte.analizando
        or validacion is None
        or validacion.estado_analisis != EstadoAnalisis.analizando
        or analisis_en_curso(validacion)
    ):
        return
    minutos = int(ANALISIS_VIGENTE.total_seconds() // 60)
    validacion.estado_analisis = EstadoAnalisis.error
    validacion.notas_ia = (
        f"El análisis no respondió en {minutos} minutos (backend-ia caído o resultado perdido)."
    )
    db.commit()
    db.refresh(reporte)


async def get_reporte_admin_service(db, reporte_id: int):
    try:
        reporte = (
            db.query(Reporte)
            .options(joinedload(Reporte.videos), joinedload(Reporte.infracciones), joinedload(Reporte.validacion))
            .filter(Reporte.id == reporte_id)
            .first()
        )
        if not reporte:
            return [None, "Reporte no encontrado"]
        if reporte.estado == EstadoReporte.enviado and puede_transicionar(reporte.estado, EstadoReporte.recibido):
            reporte.estado = EstadoReporte.recibido
            db.commit()
            db.refresh(reporte)

        marcar_analisis_estancado(db, reporte)

        return [serializar_reporte_admin(reporte), None]

    except Exception as error:
        print(f"Error al obtener reporte (admin): {error}")
        return [None, "Error interno del servidor"]


async def validar_reporte_service(
    db, admin, reporte_id: int, severidad_validada: str,
    infraccion_ids: list[int], infracciones_manuales: list[str],
    patente_confirmada: str | None, notas_admin: str | None,
):
    try:
        reporte = db.query(Reporte).filter(Reporte.id == reporte_id).first()
        if not reporte:
            return [None, "Reporte no encontrado"]

        if not puede_transicionar(reporte.estado, EstadoReporte.aprobado):
            return [None, f"No se puede aprobar un reporte en estado '{reporte.estado.value}'"]

        infracciones = []
        if infraccion_ids:
            infracciones = db.query(Infraccion).filter(Infraccion.id.in_(infraccion_ids)).all()

        reporte.severidad_validada = severidad_validada
        # Las infracciones manuales ya no se mezclan con el catálogo: van en el propio reporte.
        reporte.infracciones_manuales = infracciones_manuales or []
        reporte.patente_confirmada = patente_confirmada
        reporte.notas_admin = notas_admin
        reporte.infracciones = infracciones
        reporte.estado = EstadoReporte.aprobado
        reporte.finalizado_at = datetime.now(timezone.utc)
        reporte.validado_por_id = admin.id
        reporte.pdf_path = generar_pdf_informe(reporte)

        crear_notificacion(
            db, reporte.usuario_id,
            f"Tu reporte \"{reporte.titulo}\" fue aprobado.",
            reporte_id=reporte.id,
        )

        db.commit()
        db.refresh(reporte)
        return [serializar_reporte_admin(reporte), None]

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

        crear_notificacion(
            db, reporte.usuario_id,
            f"Tu reporte \"{reporte.titulo}\" fue rechazado.",
            reporte_id=reporte.id,
        )

        db.commit()
        db.refresh(reporte)
        return [serializar_reporte_admin(reporte), None]

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

async def listar_infracciones_catalogo_service(db):
    try:
        # Se excluyen las filas "Manual" que dejaba la versión anterior al validar.
        infracciones = (
            db.query(Infraccion)
            .filter(Infraccion.articulo != "Manual")
            .order_by(Infraccion.id)
            .all()
        )
        return [
            [{"id": i.id, "articulo": i.articulo, "descripcion": i.descripcion} for i in infracciones],
            None,
        ]
    except Exception as error:
        print(f"Error al listar catálogo de infracciones: {error}")
        return [None, "Error interno del servidor"]