import ast
import hashlib
import importlib
import json
import os
import sys
import time
from datetime import datetime

RESULTADOS = []  # (estado, titulo)


def registrar(estado, titulo, detalle=""):
    RESULTADOS.append((estado, titulo))
    print(f"  [{estado:5s}] {titulo}" + (f" -> {detalle}" if detalle else ""))


def version_de(modulo):
    try:
        return getattr(importlib.import_module(modulo), "__version__", "?")
    except Exception as error:
        return f"NO DISPONIBLE ({type(error).__name__}: {error})"


def entorno():
    print("\n== 1. Entorno ==")
    print(f"  python {sys.version.split()[0]}")
    for nombre in ("ultralytics", "onnxruntime", "onnx", "numpy", "cv2", "easyocr", "torch"):
        print(f"  {nombre}: {version_de(nombre)}")
    if version_de("ultralytics").startswith("NO DISPONIBLE"):
        registrar("FALLA", "ultralytics no se puede importar")
    if version_de("onnxruntime").startswith("NO DISPONIBLE"):
        registrar("FALLA", "onnxruntime no se puede importar")


def inspeccionar_modelo(nombre):
    """Lee el .onnx con onnxruntime (igual que hace ultralytics) y lo revisa."""
    from config import MODELOS_DIR

    print(f"\n== 2. Archivo del modelo: {nombre} ==")
    carpeta = os.path.join(MODELOS_DIR, nombre)
    ruta = os.path.join(carpeta, "modelo.onnx")
    if not os.path.isfile(ruta):
        registrar("FALLA", f"{nombre}: no existe {ruta}")
        return None

    sha = hashlib.sha256()
    with open(ruta, "rb") as f:
        for bloque in iter(lambda: f.read(1024 * 1024), b""):
            sha.update(bloque)
    info = os.stat(ruta)
    print(f"  ruta     : {ruta}")
    print(f"  tamano   : {info.st_size / 1e6:.1f} MB | modificado: {datetime.fromtimestamp(info.st_mtime):%Y-%m-%d %H:%M}")
    print(f"  sha256   : {sha.hexdigest()}")

    try:
        import numpy as np
        import onnxruntime as ort

        sesion = ort.InferenceSession(ruta, providers=["CPUExecutionProvider"])
    except Exception as error:
        registrar("FALLA", f"{nombre}: onnxruntime no pudo abrir el modelo", f"{type(error).__name__}: {error}")
        return None

    meta = sesion.get_modelmeta().custom_metadata_map
    for clave in ("description", "date", "version", "task", "imgsz", "stride", "end2end"):
        if clave in meta:
            print(f"  {clave:9s}: {meta[clave]}")

    # IA-14: estos datos sirven para saber de que exportacion es el archivo
    registrar("OK", f"{nombre}: abre con onnxruntime", f"exportado con ultralytics {meta.get('version', '?')} el {meta.get('date', '?')[:19]}")

    if meta.get("end2end", "False") == "True":
        registrar("FALLA", f"{nombre}: modelo end2end (NMS embebido)", "ultralytics 8.3.0 no lo interpreta; hay que subir la version")

    # IA-8: orden de clases embebido vs metadata.json
    clases_json = None
    ruta_json = os.path.join(carpeta, "metadata.json")
    if os.path.isfile(ruta_json):
        with open(ruta_json, encoding="utf-8") as f:
            clases_json = json.load(f).get("clases")
    try:
        nombres = ast.literal_eval(meta["names"])
        embebidas = [nombres[i] for i in sorted(nombres)]
        if clases_json is None:
            registrar("AVISO", f"{nombre}: sin metadata.json para comparar clases")
        elif embebidas == clases_json:
            registrar("OK", f"{nombre}: {len(embebidas)} clases, mismo orden que metadata.json")
        else:
            registrar("FALLA", f"{nombre}: las clases embebidas NO coinciden con metadata.json", f"{embebidas} vs {clases_json}")
    except Exception as error:
        registrar("AVISO", f"{nombre}: no se pudieron leer las clases embebidas", str(error))
        embebidas = None

    # Inferencia de prueba directa (sin ultralytics)
    try:
        entrada = sesion.get_inputs()[0]
        tam = meta.get("imgsz", "[640, 640]")
        alto, ancho = ast.literal_eval(tam)
        salida = sesion.run(None, {entrada.name: np.random.rand(1, 3, alto, ancho).astype(np.float32)})[0]
        esperado = 4 + (len(embebidas) if embebidas else 0)
        if embebidas and salida.shape[1] != esperado:
            registrar("AVISO", f"{nombre}: forma de salida inesperada", f"{salida.shape}, se esperaba (1, {esperado}, N)")
        else:
            registrar("OK", f"{nombre}: inferencia de prueba con onnxruntime", f"salida {tuple(salida.shape)}")
    except Exception as error:
        registrar("FALLA", f"{nombre}: la inferencia de prueba fallo", f"{type(error).__name__}: {error}")
    return embebidas


def probar_capa(nombre, clase, embebidas):
    """Carga la capa con TU clase (la misma que usa el orquestador) y la prueba."""
    import numpy as np

    print(f"\n== 3. Capa cargada por ultralytics: {nombre} ==")
    try:
        inicio = time.time()
        capa = clase()
        carga = time.time() - inicio
    except Exception as error:
        registrar("FALLA", f"{nombre}: la capa no se pudo cargar", f"{type(error).__name__}: {error}")
        return None

    if not capa.disponible or capa.modelo is None:
        registrar("FALLA", f"{nombre}: la capa quedo no disponible")
        return None
    registrar("OK", f"{nombre}: YOLO() cargo el modelo", f"{carga:.1f} s")

    try:
        nombres_yolo = [capa.modelo.names[i] for i in sorted(capa.modelo.names)]
        if embebidas is not None and nombres_yolo != embebidas:
            registrar("FALLA", f"{nombre}: model.names no coincide con las clases del .onnx", f"{nombres_yolo} vs {embebidas}")
        else:
            registrar("OK", f"{nombre}: model.names coincide ({len(nombres_yolo)} clases)")
    except Exception as error:
        registrar("AVISO", f"{nombre}: no se pudo leer model.names", str(error))

    try:
        imagen = np.zeros((480, 854, 3), dtype=np.uint8)
        inicio = time.time()
        resultado = capa.modelo.predict(imagen, verbose=False)[0]
        registrar("OK", f"{nombre}: predict() sobre una imagen vacia", f"{len(resultado.boxes)} cajas, {time.time() - inicio:.2f} s")
    except Exception as error:
        registrar("FALLA", f"{nombre}: predict() fallo", f"{type(error).__name__}: {error}")
    return capa


def revisar_easyocr():
    print("\n== 4. EasyOCR (lectura de patente) ==")
    carpeta = os.path.expanduser("~/.EasyOCR/model")
    pesos = [f for f in os.listdir(carpeta) if f.endswith(".pth")] if os.path.isdir(carpeta) else []
    if pesos:
        registrar("OK", "EasyOCR tiene pesos locales", ", ".join(sorted(pesos)))
    else:
        registrar(
            "AVISO",
            "EasyOCR no tiene pesos descargados",
            "la primera lectura de patente los baja de internet; sin salida a internet fallaria",
        )


def analizar_video(capas, ruta_video):
    print(f"\n== 5. Analisis del video: {ruta_video} ==")
    if not os.path.isfile(ruta_video):
        registrar("FALLA", f"el video no existe: {ruta_video}")
        return
    for nombre, capa in capas.items():
        if capa is None:
            continue
        try:
            frames = capa.extraer_frames(ruta_video)
            if not frames:
                registrar("FALLA", f"{nombre}: no se pudieron leer frames del video")
                continue
            inicio = time.time()
            resultado = capa.analizar(ruta_video)
            duracion = time.time() - inicio
            if "error" in resultado:
                registrar("FALLA", f"{nombre}: el analisis devolvio error", str(resultado["error"]))
            else:
                registrar("OK", f"{nombre}: {len(frames)} frames analizados en {duracion:.1f} s")
                print("         resultado:", json.dumps(resultado, ensure_ascii=False)[:300])
        except Exception as error:
            registrar("FALLA", f"{nombre}: el analisis lanzo una excepcion", f"{type(error).__name__}: {error}")


def main():
    ruta_video = sys.argv[1] if len(sys.argv) > 1 else None
    entorno()

    clases = {"imprudencias": None, "patente": None}
    for nombre in clases:
        try:
            clases[nombre] = inspeccionar_modelo(nombre)
        except Exception as error:
            registrar("FALLA", f"{nombre}: error inesperado al inspeccionar", f"{type(error).__name__}: {error}")

    capas = {}
    try:
        from analizadores.imprudencias import AnalizadorImprudencias
        capas["imprudencias"] = probar_capa("imprudencias", AnalizadorImprudencias, clases["imprudencias"])
    except Exception as error:
        registrar("FALLA", "imprudencias: no se pudo importar la capa", f"{type(error).__name__}: {error}")
    try:
        from analizadores.patente import AnalizadorPatente
        capas["patente"] = probar_capa("patente", AnalizadorPatente, clases["patente"])
    except Exception as error:
        registrar("FALLA", "patente: no se pudo importar la capa", f"{type(error).__name__}: {error}")

    revisar_easyocr()
    if ruta_video:
        analizar_video(capas, ruta_video)
    else:
        print("\n(Sin video: pasa la ruta de un clip como argumento para probar el analisis completo.)")

    fallas = sum(1 for e, _ in RESULTADOS if e == "FALLA")
    avisos = sum(1 for e, _ in RESULTADOS if e == "AVISO")
    oks = sum(1 for e, _ in RESULTADOS if e == "OK")
    print(f"\n== Resumen: {oks} OK, {avisos} avisos, {fallas} fallas ==")
    for estado, titulo in RESULTADOS:
        if estado != "OK":
            print(f"  [{estado}] {titulo}")
    sys.exit(1 if fallas else 0)


if __name__ == "__main__":
    main()