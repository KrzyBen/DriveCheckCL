import easyocr

from analizadores.base import AnalizadorBase

UMBRAL_CONFIANZA = 0.5

# El lector de EasyOCR se crea una sola vez (es lento de inicializar),
# no cada vez que se analiza un video.
_lector_ocr = None


def _obtener_lector_ocr():
    global _lector_ocr
    if _lector_ocr is None:
        _lector_ocr = easyocr.Reader(["en"], gpu=False)
    return _lector_ocr


class AnalizadorPatente(AnalizadorBase):
    """
    Capa de deteccion + lectura de patente.

    Dos pasos: (1) el modelo YOLO entrenado detecta la caja donde esta
    la patente, (2) se recorta esa zona de la imagen y se le pasa a
    EasyOCR para leer el texto. El modelo entrenado en Colab/Kaggle solo
    hace el paso 1; el paso 2 no se entrena, se usa tal cual.
    """

    nombre = "patente"

    def analizar_frames(self, frames) -> dict:
        mejor_caja_confianza = 0.0
        mejor_recorte = None

        for frame in frames:
            resultado = self.modelo.predict(frame, verbose=False)[0]
            for caja in resultado.boxes:
                confianza = float(caja.conf[0])
                if confianza < UMBRAL_CONFIANZA or confianza <= mejor_caja_confianza:
                    continue

                x1, y1, x2, y2 = [int(v) for v in caja.xyxy[0]]
                recorte = frame[y1:y2, x1:x2]
                if recorte.size == 0:
                    continue

                mejor_caja_confianza = confianza
                mejor_recorte = recorte

        if mejor_recorte is None:
            return {"resultado": "sin_deteccion", "confianza": 0.0, "texto": None}

        lector = _obtener_lector_ocr()
        lecturas = lector.readtext(mejor_recorte)

        if not lecturas:
            return {"resultado": "deteccion_sin_lectura", "confianza": round(mejor_caja_confianza, 4), "texto": None}

        # EasyOCR puede devolver varios fragmentos de texto (ej. si separa
        # letras y numeros); se concatenan en el orden en que los detecto.
        texto_completo = "".join(fragmento[1] for fragmento in lecturas).upper().replace(" ", "")
        confianza_ocr = sum(fragmento[2] for fragmento in lecturas) / len(lecturas)

        return {
            "resultado": "detectada",
            "texto": texto_completo,
            "confianza": round(min(mejor_caja_confianza, confianza_ocr), 4),
        }
