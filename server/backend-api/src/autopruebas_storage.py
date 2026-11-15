"""
Autopruebas de las URLs firmadas de storage/ (helpers/url_firmada_helper.py), del serializador
de reportes y de cómo responde /storage (StorageFiles) con peticiones Range. No usan la base de
datos: los reportes son objetos falsos y /storage se monta sobre un storage temporal.

Uso (desde el contenedor de backend-api, en src/):
    python autopruebas_storage.py
"""
import os
import sys
import tempfile
from datetime import datetime, timezone
from types import SimpleNamespace
from urllib.parse import parse_qs, unquote, urlsplit

from starlette.applications import Starlette
from starlette.testclient import TestClient

from config.configEnv import ACCESS_TOKEN_SECRET
from entity.reporte_entity import EstadoReporte
from helpers import url_firmada_helper as uf
from helpers.reporte_serializer import serializar_reporte, serializar_reporte_admin
from helpers.storage_static import StorageFiles

RUTA = "usuario_1/reporte_2/video_1.mp4"
T = 1_800_000_000  # instante fijo para que las pruebas no dependan del reloj
FALLOS = []


def comprobar(condicion, nombre):
    print(f"  [{'OK' if condicion else 'FALLA'}] {nombre}")
    if not condicion:
        FALLOS.append(nombre)


def partes(url):
    """(ruta relativa, exp, sig) de una URL firmada."""
    separada = urlsplit(url)
    consulta = parse_qs(separada.query)
    return unquote(separada.path[len("/storage/"):]), consulta["exp"][0], consulta["sig"][0]


def sin_excepcion(funcion):
    try:
        return funcion()
    except Exception as error:  # noqa: BLE001 - se informa como falla
        return f"excepción: {error!r}"


# ── firma y verificación ───────────────────────────────────────────────────

def pruebas_firma():
    print("== 1. Firma y verificación ==")
    url = uf.url_firmada(RUTA, ahora=T)
    ruta, exp, sig = partes(url)
    comprobar(url.startswith(f"/storage/{RUTA}?exp=") and "&sig=" in url, "formato /storage/<ruta>?exp=..&sig=..")
    comprobar(ruta == RUTA and uf.firma_valida(ruta, exp, sig, ahora=T), "la URL recién firmada es válida")
    comprobar(uf._CLAVE != ACCESS_TOKEN_SECRET.encode() and len(uf._CLAVE) == 32,
              "la clave es derivada (no es el secreto del JWT tal cual)")

    print("== 2. Cualquier cambio invalida la firma ==")
    comprobar(not uf.firma_valida("usuario_1/reporte_3/video_1.mp4", exp, sig, ahora=T), "otra ruta con la misma firma")
    comprobar(not uf.firma_valida("usuario_1/reporte_2/video_1.json", exp, sig, ahora=T), "otro archivo del mismo reporte")
    comprobar(not uf.firma_valida(ruta, str(int(exp) + 1), sig, ahora=T), "expiración alterada (+1 s)")
    otra = ("A" if sig[0] != "A" else "B") + sig[1:]
    comprobar(not uf.firma_valida(ruta, exp, otra, ahora=T), "firma alterada en un carácter")
    comprobar(not uf.firma_valida(ruta, exp, sig + "=", ahora=T), "firma con relleno extra")
    comprobar(sin_excepcion(lambda: uf.firma_valida(ruta, exp, "ñandú", ahora=T)) is False,
              "firma con caracteres no ASCII -> False (sin TypeError)")
    for malo in ("", "abc", "-1", "1e10", " 1800003600", "١٨٠٠٠٠٣٦٠٠", "9" * 13):
        comprobar(sin_excepcion(lambda: uf.firma_valida(ruta, malo, sig, ahora=T)) is False,
                  f"exp inválido {malo!r} -> False")
    comprobar(not uf.firma_valida(ruta, None, sig, ahora=T) and not uf.firma_valida(ruta, exp, None, ahora=T),
              "exp o sig ausentes -> False")

    print("== 3. Vencimiento ==")
    vence = int(exp)
    comprobar(uf.firma_valida(ruta, exp, sig, ahora=vence - 1), "vale hasta un segundo antes de vencer")
    comprobar(not uf.firma_valida(ruta, exp, sig, ahora=vence), "en el instante de vencer ya no vale")
    comprobar(not uf.firma_valida(ruta, exp, sig, ahora=vence + 3600), "vencida hace una hora -> False")
    comprobar(T + uf.DURACION_S <= vence < T + uf.DURACION_S + uf.TRAMO_S and vence % uf.TRAMO_S == 0,
              "expira entre 60 y 70 min después, en un borde de tramo")

    print("== 4. Tramos: la URL es estable dentro de un tramo ==")
    inicio = vence - uf.DURACION_S - uf.TRAMO_S + 1  # primer instante cuyo exp es `vence`
    comprobar(uf.url_firmada(RUTA, ahora=inicio) == uf.url_firmada(RUTA, ahora=inicio + uf.TRAMO_S - 1),
              "misma URL durante todo el tramo (el polling no recarga el <video>)")
    comprobar(uf.url_firmada(RUTA, ahora=inicio) != uf.url_firmada(RUTA, ahora=inicio + uf.TRAMO_S),
              "URL nueva al pasar al tramo siguiente")


def pruebas_rutas():
    print("== 5. Rutas que nunca se firman ni se aceptan ==")
    rechazadas = [
        "../usuario_1/x.mp4", "usuario_1/../dataset/README.md", "usuario_1/./x.mp4", "usuario_1//x.mp4",
        "usuario_1/", "/usuario_1/x.mp4", "usuario_1\\x.mp4", "usuario_1/x\x00.mp4", "", None, 123,
        "dataset/README.md", "dataset.nuevo/x", "dataset.anterior/x", "dataset.lock", "..",
    ]
    for ruta in rechazadas:
        # Firma hecha a mano con la clave real: aun así la ruta se rechaza
        exp = str(T + 3600)
        sig = uf._firma(ruta, int(exp)) if isinstance(ruta, str) else "x"
        comprobar(uf.url_firmada(ruta, ahora=T) is None and not uf.firma_valida(ruta, exp, sig, ahora=T),
                  f"{ruta!r} rechazada")
    comprobar(uf.ruta_permitida("usuario_3/reporte_69/informe.pdf"), "una ruta normal de reporte se permite")
    comprobar(uf.ruta_permitida("datasets/x") and uf.ruta_permitida("usuario_1/dataset/x"),
              "solo se bloquea la carpeta privada exacta en la raíz (no 'datasets' ni subcarpetas)")


# ── serializador ───────────────────────────────────────────────────────────

def reporte_falso(pdf_path):
    ahora = datetime(2026, 11, 15, tzinfo=timezone.utc)
    videos = [SimpleNamespace(id=10 + n, orden=n, video_path=f"usuario_1/reporte_2/video_{n}.mp4",
                              lat=None, lon=None, grabado_en=None) for n in (1, 2)]
    return SimpleNamespace(
        id=2, titulo="t", comentario=None, estado=EstadoReporte.aprobado, pdf_path=pdf_path, notas_admin=None,
        created_at=ahora, updated_at=ahora, videos=videos, usuario_id=1, usuario=None, validacion=None,
        severidad_validada=None, patente_confirmada=None, infracciones_manuales=None, finalizado_at=None,
        infracciones=[],
    )


def pruebas_serializador():
    print("== 6. Serializador de reportes ==")
    datos = serializar_reporte(reporte_falso("usuario_1/reporte_2/informe.pdf"))
    comprobar(all(uf.firma_valida(*partes(v["url"])) and partes(v["url"])[0] == v["path"] for v in datos["videos"]),
              "cada video trae una URL firmada válida de su propia ruta")
    comprobar(datos["pdf_path"] == "usuario_1/reporte_2/informe.pdf" and uf.firma_valida(*partes(datos["pdf_url"])),
              "pdf_path se mantiene y pdf_url es una URL firmada válida")
    comprobar(serializar_reporte(reporte_falso(None))["pdf_url"] is None, "sin PDF -> pdf_url None")
    admin = serializar_reporte_admin(reporte_falso("usuario_1/reporte_2/informe.pdf"))
    comprobar("pdf_url" in admin and all("url" in v for v in admin["videos"]),
              "el serializador del panel también trae las URLs")


# ── /storage con Range ─────────────────────────────────────────────────────

def pruebas_http(storage):
    print("== 7. /storage: Range y carpetas privadas ==")
    contenido = bytes(range(256)) * 40  # 10.240 bytes
    for ruta, datos in ((RUTA, contenido), ("dataset/README.md", b"privado")):
        absoluta = os.path.join(storage, ruta)
        os.makedirs(os.path.dirname(absoluta), exist_ok=True)
        with open(absoluta, "wb") as archivo:
            archivo.write(datos)
    app = Starlette()
    app.mount("/storage", StorageFiles(directory=storage), name="storage")
    cliente = TestClient(app)

    url = uf.url_firmada(RUTA)
    r = cliente.get(url)
    comprobar(r.status_code == 200 and r.content == contenido and r.headers.get("accept-ranges") == "bytes",
              "la URL firmada descarga el archivo y anuncia Accept-Ranges")
    r = cliente.get(url, headers={"Range": "bytes=0-99"})
    comprobar(r.status_code == 206 and r.content == contenido[:100]
              and r.headers.get("content-range") == f"bytes 0-99/{len(contenido)}",
              "Range bytes=0-99 -> 206 con los primeros 100 bytes")
    r = cliente.get(url, headers={"Range": "bytes=5000-5009"})
    comprobar(r.status_code == 206 and r.content == contenido[5000:5010], "rango intermedio (buscar posición) -> 206")
    r = cliente.get(url, headers={"Range": "bytes=-10"})
    comprobar(r.status_code == 206 and r.content == contenido[-10:], "últimos 10 bytes (bytes=-10) -> 206")
    comprobar(cliente.get("/storage/dataset/README.md").status_code == 404
              and cliente.get("/storage/usuario_1/../dataset/README.md").status_code == 404,
              "dataset sigue en 404, también con '..'")
    # Estado del paso 1: todavía no se exige la firma. El paso 5 cambia esto a 403.
    comprobar(cliente.get(f"/storage/{RUTA}").status_code == 200,
              "sin firma todavía responde 200 (se exigirá en el paso 5)")


def main():
    pruebas_firma()
    pruebas_rutas()
    pruebas_serializador()
    with tempfile.TemporaryDirectory() as storage:
        pruebas_http(storage)
    print("\nTODO OK" if not FALLOS else f"\nFALLARON {len(FALLOS)}: {FALLOS}")
    sys.exit(1 if FALLOS else 0)


if __name__ == "__main__":
    main()
