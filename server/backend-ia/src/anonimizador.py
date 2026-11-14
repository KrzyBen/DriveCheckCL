import logging
import os
import time

import cv2

from patente_formato import normalizar_patente

logger = logging.getLogger(__name__)

# Umbrales BAJOS a propósito: para difuminar importa el recall, no la precisión.
UMBRAL_ROSTROS = 0.3
UMBRAL_PLACAS = 0.05

FACTOR_ROSTRO = 0.30   # la caja de un rostro se amplía ~30 %
FACTOR_PLACA = 0.20
PASO_DETECCION = 2     # se detecta cada N frames
PERSISTENCIA = 4       # una caja se mantiene N frames más allá del frame donde se vio
MAX_LECTURAS_POR_TRACK = 4
LARGO_MIN_COMPARACION = 5   # no se compara una placa con menos caracteres que esto
TOLERANCIA = 1              # caracteres de diferencia aceptados al comparar con la del evento


class ModeloNoDisponible(Exception):
    """Falta un modelo necesario: no se debe exportar sin anonimizar."""


# ── geometría ──────────────────────────────────────────────────────────────

def expandir(caja, factor, ancho, alto):
    x1, y1, x2, y2 = caja
    dx, dy = (x2 - x1) * factor / 2, (y2 - y1) * factor / 2
    return (max(0, int(x1 - dx)), max(0, int(y1 - dy)),
            min(ancho, int(x2 + dx + 0.999)), min(alto, int(y2 + dy + 0.999)))


def iou(a, b):
    ix1, iy1, ix2, iy2 = max(a[0], b[0]), max(a[1], b[1]), min(a[2], b[2]), min(a[3], b[3])
    inter = max(0, ix2 - ix1) * max(0, iy2 - iy1)
    union = (a[2] - a[0]) * (a[3] - a[1]) + (b[2] - b[0]) * (b[3] - b[1]) - inter
    return inter / union if union > 0 else 0.0


def desenfocar(frame, caja):
    """Pixelado fuerte + desenfoque: más difícil de revertir que un desenfoque solo."""
    x1, y1, x2, y2 = caja
    zona = frame[y1:y2, x1:x2]
    if zona.size == 0:
        return
    alto, ancho = zona.shape[:2]
    bloque = max(2, max(ancho, alto) // 6)
    pequeno = cv2.resize(zona, (max(1, ancho // bloque), max(1, alto // bloque)), interpolation=cv2.INTER_AREA)
    pixelada = cv2.resize(pequeno, (ancho, alto), interpolation=cv2.INTER_NEAREST)
    k = max(3, (min(ancho, alto) // 3) | 1)
    frame[y1:y2, x1:x2] = cv2.GaussianBlur(pixelada, (k, k), 0)


# ── patentes ───────────────────────────────────────────────────────────────

def distancia(a: str, b: str) -> int:
    """Distancia de Levenshtein."""
    previa = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        actual = [i]
        for j, cb in enumerate(b, 1):
            actual.append(min(previa[j] + 1, actual[j - 1] + 1, previa[j - 1] + (ca != cb)))
        previa = actual
    return previa[-1]


def coincide_con_evento(lectura, patente_evento) -> bool:
    """True si la lectura es la placa del evento (tolerando TOLERANCIA caracteres)."""
    if not lectura or not patente_evento:
        return False
    a = normalizar_patente(lectura)["texto"]
    b = normalizar_patente(patente_evento)["texto"]
    if not a or not b or len(a) < LARGO_MIN_COMPARACION or len(b) < LARGO_MIN_COMPARACION:
        return False
    return distancia(a, b) <= TOLERANCIA


class Track:
    """Una misma placa seguida a lo largo de varios frames (por superposición de cajas)."""

    def __init__(self):
        self.detecciones = []   # (frame, caja)
        self.lecturas = 0
        self.es_evento = False

    @property
    def ultimo(self):
        return self.detecciones[-1]


def _asociar(tracks, caja, frame, ya_usados, max_hueco, iou_min=0.3):
    mejor, mejor_iou = None, iou_min
    for t in tracks:
        if id(t) in ya_usados or frame - t.ultimo[0] > max_hueco:
            continue
        v = iou(t.ultimo[1], caja)
        if v >= mejor_iou:
            mejor, mejor_iou = t, v
    if mejor is None:
        mejor = Track()
        tracks.append(mejor)
    ya_usados.add(id(mejor))
    mejor.detecciones.append((frame, caja))
    return mejor


# ── rotación ───────────────────────────────────────────────────────────────

# Rotación del metadato (grados en sentido horario) -> giro que endereza el frame
_GIROS = {
    90: cv2.ROTATE_90_CLOCKWISE,
    180: cv2.ROTATE_180,
    270: cv2.ROTATE_90_COUNTERCLOCKWISE,
}


def _abrir_video(origen):
    """
    Abre el video con la rotación automática desactivada y devuelve (captura, rotación).
    Los clips de Android vienen como 1280x720 con metadato de rotación 90°: OpenCV no
    siempre endereza esos frames, así que se hace a mano con _enderezar para que el
    resultado no dependa de la versión ni del backend de OpenCV.
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
    if rotacion not in (0, *_GIROS):
        logger.warning("Rotación %s no soportada en %s; se trata como 0", rotacion, origen)
        rotacion = 0
    return captura, rotacion


def _enderezar(frame, rotacion):
    """Gira el frame según el metadato de rotación. Con rotación 0 lo devuelve sin tocar."""
    if rotacion == 0:
        return frame
    return cv2.rotate(frame, _GIROS[rotacion])


# ── núcleo ─────────────────────────────────────────────────────────────────

def anonimizar_clip(origen, destino, patente_evento, rostros, placas, lector,
                    paso=PASO_DETECCION, persistencia=PERSISTENCIA) -> dict:
    """
    rostros.detectar(frame) -> [(x1, y1, x2, y2)]
    placas.detectar(frame)  -> [(x1, y1, x2, y2)]
    lector(frame, caja)     -> texto leído o None
    Devuelve estadísticas para revisar el resultado a ojo.
    Los frames se enderezan según el metadato de rotación antes de detectar y de
    escribir, así que el video de salida queda derecho y sin metadato de rotación.
    """
    inicio = time.time()
    captura, rotacion = _abrir_video(origen)
    fps = captura.get(cv2.CAP_PROP_FPS) or 30.0
    # El tamaño se toma del primer frame YA enderezado (no de CAP_PROP_FRAME_WIDTH/HEIGHT,
    # que describen el frame sin girar)
    ancho = alto = None

    # Pasada 1: detectar y leer placas por tramo
    tracks, caras = [], []   # caras: (frame, caja)
    total, indice = 0, 0
    try:
        while True:
            ok, frame = captura.read()
            if not ok:
                break
            frame = _enderezar(frame, rotacion)
            if total == 0:
                alto, ancho = frame.shape[:2]
            total += 1
            if indice % paso == 0:
                for caja in rostros.detectar(frame):
                    caras.append((indice, caja))
                usados = set()
                for caja in placas.detectar(frame):
                    t = _asociar(tracks, caja, indice, usados, max_hueco=paso * 4)
                    if t.lecturas < MAX_LECTURAS_POR_TRACK and not t.es_evento:
                        t.lecturas += 1
                        if coincide_con_evento(lector(frame, caja), patente_evento):
                            t.es_evento = True
            indice += 1
    finally:
        captura.release()
    if total == 0:
        raise IOError(f"El video no tiene frames legibles: {origen}")

    # Cajas a difuminar por frame (cada detección cubre +-(paso + persistencia) frames)
    radio = paso + persistencia
    por_frame = {}

    def cubrir(frame_detectado, caja, factor):
        c = expandir(caja, factor, ancho, alto)
        for f in range(max(0, frame_detectado - radio), min(total, frame_detectado + radio + 1)):
            por_frame.setdefault(f, []).append(c)

    for f, caja in caras:
        cubrir(f, caja, FACTOR_ROSTRO)
    difuminadas = 0
    for t in tracks:
        if t.es_evento:
            continue
        difuminadas += 1
        for f, caja in t.detecciones:
            cubrir(f, caja, FACTOR_PLACA)

    # Pasada 2: reescribir el video (sin audio) con las cajas difuminadas, con los frames
    # ya derechos y del tamaño del primer frame enderezado
    os.makedirs(os.path.dirname(os.path.abspath(destino)), exist_ok=True)
    temporal = destino + ".tmp.mp4"
    salida = cv2.VideoWriter(temporal, cv2.VideoWriter_fourcc(*"mp4v"), fps, (ancho, alto))
    captura = None
    completo = False
    try:
        if not salida.isOpened():
            raise IOError("No se pudo crear el video de salida (codec mp4v no disponible)")
        captura, rotacion = _abrir_video(origen)
        escritos = 0
        for indice in range(total):
            ok, frame = captura.read()
            if not ok:
                break
            frame = _enderezar(frame, rotacion)
            # VideoWriter descarta en silencio los frames de otro tamaño: mejor fallar
            if frame.shape[:2] != (alto, ancho):
                raise IOError(
                    f"El frame {indice} mide {frame.shape[1]}x{frame.shape[0]} y el video "
                    f"{ancho}x{alto}: no se guarda el video"
                )
            for caja in por_frame.get(indice, ()):
                desenfocar(frame, caja)
            salida.write(frame)
            escritos += 1
        if escritos != total:
            raise IOError(f"Se leyeron {escritos} de {total} frames: no se guarda el video")
        completo = True
    finally:
        # Se liberan lector y escritor siempre, y ante cualquier error se borra el
        # temporal para no dejar un video a medio escribir
        if captura is not None:
            captura.release()
        salida.release()
        if not completo and os.path.exists(temporal):
            os.remove(temporal)
    os.replace(temporal, destino)

    return {
        "frames": total,
        "rostros_detectados": len(caras),
        "tracks_placa": len(tracks),
        "tracks_evento_visibles": sum(t.es_evento for t in tracks),
        "tracks_placa_difuminados": difuminadas,
        "evento_sin_coincidencia": bool(patente_evento) and not any(t.es_evento for t in tracks),
        "segundos": round(time.time() - inicio, 2),
    }


# ── detectores reales (se cargan solo al usarlos) ──────────────────────────

class DetectorRostros:
    """OpenCV YuNet (cv2.FaceDetectorYN). Archivo: modelos/rostros/face_detection_yunet_2023mar.onnx"""

    def __init__(self, ruta_modelo, umbral=UMBRAL_ROSTROS):
        if not os.path.isfile(ruta_modelo) or os.path.getsize(ruta_modelo) < 10_000:
            raise ModeloNoDisponible(f"Falta el modelo de rostros (o es un puntero de LFS): {ruta_modelo}")
        try:
            self._det = cv2.FaceDetectorYN.create(ruta_modelo, "", (320, 320), umbral, 0.3, 5000)
        except cv2.error as e:
            raise ModeloNoDisponible(f"No se pudo cargar el modelo de rostros (¿archivo dañado o incompleto?): {ruta_modelo}") from e

    def detectar(self, frame):
        alto, ancho = frame.shape[:2]
        self._det.setInputSize((ancho, alto))
        _, caras = self._det.detect(frame)
        if caras is None:
            return []
        return [(int(x), int(y), int(x + w), int(y + h)) for x, y, w, h in caras[:, :4]]


class DetectorPlacas:
    """El mismo modelo YOLO de la capa de patente, pero con umbral bajo."""

    def __init__(self, ruta_modelo, umbral=UMBRAL_PLACAS):
        if not os.path.isfile(ruta_modelo):
            raise ModeloNoDisponible(f"Falta el modelo de placas: {ruta_modelo}")
        from ultralytics import YOLO
        self._modelo = YOLO(ruta_modelo)
        self._umbral = umbral

    def detectar(self, frame):
        resultado = self._modelo.predict(frame, conf=self._umbral, verbose=False)[0]
        return [tuple(int(v) for v in caja.xyxy[0]) for caja in resultado.boxes]


def lector_ocr(frame, caja):
    """Lee el texto de una placa con el mismo EasyOCR de la capa de patente."""
    from analizadores.patente import _obtener_lector_ocr
    x1, y1, x2, y2 = caja
    recorte = frame[max(0, y1):y2, max(0, x1):x2]
    if recorte.size == 0:
        return None
    lecturas = _obtener_lector_ocr().readtext(recorte)
    return "".join(l[1] for l in lecturas) if lecturas else None


def cargar_detectores(modelos_dir):
    return (
        DetectorRostros(os.path.join(modelos_dir, "rostros", "face_detection_yunet_2023mar.onnx")),
        DetectorPlacas(os.path.join(modelos_dir, "patente", "modelo.onnx")),
        lector_ocr,
    )


if __name__ == "__main__":
    import argparse
    import json
    from config import MODELOS_DIR

    ap = argparse.ArgumentParser(description="Anonimiza un clip (rostros y placas de terceros).")
    ap.add_argument("origen")
    ap.add_argument("destino")
    ap.add_argument("--patente", default=None, help="patente del evento (queda visible); sin ella se difuminan todas")
    args = ap.parse_args()
    print(json.dumps(anonimizar_clip(args.origen, args.destino, args.patente, *cargar_detectores(MODELOS_DIR)),
                     indent=2, ensure_ascii=False))
