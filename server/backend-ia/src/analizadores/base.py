import os
import json

import cv2
from ultralytics import YOLO

from config import MODELOS_DIR, N_FRAMES


class AnalizadorBase:
    """
    Clase base para cada capa de IA (accidentes, imprudencias, patente).

    Cada capa nueva solo necesita heredar de esta clase e implementar
    `analizar_frames(self, frames)`. Todo lo comun (cargar el modelo,
    leer su metadata.json, extraer frames del video) ya esta resuelto
    aca, para que agregar una capa mas no implique repetir ese trabajo.

    Se usa la libreria ultralytics para correr el .onnx (no onnxruntime
    directo): ultralytics ya resuelve la decodificacion de cajas y el
    NMS de YOLO de forma correcta, y escribir eso a mano es una fuente
    comun de bugs dificiles de encontrar. El costo es una imagen de
    Docker mas pesada; para el tamano de este proyecto vale la pena
    priorizar que funcione bien por sobre ahorrar espacio.
    """

    nombre = "base"

    def __init__(self):
        carpeta = os.path.join(MODELOS_DIR, self.nombre)
        ruta_onnx = os.path.join(carpeta, "modelo.onnx")
        ruta_metadata = os.path.join(carpeta, "metadata.json")

        self.disponible = os.path.isfile(ruta_onnx)
        self.metadata = {}
        self.modelo = None

        if not self.disponible:
            print(f"[{self.nombre}] No se encontro modelo.onnx en {carpeta}, esta capa se omite por ahora.")
            return

        if os.path.isfile(ruta_metadata):
            with open(ruta_metadata) as f:
                self.metadata = json.load(f)

        self.modelo = YOLO(ruta_onnx)
        print(f"[{self.nombre}] Modelo cargado desde {ruta_onnx}")

    def extraer_frames(self, video_path: str, cantidad: int = N_FRAMES):
        """Muestrea 'cantidad' frames repartidos a lo largo del video."""
        captura = cv2.VideoCapture(video_path)
        total_frames = int(captura.get(cv2.CAP_PROP_FRAME_COUNT))

        if total_frames <= 0:
            captura.release()
            return []

        indices = [int(i * total_frames / cantidad) for i in range(cantidad)]
        frames = []
        for indice in indices:
            captura.set(cv2.CAP_PROP_POS_FRAMES, indice)
            ok, frame = captura.read()
            if ok:
                frames.append(frame)

        captura.release()
        return frames

    def analizar(self, video_path: str) -> dict:
        """Punto de entrada usado por el orquestador."""
        if not self.disponible:
            return {"disponible": False}

        frames = self.extraer_frames(video_path)
        if not frames:
            return {"disponible": True, "error": "No se pudieron leer frames del video"}

        return self.analizar_frames(frames)

    def analizar_frames(self, frames) -> dict:
        """Cada capa concreta implementa esto: recibe una lista de frames
        (arrays de OpenCV, formato BGR) y devuelve el resultado de su capa."""
        raise NotImplementedError
