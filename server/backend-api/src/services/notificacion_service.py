from sqlalchemy.orm import Session

from entity.notificacion_entity import Notificacion


def crear_notificacion(db: Session, usuario_id: int, mensaje: str, reporte_id: int | None = None) -> None:
    """
    Encola una notificación sin hacer commit: se llama desde dentro de otro
    flujo (ej. al aprobar/rechazar un reporte) que ya maneja su propio
    commit/rollback, así que este helper solo hace db.add().
    """
    db.add(Notificacion(usuario_id=usuario_id, reporte_id=reporte_id, mensaje=mensaje))


async def listar_notificaciones_service(db: Session, usuario_id: int):
    try:
        notificaciones = (
            db.query(Notificacion)
            .filter(Notificacion.usuario_id == usuario_id)
            .order_by(Notificacion.created_at.desc())
            .limit(50)
            .all()
        )

        data = [
            {
                "id": n.id,
                "reporte_id": n.reporte_id,
                "mensaje": n.mensaje,
                "leida": n.leida,
                "created_at": str(n.created_at),
            }
            for n in notificaciones
        ]

        no_leidas = sum(1 for n in notificaciones if not n.leida)

        return [{"no_leidas": no_leidas, "notificaciones": data}, None]
    except Exception as error:
        print(f"Error al listar notificaciones: {error}")
        return [None, "Error interno del servidor"]


async def marcar_notificaciones_leidas_service(db: Session, usuario_id: int):
    try:
        db.query(Notificacion).filter(
            Notificacion.usuario_id == usuario_id,
            Notificacion.leida == False,
        ).update({"leida": True})
        db.commit()
        return [True, None]
    except Exception as error:
        db.rollback()
        print(f"Error al marcar notificaciones como leídas: {error}")
        return [None, "Error interno del servidor"]
