import os
import sys
from collections import Counter

import cv2
import numpy as np

from analizadores.patente import AnalizadorPatente, UMBRAL_CONFIANZA, _obtener_lector_ocr
from config import N_FRAMES

try:
    from patente_formato import normalizar_patente
except ImportError:  # por si se corre con una version sin patente_formato.py
    def normalizar_patente(texto):
        limpio = "".join(c for c in (texto or "").upper() if c.isalnum())
        return {"texto": limpio or None, "formato_valido": False, "formato": None,
                "correcciones": 0, "texto_crudo": limpio}

# OJO: model.predict() descarta por si solo todo lo que tenga confianza < 0.25 (valor por defecto de
# ultralytics) si no se le pasa conf=. Por eso aqui se pasa conf explicito: para ver las detecciones debiles.
CONF_MINIMA_VER = 0.05
CONF_MINIMA_OCR = 0.10
MAX_OCR_POR_FRAME = 3
FRACCION_RECORTE = 0.6
ALLOWLIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"


def generar_recortes(ancho, alto, fraccion=FRACCION_RECORTE):
    """4 zonas (arriba/abajo, izquierda/derecha) de fraccion x fraccion del frame, con solapamiento."""
    tw, th = int(ancho * fraccion), int(alto * fraccion)
    return [(x, y, x + tw, y + th) for y in (0, alto - th) for x in (0, ancho - tw)]


def detectar(modelo, imagen, dx, dy, origen):
    res = modelo.predict(imagen, conf=CONF_MINIMA_VER, verbose=False)[0]
    out = []
    for c in res.boxes:
        x1, y1, x2, y2 = [int(v) for v in c.xyxy[0]]
        out.append({"conf": float(c.conf[0]), "origen": origen,
                    "caja": [x1 + dx, y1 + dy, x2 + dx, y2 + dy]})
    return out


def fusionar(dets, iou=0.4):
    """Quita duplicados (la misma patente vista en el frame completo y en un recorte)."""
    if not dets:
        return []
    cajas = [[d["caja"][0], d["caja"][1], d["caja"][2] - d["caja"][0], d["caja"][3] - d["caja"][1]] for d in dets]
    idx = cv2.dnn.NMSBoxes(cajas, [d["conf"] for d in dets], CONF_MINIMA_VER, iou)
    return [dets[i] for i in np.array(idx).flatten()]


def preprocesar(recorte):
    """Ampliar a ~200 px de alto, gris y contraste local. Barato y sin entrenar nada."""
    alto = recorte.shape[0]
    factor = max(1.0, 200.0 / max(alto, 1))
    grande = cv2.resize(recorte, None, fx=factor, fy=factor, interpolation=cv2.INTER_CUBIC)
    gris = cv2.cvtColor(grande, cv2.COLOR_BGR2GRAY)
    return cv2.createCLAHE(clipLimit=2.0, tileGridSize=(4, 4)).apply(gris)


def leer(lector, imagen, usar_allowlist):
    kwargs = {"allowlist": ALLOWLIST} if usar_allowlist else {}
    lecturas = lector.readtext(imagen, **kwargs)
    if not lecturas:
        return "", 0.0
    return "".join(l[1] for l in lecturas), sum(l[2] for l in lecturas) / len(lecturas)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    ruta = sys.argv[1]
    salida = sys.argv[2] if len(sys.argv) > 2 else "/tmp/debug_patente"
    n_frames = int(sys.argv[3]) if len(sys.argv) > 3 else N_FRAMES
    os.makedirs(salida, exist_ok=True)
    if not os.path.isfile(ruta):
        print(f"No existe el video: {ruta}")
        sys.exit(1)

    cap = cv2.VideoCapture(ruta)
    ancho, alto = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)), int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS) or 0
    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    cap.release()
    escala = 640 / max(ancho, alto, 1)
    print(f"Video: {ancho}x{alto}, {fps:.0f} fps, {total} frames")
    print(f"Frame completo -> 640 px: factor {escala:.2f}. Recorte de {int(FRACCION_RECORTE*100)} % -> 640 px: "
          f"factor {640 / (max(ancho, alto) * FRACCION_RECORTE):.2f} (patente mas grande para el modelo)")
    print(f"Umbral de produccion: {UMBRAL_CONFIANZA}. Aqui se listan cajas desde {CONF_MINIMA_VER}.\n")

    analizador = AnalizadorPatente()
    if not analizador.disponible:
        print("La capa de patente no tiene modelo cargado.")
        sys.exit(1)
    lector = _obtener_lector_ocr()
    frames = analizador.extraer_frames(ruta, cantidad=n_frames)
    print(f"Frames leidos: {len(frames)} de {n_frames} pedidos\n")

    mejor = {"completo": 0.0, "recorte": 0.0}
    frames_ok = {"completo": 0, "recorte": 0}
    anchos = []
    votos_actual, votos_mejorada = Counter(), Counter()
    validas_actual = validas_mejorada = recortes_ocr = 0

    for i, frame in enumerate(frames):
        h, w = frame.shape[:2]
        completo = detectar(analizador.modelo, frame, 0, 0, "completo")
        recortes = []
        for (x0, y0, x1, y1) in generar_recortes(w, h):
            recortes += detectar(analizador.modelo, frame[y0:y1, x0:x1], x0, y0, "recorte")
        for nombre, dets in (("completo", completo), ("recorte", recortes)):
            if dets:
                m = max(d["conf"] for d in dets)
                mejor[nombre] = max(mejor[nombre], m)
                frames_ok[nombre] += m >= UMBRAL_CONFIANZA
        cajas = sorted(fusionar(completo + recortes), key=lambda d: -d["conf"])

        vista = frame.copy()
        for j, d in enumerate(cajas):
            x1, y1, x2, y2 = d["caja"]
            pasa = d["conf"] >= UMBRAL_CONFIANZA
            cv2.rectangle(vista, (x1, y1), (x2, y2), (0, 200, 0) if pasa else (0, 165, 255), 3)
            linea = (f"  frame {i:02d} caja {j}: conf {d['conf']:.2f} [{'OK ' if pasa else 'baja'}] "
                     f"{x2 - x1}x{y2 - y1} px, vista en {d['origen']}")
            if pasa:
                anchos.append(x2 - x1)
            if d["conf"] >= CONF_MINIMA_OCR and j < MAX_OCR_POR_FRAME:
                crop = frame[max(y1, 0):y2, max(x1, 0):x2]
                if crop.size:
                    cv2.imwrite(os.path.join(salida, f"crop_{i:02d}_{j}.png"), crop)
                    t1, c1 = leer(lector, crop, False)
                    t2, c2 = leer(lector, preprocesar(crop), True)
                    n1, n2 = normalizar_patente(t1), normalizar_patente(t2)
                    recortes_ocr += 1
                    if n1["texto"]:
                        votos_actual[n1["texto"]] += 1
                    if n2["texto"]:
                        votos_mejorada[n2["texto"]] += 1
                    validas_actual += bool(n1["formato_valido"])
                    validas_mejorada += bool(n2["formato_valido"])
                    linea += (f"\n      OCR actual    : {t1!r:16} -> {n1['texto']!s:10} (valido={n1['formato_valido']}, conf {c1:.2f})"
                              f"\n      OCR mejorado  : {t2!r:16} -> {n2['texto']!s:10} (valido={n2['formato_valido']}, conf {c2:.2f})")
            print(linea)
        if cajas:
            cv2.imwrite(os.path.join(salida, f"frame_{i:02d}.jpg"),
                        cv2.resize(vista, None, fx=escala, fy=escala) if escala < 1 else vista)

    print("\n==================== RESUMEN ====================")
    print(f"Mejor confianza en todo el video:  frame completo {mejor['completo']:.2f}  |  recortes {mejor['recorte']:.2f}")
    print(f"Frames que pasan el umbral {UMBRAL_CONFIANZA}:    frame completo {frames_ok['completo']}  |  recortes {frames_ok['recorte']}  (de {len(frames)})")
    if anchos:
        anchos.sort()
        print(f"Ancho de las cajas que pasan el umbral: mediana {anchos[len(anchos) // 2]} px (minimo {anchos[0]}, maximo {anchos[-1]})")
    print(f"Recortes enviados al OCR: {recortes_ocr}")
    print(f"  lecturas con formato valido  actual: {validas_actual}  | mejorada: {validas_mejorada}")
    print(f"  lectura mas votada           actual: {votos_actual.most_common(2)}")
    print(f"                               mejorada: {votos_mejorada.most_common(2)}")

    print("\n--- Lectura del resultado ---")
    if mejor["recorte"] >= UMBRAL_CONFIANZA and frames_ok["completo"] == 0:
        print("EL TAMANO EN PANTALLA ES EL PROBLEMA: en el frame completo ninguna caja pasa el umbral, pero con "
              "recortes si. Se arregla sin reentrenar: detectar por zonas (recortes) en patente.py.")
    elif mejor["recorte"] > mejor["completo"] + 0.10:
        print(f"LOS RECORTES AYUDAN ({mejor['completo']:.2f} -> {mejor['recorte']:.2f}) pero no llegan al umbral. "
              "Combinar recortes con un umbral de deteccion mas bajo, y probar grabar a 1080p.")
    elif max(mejor.values()) < UMBRAL_CONFIANZA:
        print("NO MEJORA CON RECORTES: el modelo ve la patente con poca seguridad aun mas grande. Es mas probable un "
              "cambio de dominio (angulo, parabrisas, desenfoque): conviene afinar el modelo con frames de tu celular.")
    elif validas_actual == 0 and validas_mejorada == 0:
        print("FALLA LA LECTURA: la patente se detecta pero el texto no calza con ningun formato. "
              "Mira los crop_XX_Y.png: si a ojo no se leen, es resolucion o borrosidad del recorte.")
    elif validas_mejorada > validas_actual:
        print("EL PREPROCESADO AYUDA: ampliar + contraste + allowlist lee mejor que hoy.")
    else:
        print("La deteccion y la lectura funcionan en este video.")
    print(f"\nImagenes guardadas en: {salida}")


if __name__ == "__main__":
    main()