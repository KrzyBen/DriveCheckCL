"""
Utilidades de lectura de video compartidas por el anonimizador y los analizadores.

Los clips de Android vienen con un metadato de rotación (por ejemplo 1280x720 con
rotación 90°) y OpenCV no siempre endereza esos frames. Aquí se abre el video con la
rotación automática desactivada y se endereza a mano, para que todo backend-ia vea
los frames derechos sin depender de la versión ni del backend de OpenCV.
"""

import logging

import cv2

logger = logging.getLogger(__name__)

# Rotación del metadato (grados en sentido horario) -> giro que endereza el frame
GIROS = {
    90: cv2.ROTATE_90_CLOCKWISE,
    180: cv2.ROTATE_180,
    270: cv2.ROTATE_90_COUNTERCLOCKWISE,
}


def abrir_video(origen):
    """
    Abre el video con la rotación automática desactivada y devuelve (captura, rotación).
    Los clips de Android vienen como 1280x720 con metadato de rotación 90°: OpenCV no
    siempre endereza esos frames, así que se hace a mano con enderezar para que el
    resultado no dependa de la versión ni del backend de OpenCV.
    Lanza IOError si el video no se puede abrir.
    """
    captura = cv2.VideoCapture(origen)
    if not captura.isOpened():
        captura.release()
        raise IOError(f"No se pudo abrir el video: {origen}")

    # Si el backend no acepta desactivarla, se sigue igual pero queda registrado:
    # en ese caso podría enderezarse dos veces y conviene saberlo al revisar el resultado
    if not captura.set(cv2.CAP_PROP_ORIENTATION_AUTO, 0):
        logger.warning("No se pudo desactivar la rotación automática al abrir %s", origen)

    # El metadato puede venir como 90.0 o -90; se normaliza a 0, 90, 180 o 270
    rotacion = int(round(captura.get(cv2.CAP_PROP_ORIENTATION_META))) % 360
    if rotacion not in (0, *GIROS):
        logger.warning("Rotación %s no soportada en %s; se trata como 0", rotacion, origen)
        rotacion = 0
    return captura, rotacion


def enderezar(frame, rotacion):
    """Gira el frame según el metadato de rotación. Con rotación 0 lo devuelve sin tocar."""
    if rotacion == 0:
        return frame
    return cv2.rotate(frame, GIROS[rotacion])
