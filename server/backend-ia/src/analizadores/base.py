import os
import json

import cv2
from ultralytics import YOLO

from config import MODELOS_DIR, N_FRAMES


class VideoIlegibleError(Exception):
    """El video existe pero no se pudo leer ningun frame."""


class AnalizadorBase:
    """
    Una capa de analisis (imprudencias, patente...). Cada capa vive en
    modelos/<nombre>/ con su modelo.onnx y su metadata.json, y devuelve por
    cada video un resultado con la forma del contrato (ver contrato.py):

        {"hallazgo": str, "confianza": float, "detalle": dict}

    El orquestador agrega el numero de video y elige entre los videos del reporte.
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

    def nombres_de_clases(self) -> dict:
        """Clases segun el propio modelo (model.names); si no las trae, las de metadata.json."""
        nombres = getattr(self.modelo, "names", None)
        if nombres:
            return {int(i): str(n) for i, n in dict(nombres).items()}
        return {i: str(n) for i, n in enumerate(self.metadata.get("clases", []))}

    def extraer_frames(self, video_path: str, cantidad: int = N_FRAMES):
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
        """Analiza un video. Lanza VideoIlegibleError si no se pudo leer ningun frame."""
        if not self.disponible:
            raise RuntimeError(f"La capa {self.nombre} no tiene modelo cargado")

        frames = self.extraer_frames(video_path)
        if not frames:
            raise VideoIlegibleError(video_path)

        return self.analizar_frames(frames)

    def analizar_frames(self, frames) -> dict:
        raise NotImplementedError
