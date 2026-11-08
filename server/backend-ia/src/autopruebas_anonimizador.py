import hashlib
import os
import sys
import tempfile

import cv2
import numpy as np

import anonimizador as an

ANCHO, ALTO, N = 480, 360, 40
ROSTRO = (30, 30, 90, 100)
PLACA_A = (150, 250, 230, 280)    # la del evento
PLACA_B = (330, 60, 410, 90)      # otro auto, legible
PLACA_C = (40, 270, 120, 300)     # otro auto, ilegible
FALLOS = []


def comprobar(condicion, nombre):
    print(f"  [{'OK' if condicion else 'FALLA'}] {nombre}")
    if not condicion:
        FALLOS.append(nombre)


def crear_video(ruta):
    rng = np.random.default_rng(1)
    fondo = rng.integers(0, 256, (ALTO, ANCHO, 3), dtype=np.uint8)   # textura: mucha varianza
    w = cv2.VideoWriter(ruta, cv2.VideoWriter_fourcc(*"mp4v"), 20, (ANCHO, ALTO))
    for _ in range(N):
        w.write(fondo)
    w.release()


def desviacion(frame, caja):
    x1, y1, x2, y2 = caja
    return float(frame[y1:y2, x1:x2].std())


class Rostros:
    def __init__(self, parpadeo=True):
        self.n, self.parpadeo = 0, parpadeo

    def detectar(self, frame):
        self.n += 1
        return [] if (self.parpadeo and self.n % 5 == 3) else [ROSTRO]   # falla 1 de cada 5


class Placas:
    def __init__(self):
        self.n = 0

    def detectar(self, frame):
        self.n += 1
        d = self.n * 2   # B se mueve: el seguimiento debe mantener su track
        return [PLACA_A, (PLACA_B[0] + d, PLACA_B[1], PLACA_B[2] + d, PLACA_B[3]), PLACA_C]


def caja_b(i):
    """Posición real de la placa B en el frame i, algo reducida (la detección es del frame par más cercano)."""
    d = (i // 2 + 1) * 2
    return (PLACA_B[0] + d + 6, PLACA_B[1] + 4, PLACA_B[2] + d - 6, PLACA_B[3] - 4)


def lector_con(texto_a, texto_b="GHJK34"):
    def lector(frame, caja):
        if caja[1] > 200 and caja[0] > 100:
            return texto_a      # placa A
        if caja[1] < 200:
            return texto_b      # placa B
        return None             # placa C: ilegible
    return lector


def leer(ruta):
    c = cv2.VideoCapture(ruta)
    frames = []
    while True:
        ok, f = c.read()
        if not ok:
            break
        frames.append(f)
    c.release()
    return frames


def correr(directorio, origen, evento, lector, parpadeo=True):
    destino = os.path.join(directorio, "salida.mp4")
    stats = an.anonimizar_clip(origen, destino, evento, Rostros(parpadeo), Placas(), lector)
    return stats, leer(destino), destino


def main():
    with tempfile.TemporaryDirectory() as d:
        origen = os.path.join(d, "origen.mp4")
        crear_video(origen)
        original = leer(origen)
        sha = hashlib.sha256(open(origen, "rb").read()).hexdigest()
        base = {nombre: np.mean([desviacion(f, c) for f in original])
                for nombre, c in [("rostro", ROSTRO), ("A", PLACA_A), ("B", PLACA_B), ("C", PLACA_C)]}

        print("== 1. Placa del evento visible; las demás y los rostros, difuminados ==")
        stats, salida, destino = correr(d, origen, "BCDF12", lector_con("BC-DF12"))
        comprobar(len(salida) == N and salida[0].shape[:2] == (ALTO, ANCHO), "mismo número de frames y tamaño")
        comprobar(all(desviacion(f, ROSTRO) < 0.25 * base["rostro"] for f in salida),
                  "rostro difuminado en TODOS los frames (aunque el detector falle 1 de cada 5)")
        comprobar(all(desviacion(f, PLACA_A) > 0.5 * base["A"] for f in salida), "placa del evento visible en todos los frames")
        comprobar(all(desviacion(f, caja_b(i)) < 0.25 * base["B"] for i, f in enumerate(salida)),
                  "placa legible de otro auto difuminada en todos los frames (aunque se mueva)")
        comprobar(all(desviacion(f, PLACA_C) < 0.25 * base["C"] for f in salida), "placa ilegible difuminada")
        comprobar(stats["tracks_placa"] == 3 and stats["tracks_evento_visibles"] == 1
                  and stats["tracks_placa_difuminados"] == 2 and not stats["evento_sin_coincidencia"],
                  f"estadísticas coherentes {stats}")
        comprobar(hashlib.sha256(open(origen, "rb").read()).hexdigest() == sha, "el original no se modifica")
        comprobar(not os.path.exists(destino + ".tmp.mp4"), "no queda archivo temporal")

        print("== 2. Sin patente del evento: se difuminan todas ==")
        stats, salida, _ = correr(d, origen, None, lector_con("BC-DF12"))
        comprobar(all(desviacion(f, PLACA_A) < 0.25 * base["A"] for f in salida), "también la placa A")
        comprobar(stats["tracks_evento_visibles"] == 0 and not stats["evento_sin_coincidencia"], "sin evento no hay 'sin coincidencia'")

        print("== 3. Tolerancia de 1 carácter ==")
        _, salida, _ = correr(d, origen, "BCDF12", lector_con("BCDF13"))
        comprobar(all(desviacion(f, PLACA_A) > 0.5 * base["A"] for f in salida), "BCDF13 vs BCDF12: queda visible")
        _, salida, _ = correr(d, origen, "BCDF12", lector_con("BCDX99"))
        comprobar(all(desviacion(f, PLACA_A) < 0.25 * base["A"] for f in salida), "BCDX99 vs BCDF12: se difumina")

        print("== 4. La IA no encuentra la placa del evento: se difumina todo ==")
        stats, salida, _ = correr(d, origen, "BCDF12", lector_con(None, None))
        comprobar(stats["evento_sin_coincidencia"] and all(desviacion(f, PLACA_A) < 0.25 * base["A"] for f in salida),
                  "evento_sin_coincidencia=True y la placa A también se difumina")

        print("== 5. Funciones sueltas ==")
        comprobar(an.distancia("BCDF12", "BCDF13") == 1 and an.distancia("BCDF12", "BCDF12") == 0
                  and an.distancia("BCDF12", "BCDF1") == 1, "distancia de Levenshtein")
        comprobar(not an.coincide_con_evento("AB", "AC") and not an.coincide_con_evento(None, "BCDF12")
                  and not an.coincide_con_evento("BCDF12", None), "lecturas cortas o vacías nunca coinciden")
        comprobar(an.expandir((10, 10, 20, 20), 0.3, 100, 100) == (8, 8, 22, 22)
                  and an.expandir((0, 0, 10, 10), 0.5, 100, 100)[:2] == (0, 0), "expandir y recortar al borde")
        comprobar(an.iou((0, 0, 10, 10), (0, 0, 10, 10)) == 1.0 and an.iou((0, 0, 10, 10), (20, 20, 30, 30)) == 0.0, "iou")

        print("== 6. Sin modelo no se produce nada ==")
        for ruta, contenido in [("/no/existe.onnx", None), (os.path.join(d, "puntero.onnx"), b"version https://git-lfs")]:
            if contenido:
                open(ruta, "wb").write(contenido)
            try:
                an.DetectorRostros(ruta)
                comprobar(False, f"debía fallar con {ruta}")
            except an.ModeloNoDisponible:
                comprobar(True, f"ModeloNoDisponible con {os.path.basename(ruta)}")

    print("\nTODO OK" if not FALLOS else f"\nFALLARON {len(FALLOS)}: {FALLOS}")
    sys.exit(1 if FALLOS else 0)


if __name__ == "__main__":
    main()
