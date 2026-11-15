"""
Comprueba el modelo de detección de rostros con una imagen o un video, y dibuja el resultado.

    docker compose exec backend-ia python probar_rostros.py /storage/usuario_X/reporte_Y/video_1.mp4 /tmp/rostros.jpg
    docker compose exec backend-ia python probar_rostros.py foto.jpg /tmp/rostros.jpg --difuminar

Dibuja en verde el rostro detectado y en amarillo la caja ampliada que se difuminaría. Con --difuminar muestra
cómo quedaría. Es el mismo detector (anonimizador.DetectorRostros) que usará el anonimizador.
"""
import argparse
import os
import sys

import cv2

import anonimizador as an
from config import MODELOS_DIR

VIDEOS = (".mp4", ".mov", ".avi", ".mkv", ".webm")


def cargar_imagen(origen, segundo):
    if origen.lower().endswith(VIDEOS):
        captura = cv2.VideoCapture(origen)
        if not captura.isOpened():
            sys.exit(f"No se pudo abrir el video: {origen}")
        fps = captura.get(cv2.CAP_PROP_FPS) or 30.0
        captura.set(cv2.CAP_PROP_POS_FRAMES, int(segundo * fps))
        ok, frame = captura.read()
        captura.release()
        if not ok:
            sys.exit(f"No hay un frame en el segundo {segundo} de {origen}")
        return frame
    frame = cv2.imread(origen)
    if frame is None:
        sys.exit(f"No se pudo leer la imagen: {origen}")
    return frame


def main():
    ap = argparse.ArgumentParser(description="Prueba el detector de rostros (YuNet).")
    ap.add_argument("origen", help="imagen o video")
    ap.add_argument("salida", nargs="?", default="/tmp/rostros.jpg")
    ap.add_argument("--segundo", type=float, default=1.0, help="segundo del video a analizar (por defecto 1)")
    ap.add_argument("--umbral", type=float, default=an.UMBRAL_ROSTROS, help="confianza mínima (por defecto %(default)s)")
    ap.add_argument("--difuminar", action="store_true", help="aplica el difuminado del anonimizador")
    args = ap.parse_args()

    ruta = os.path.join(MODELOS_DIR, "rostros", "face_detection_yunet_2023mar.onnx")
    try:
        detector = an.DetectorRostros(ruta, umbral=args.umbral)
    except an.ModeloNoDisponible as e:
        sys.exit(f"{e}\nDescárgalo según modelos/rostros/LEEME.txt.")

    frame = cargar_imagen(args.origen, args.segundo)
    alto, ancho = frame.shape[:2]
    cajas = detector.detectar(frame)

    print(f"Modelo: {ruta}")
    print(f"Imagen: {ancho}x{alto} px · umbral {args.umbral} · rostros detectados: {len(cajas)}")
    for i, (x1, y1, x2, y2) in enumerate(cajas, 1):
        print(f"  {i}. caja ({x1}, {y1})-({x2}, {y2}) · {x2 - x1}x{y2 - y1} px")
    if not cajas:
        print("Ningún rostro detectado. Si hay rostros: ¿son muy grandes (>300 px) o muy chicos (<10 px)? Prueba --umbral 0.2")

    if args.difuminar:
        for caja in cajas:
            an.desenfocar(frame, an.expandir(caja, an.FACTOR_ROSTRO, ancho, alto))
    else:
        for caja in cajas:
            x1, y1, x2, y2 = an.expandir(caja, an.FACTOR_ROSTRO, ancho, alto)
            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 220, 255), 2)
            cv2.rectangle(frame, caja[:2], caja[2:], (0, 200, 0), 2)
    if not cv2.imwrite(args.salida, frame):
        sys.exit(f"No se pudo escribir {args.salida}")
    print(f"Resultado guardado en {args.salida}")


if __name__ == "__main__":
    main()
