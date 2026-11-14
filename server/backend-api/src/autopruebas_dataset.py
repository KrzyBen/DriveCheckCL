"""
Autopruebas de la exportación del dataset con anonimización. No usan la base de datos ni
backend-ia: los reportes son objetos falsos y el anonimizador es una función de prueba o
el cliente real con un transporte HTTP simulado (httpx.MockTransport).

Uso (desde el contenedor de backend-api, en src/):
    python autopruebas_dataset.py
"""
import json
import os
import sys
import tempfile
from types import SimpleNamespace

import httpx

from config.consentimiento import CONSENTIMIENTO_VERSION_VIGENTE
from entity.reporte_entity import EstadoReporte
from helpers import storage_static
from helpers.anonimizador_client import (
    SUFIJO_PARCIAL, AnonimizacionFallida, ClienteAnonimizador, ServicioNoDisponible,
)
from services import dataset_service as ds

SECRETO = "secreto-de-prueba"
ORIGINAL = b"VIDEO-ORIGINAL-SIN-ANONIMIZAR"
FALLOS = []


def comprobar(condicion, nombre):
    print(f"  [{'OK' if condicion else 'FALLA'}] {nombre}")
    if not condicion:
        FALLOS.append(nombre)


# ── datos falsos ───────────────────────────────────────────────────────────

def video(id_, reporte_id, orden):
    return SimpleNamespace(id=id_, orden=orden, video_path=f"usuario_1/reporte_{reporte_id}/video_{orden}.mp4",
                           lat=None, lon=None, grabado_en=None)


def reporte(id_, videos, patente=None):
    return SimpleNamespace(
        id=id_, titulo=f"Reporte {id_}", estado=EstadoReporte.aprobado, usuario_id=1,
        usuario=SimpleNamespace(consentimiento_estudio=True, consentimiento_version=CONSENTIMIENTO_VERSION_VIGENTE),
        validacion=None, videos=videos, patente_confirmada=patente,
        severidad_validada="leve", infracciones=[],
    )


def preparar(storage, reportes):
    """Crea los videos originales en `storage` y engancha los datos falsos en dataset_service."""
    for r in reportes:
        for v in r.videos:
            ruta = os.path.join(storage, v.video_path)
            os.makedirs(os.path.dirname(ruta), exist_ok=True)
            with open(ruta, "wb") as archivo:
                archivo.write(ORIGINAL)
    ds.reportes_con_todo = lambda db: reportes
    ds.filas_por_video = lambda db: {}
    ds.get_ruta_absoluta = lambda relativa: os.path.join(storage, relativa)


class AnonimizadorFalso:
    """Escribe un video 'anonimizado' distinto del original; puede fallar en clips elegidos."""

    def __init__(self, fallos=None):
        self.fallos = fallos or {}     # video_path -> excepción a lanzar
        self.llamadas = []             # (video_path, patente_evento)

    def __call__(self, video_path, destino, patente_evento=None):
        self.llamadas.append((video_path, patente_evento))
        if video_path in self.fallos:
            raise self.fallos[video_path]
        with open(destino, "wb") as archivo:
            archivo.write(b"ANONIMIZADO:" + video_path.encode())


def foto(carpeta):
    """{ruta relativa: contenido} de todo lo que hay en la carpeta, para comparar antes/después."""
    contenido = {}
    for raiz, _, archivos in os.walk(carpeta):
        for nombre in archivos:
            ruta = os.path.join(raiz, nombre)
            with open(ruta, "rb") as archivo:
                contenido[os.path.relpath(ruta, carpeta)] = archivo.read()
    return contenido


def metadata(destino):
    with open(os.path.join(destino, "metadata.jsonl"), encoding="utf-8") as archivo:
        return [json.loads(linea) for linea in archivo if linea.strip()]


def sin_restos(destino):
    return not any(os.path.exists(destino + s) for s in (ds.SUFIJO_NUEVO, ds.SUFIJO_ANTERIOR))


def sin_original(destino):
    return all(c != ORIGINAL for c in foto(destino).values())


# ── pruebas de exportar_dataset ────────────────────────────────────────────

def pruebas_exportacion(storage):
    destino = os.path.join(storage, "dataset")
    r1 = reporte(1, [video(11, 1, 1), video(12, 1, 2)], patente="BCDF12")
    r2 = reporte(2, [video(21, 2, 1)])
    r3 = reporte(3, [video(31, 3, 1)])
    preparar(storage, [r1, r2, r3])

    print("== 1. Exportación completa ==")
    anon = AnonimizadorFalso()
    res = ds.exportar_dataset(None, destino, anon, SECRETO)
    regs = metadata(destino)
    comprobar(res["clips"] == 4 and len(regs) == 4, "4 clips en el resumen y en metadata.jsonl")
    comprobar(all(r["anonimizado"] and os.path.isfile(os.path.join(destino, r["archivo"])) for r in regs),
              "cada registro apunta a un clip anonimizado que existe")
    comprobar(sin_original(destino), "ningún archivo del dataset es el video original")
    comprobar(("usuario_1/reporte_1/video_1.mp4", "BCDF12") in anon.llamadas
              and ("usuario_1/reporte_2/video_1.mp4", None) in anon.llamadas,
              "se pasa la ruta relativa y la patente confirmada (o None si no hay)")
    comprobar(res["omitidos"]["total"] == 0 and sin_restos(destino), "sin omitidos ni carpetas temporales")
    with open(os.path.join(destino, "README.md"), encoding="utf-8") as archivo:
        readme = archivo.read()
    comprobar("queda visible" in readme and "omitidos" not in readme,
              "el README avisa que la patente del evento queda visible y no menciona omitidos")
    dataset_1 = foto(destino)

    print("== 2. Un clip falla: se omite con su motivo ==")
    anon = AnonimizadorFalso({"usuario_1/reporte_1/video_2.mp4": AnonimizacionFallida("tiempo_agotado")})
    res = ds.exportar_dataset(None, destino, anon, SECRETO)
    ids = {r["clip_id"] for r in metadata(destino)}
    comprobar("clip_000001_2" not in ids and len(ids) == 3, "el clip fallido no está en metadata.jsonl")
    comprobar(not os.path.exists(os.path.join(destino, "clips", "clip_000001_2.mp4")),
              "ni en clips/ (tampoco la versión de la exportación anterior)")
    comprobar(sin_original(destino), "el original nunca se copia")
    comprobar(res["omitidos"]["total"] == 1 and res["omitidos"]["motivos"] == {"tiempo_agotado": 1}
              and res["omitidos"]["clips"] == [{"clip_id": "clip_000001_2", "motivo": "tiempo_agotado"}],
              "omitidos: total, motivos y lista de clips")
    with open(os.path.join(destino, "resumen.json"), encoding="utf-8") as archivo:
        resumen_json = json.load(archivo)
    comprobar(resumen_json["omitidos"] == {"total": 1, "motivos": {"tiempo_agotado": 1}},
              "resumen.json guarda los conteos sin la lista de clips")
    with open(os.path.join(destino, "README.md"), encoding="utf-8") as archivo:
        readme = archivo.read()
    comprobar("Clips omitidos porque no se pudieron anonimizar: 1 (tiempo_agotado: 1)" in readme,
              "el README dice cuántos se omitieron y por qué")

    print("== 3. Todos los clips de un reporte fallan (otros no) ==")
    anon = AnonimizadorFalso({"usuario_1/reporte_2/video_1.mp4": AnonimizacionFallida("error_anonimizacion")})
    res = ds.exportar_dataset(None, destino, anon, SECRETO)
    comprobar(res["excluidos"]["sin_video"] == 0 and res["omitidos"]["total"] == 1 and res["eventos"] == 2,
              "el reporte no se cuenta como 'sin video', sino como omitido")

    # Se vuelve al dataset completo para comparar los abortos contra él
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO)
    comprobar(foto(destino).keys() == dataset_1.keys(), "(se regenera el dataset completo de referencia)")
    referencia = foto(destino)

    print("== 4. backend-ia no disponible: se aborta sin tocar el dataset ==")
    anon = AnonimizadorFalso({"usuario_1/reporte_1/video_2.mp4": ServicioNoDisponible("servicio_no_disponible")})
    try:
        ds.exportar_dataset(None, destino, anon, SECRETO)
        comprobar(False, "debía abortar")
    except ds.ExportacionAbortada as error:
        comprobar(error.motivo == "servicio_no_disponible", f"ExportacionAbortada ({error.motivo})")
    comprobar(foto(destino) == referencia, "el dataset anterior queda idéntico")
    comprobar(sin_restos(destino), "no queda dataset.nuevo")
    comprobar(len(anon.llamadas) == 2, "no se siguen pidiendo clips después del fallo del servicio")

    print("== 5. Fallan todos los clips: se aborta ==")
    # Con 3 o más candidatos cortaría antes por fallos consecutivos (sección 5b), así que
    # esta sección usa un storage aparte con un solo reporte de 2 clips.
    storage_dos = os.path.join(storage, "solo_dos")
    destino_dos = os.path.join(storage_dos, "dataset")
    preparar(storage_dos, [reporte(4, [video(41, 4, 1), video(42, 4, 2)])])
    ds.exportar_dataset(None, destino_dos, AnonimizadorFalso(), SECRETO)
    referencia_dos = foto(destino_dos)
    todos = {f"usuario_1/reporte_4/video_{o}.mp4": AnonimizacionFallida("error_anonimizacion") for o in (1, 2)}
    try:
        ds.exportar_dataset(None, destino_dos, AnonimizadorFalso(todos), SECRETO)
        comprobar(False, "debía abortar")
    except ds.ExportacionAbortada as error:
        comprobar(error.motivo == "todos_fallaron", "ExportacionAbortada (todos_fallaron)")
    comprobar(foto(destino_dos) == referencia_dos and sin_restos(destino_dos), "el dataset anterior queda idéntico")
    preparar(storage, [r1, r2, r3])   # se vuelve al conjunto principal

    print("== 5b. Fallan 3 clips seguidos: se aborta sin esperar al resto ==")
    tres = {f"usuario_1/reporte_{r}/video_{o}.mp4": AnonimizacionFallida("tiempo_agotado")
            for r, o in ((1, 1), (1, 2), (2, 1))}
    anon = AnonimizadorFalso(tres)
    try:
        ds.exportar_dataset(None, destino, anon, SECRETO)
        comprobar(False, "debía abortar")
    except ds.ExportacionAbortada as error:
        comprobar(error.motivo == "fallos_consecutivos", "ExportacionAbortada (fallos_consecutivos)")
    comprobar(len(anon.llamadas) == 3, "no se pide el 4.º clip")
    comprobar(foto(destino) == referencia and sin_restos(destino), "el dataset anterior queda idéntico")
    # Falla, falla, sale bien, falla: el éxito reinicia el contador y no se aborta
    alternados = {f"usuario_1/reporte_{r}/video_{o}.mp4": AnonimizacionFallida("tiempo_agotado")
                  for r, o in ((1, 1), (1, 2), (3, 1))}
    res = ds.exportar_dataset(None, destino, AnonimizadorFalso(alternados), SECRETO)
    comprobar(res["clips"] == 1 and res["omitidos"]["total"] == 3,
              "un clip exitoso entre fallos reinicia el contador: se exporta con 3 omitidos")
    # Se vuelve al dataset completo (README y resumen.json llevan la hora: nueva referencia)
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO)
    referencia = foto(destino)

    print("== 6. Error inesperado: se propaga y no toca el dataset ==")
    try:
        ds.exportar_dataset(None, destino, AnonimizadorFalso({"usuario_1/reporte_2/video_1.mp4": OSError("disco lleno")}),
                            SECRETO)
        comprobar(False, "debía lanzar el error")
    except OSError:
        comprobar(True, "el OSError llega a quien llamó")
    comprobar(foto(destino) == referencia and sin_restos(destino), "el dataset anterior queda idéntico")

    print("== 7. Restos de una exportación cortada ==")
    os.makedirs(os.path.join(destino + ds.SUFIJO_NUEVO, "clips"))
    with open(os.path.join(destino + ds.SUFIJO_NUEVO, "clips", "clip_000001_1.mp4" + SUFIJO_PARCIAL), "wb") as a:
        a.write(b"a medias")
    with open(os.path.join(destino, "clips", "huerfano.mp4" + SUFIJO_PARCIAL), "wb") as a:
        a.write(b"a medias")
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO)
    partes = [n for n in foto(destino) if n.endswith(SUFIJO_PARCIAL)]
    comprobar(not partes and sin_restos(destino), "se borran dataset.nuevo y los .part huérfanos")
    # Corte entre los dos renombres de _reemplazar: el dataset está en dataset.anterior
    os.rename(destino, destino + ds.SUFIJO_ANTERIOR)
    ds._limpiar_restos(destino)
    comprobar(os.path.isfile(os.path.join(destino, "metadata.jsonl")) and sin_restos(destino),
              "se restaura dataset.anterior si dataset no existe")

    print("== 8. Dos exportaciones a la vez ==")
    with ds._bloqueo_exportacion(destino):
        try:
            ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO)
            comprobar(False, "debía rechazar la segunda")
        except ds.ExportacionEnCurso:
            comprobar(True, "la segunda recibe ExportacionEnCurso")
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO)
    comprobar(True, "al soltar el candado se puede volver a exportar")

    print("== 9. Sin anonimizador no se copia ningún video ==")
    res = ds.exportar_dataset(None, destino, None, SECRETO)
    comprobar(res["sin_anonimizar"] == 4 and not os.listdir(os.path.join(destino, "clips")),
              "clips/ vacía y sin_anonimizar = 4")
    comprobar(all(not r["anonimizado"] and r["archivo"] is None for r in metadata(destino)),
              "registros con anonimizado=false y archivo=null")

    print("== 9b. Aviso de progreso (al_avanzar) ==")
    avisos = []
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO,
                        al_avanzar=lambda a, t, c: avisos.append((a, t, c)))
    comprobar(avisos == [(1, 4, "clip_000001_1"), (2, 4, "clip_000001_2"),
                         (3, 4, "clip_000002_1"), (4, 4, "clip_000003_1")],
              "se llama antes de cada clip con (actual, total, clip_id)")
    # Un clip sin archivo en disco no cuenta en el total
    ruta_r3 = os.path.join(storage, "usuario_1/reporte_3/video_1.mp4")
    os.rename(ruta_r3, ruta_r3 + ".aparte")
    avisos.clear()
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO,
                        al_avanzar=lambda a, t, c: avisos.append((a, t, c)))
    os.rename(ruta_r3 + ".aparte", ruta_r3)
    comprobar([t for _, t, _ in avisos] == [3, 3, 3] and len(avisos) == 3, "el total solo cuenta clips con archivo")
    avisos.clear()
    ds.exportar_dataset(None, destino, None, SECRETO, al_avanzar=lambda a, t, c: avisos.append((a, t, c)))
    comprobar(avisos == [], "sin anonimizador no se llama")

    print("== 9c. Mensajes del script de consola ==")
    import contextlib
    import io
    import exportar_dataset as script
    salida = io.StringIO()
    falso = AnonimizadorFalso({"usuario_1/reporte_1/video_2.mp4": AnonimizacionFallida("tiempo_agotado")})
    with contextlib.redirect_stdout(salida):
        ds.exportar_dataset(None, destino, script.con_reporte(falso), SECRETO, al_avanzar=script.al_avanzar)
    lineas = salida.getvalue().splitlines()
    comprobar(lineas[0].startswith("  Anonimizando clip 1 de 4 (clip_000001_1)... listo en ")
              and lineas[0].endswith(" s"), f"éxito: {lineas[0].strip()!r}")
    comprobar(lineas[1] == "  Anonimizando clip 2 de 4 (clip_000001_2)... omitido (tiempo_agotado)",
              f"omisión: {lineas[1].strip()!r}")
    ds.exportar_dataset(None, destino, AnonimizadorFalso(), SECRETO)   # deja el dataset completo

    print("== 10. Las carpetas de trabajo no se sirven por /storage ==")
    nombres = {os.path.basename(destino) + s for s in (ds.SUFIJO_NUEVO, ds.SUFIJO_ANTERIOR, ds.SUFIJO_LOCK)}
    comprobar(nombres <= set(storage_static.CARPETAS_PRIVADAS), f"{sorted(nombres)} son privadas")


# ── pruebas del cliente HTTP ───────────────────────────────────────────────

class CuerpoCortado(httpx.SyncByteStream):
    """Entrega un trozo y luego simula que la conexión se corta."""

    def __iter__(self):
        yield b"primer-trozo"
        raise httpx.ReadError("conexión cortada")


def cliente_con(manejador):
    return ClienteAnonimizador(cliente=httpx.Client(transport=httpx.MockTransport(manejador)),
                               url="http://backend-ia:8100", clave="clave-prueba")


def pruebas_cliente(carpeta):
    destino = os.path.join(carpeta, "salida.mp4")

    def limpio():
        return not os.path.exists(destino) and not os.path.exists(destino + SUFIJO_PARCIAL)

    print("== 11. Cliente: respuesta correcta ==")
    vistas = []

    def ok(request):
        vistas.append(request)
        return httpx.Response(200, headers={"content-type": "video/mp4"}, content=b"mp4-anonimizado")

    cliente_con(ok)("usuario_1/reporte_1/video_1.mp4", destino, "BCDF12")
    with open(destino, "rb") as archivo:
        comprobar(archivo.read() == b"mp4-anonimizado", "el video queda en destino")
    comprobar(not os.path.exists(destino + SUFIJO_PARCIAL), "no queda el .part")
    pedido = vistas[0]
    comprobar(pedido.url.path == "/anonimizar" and pedido.headers.get("x-internal-key") == "clave-prueba",
              "POST /anonimizar con X-Internal-Key")
    comprobar(json.loads(pedido.content) == {"video_path": "usuario_1/reporte_1/video_1.mp4", "patente_evento": "BCDF12"},
              "cuerpo con la ruta relativa y la patente")
    os.remove(destino)
    cliente_con(ok)("v.mp4", destino, None)
    comprobar("patente_evento" not in json.loads(vistas[1].content), "sin patente no se envía el campo")
    os.remove(destino)

    print("== 12. Cliente: errores por clip y del servicio ==")
    casos = [(400, AnonimizacionFallida, "ruta_invalida"), (404, AnonimizacionFallida, "video_no_encontrado"),
             (500, AnonimizacionFallida, "error_anonimizacion"), (502, AnonimizacionFallida, "http_502"),
             (401, ServicioNoDisponible, "clave_invalida"), (503, ServicioNoDisponible, "servicio_no_disponible")]
    for codigo, clase, motivo in casos:
        try:
            cliente_con(lambda r, c=codigo: httpx.Response(c, json={"detail": "x"}))("v.mp4", destino)
            comprobar(False, f"HTTP {codigo} debía fallar")
        except (AnonimizacionFallida, ServicioNoDisponible) as error:
            comprobar(type(error) is clase and error.motivo == motivo and limpio(),
                      f"HTTP {codigo} -> {clase.__name__}({motivo})")

    def lanza(excepcion):
        def manejador(request):
            raise excepcion
        return manejador

    otros = [
        ("sin conexión", lanza(httpx.ConnectError("rechazada")), ServicioNoDisponible, "sin_conexion"),
        ("timeout de lectura", lanza(httpx.ReadTimeout("lento")), AnonimizacionFallida, "tiempo_agotado"),
        ("corte a mitad de la descarga",
         lambda r: httpx.Response(200, headers={"content-type": "video/mp4"}, stream=CuerpoCortado()),
         AnonimizacionFallida, "error_comunicacion"),
        ("content-type distinto", lambda r: httpx.Response(200, headers={"content-type": "application/json"}, content=b"{}"),
         AnonimizacionFallida, "respuesta_invalida"),
        ("cuerpo vacío", lambda r: httpx.Response(200, headers={"content-type": "video/mp4"}, content=b""),
         AnonimizacionFallida, "respuesta_vacia"),
        ("content-length no calza",
         lambda r: httpx.Response(200, headers={"content-type": "video/mp4", "content-length": "999"}, content=b"corto"),
         AnonimizacionFallida, "respuesta_incompleta"),
    ]
    for nombre, manejador, clase, motivo in otros:
        try:
            cliente_con(manejador)("v.mp4", destino)
            comprobar(False, f"{nombre}: debía fallar")
        except (AnonimizacionFallida, ServicioNoDisponible) as error:
            comprobar(type(error) is clase and error.motivo == motivo and limpio(),
                      f"{nombre} -> {clase.__name__}({motivo}), sin archivos a medias")

    try:
        ClienteAnonimizador(cliente=httpx.Client(transport=httpx.MockTransport(ok)), clave="")("v.mp4", destino)
        comprobar(False, "sin clave debía fallar")
    except ServicioNoDisponible as error:
        comprobar(error.motivo == "sin_clave" and limpio(), "sin INTERNAL_API_KEY -> ServicioNoDisponible(sin_clave)")


def main():
    with tempfile.TemporaryDirectory() as storage:
        pruebas_exportacion(storage)
    with tempfile.TemporaryDirectory() as carpeta:
        pruebas_cliente(carpeta)
    print("\nTODO OK" if not FALLOS else f"\nFALLARON {len(FALLOS)}: {FALLOS}")
    sys.exit(1 if FALLOS else 0)


if __name__ == "__main__":
    main()
