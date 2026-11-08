"""
Funciones puras para armar el dataset de infracciones. No tocan la base de datos
ni el disco (salvo leer un archivo JSON), así que se pueden probar solas.

Regla general: lo que no está en las listas blancas de abajo no sale del servidor.
"""
import hashlib
import hmac
import json
import os

ESQUEMA = 1

# Capas de la IA cuyo "detalle" es seguro exportar, y qué claves de él.
# La patente queda fuera a propósito: identifica a un tercero. Una capa nueva
# exporta solo estado/hallazgo/confianza hasta que se agregue acá.
DETALLE_PERMITIDO = {
    "imprudencias": ("elementos_detectados",),
}
CAMPOS_CAPA = ("estado", "hallazgo", "confianza", "video", "codigo", "omitidos")


def seudonimo_conductor(usuario_id: int, secreto: str) -> str:
    """Id estable del conductor que no permite volver al usuario sin el secreto del servidor."""
    mensaje = f"drivecheckcl-dataset-v1|conductor|{usuario_id}".encode()
    return hmac.new(secreto.encode(), mensaje, hashlib.sha256).hexdigest()[:12]


def clip_id(reporte_id: int, orden: int) -> str:
    return f"clip_{reporte_id:06d}_{orden}"


def limpiar_ia(resultados_ia) -> dict:
    """Resultados por capa, sin patentes ni ningún campo que no esté en la lista blanca."""
    try:
        capas = resultados_ia["capas"]
    except (KeyError, TypeError):
        return {}
    if not isinstance(capas, dict):
        return {}

    limpio = {}
    for nombre, capa in capas.items():
        if not isinstance(capa, dict):
            continue
        salida = {k: capa[k] for k in CAMPOS_CAPA if k in capa}
        permitidas = DETALLE_PERMITIDO.get(nombre)
        detalle = capa.get("detalle")
        if permitidas and isinstance(detalle, dict):
            salida["detalle"] = {k: detalle[k] for k in permitidas if k in detalle}
        limpio[nombre] = salida
    return limpio


def _punto(dato):
    if not isinstance(dato, dict):
        return None
    lat, lon = dato.get("lat"), dato.get("lon")
    if lat is None or lon is None:
        return None
    return {"lat": lat, "lon": lon, "precision_m": dato.get("precision_m")}


def leer_metadatos_clip(ruta_json: str) -> dict:
    """Lee video_N.json (lo escribe backend-api al recibir el reporte). {} si falta o está dañado."""
    try:
        with open(ruta_json, encoding="utf-8") as archivo:
            documento = json.load(archivo)
    except (OSError, ValueError):
        return {}
    return documento if isinstance(documento, dict) else {}


def ruta_json_de(video_path: str) -> str:
    return os.path.splitext(video_path)[0] + ".json"


def infracciones_del_catalogo(infracciones) -> list[dict]:
    """Etiquetas: solo el catálogo. Las filas 'Manual' son texto libre del catálogo antiguo."""
    return [
        {"id": i.id, "articulo": i.articulo, "descripcion": i.descripcion}
        for i in sorted(infracciones, key=lambda i: i.id)
        if i.articulo != "Manual"
    ]


def etiquetas_efectivas(reporte, fila):
    """
    (severidad, infracciones, origen) de un clip. fila = DatasetClip o None.
    Sin etiquetas propias, el clip sigue lo que validó el reporte.
    """
    if fila is not None and fila.etiquetas_propias:
        return fila.severidad, infracciones_del_catalogo(fila.infracciones), "dataset"
    return reporte.severidad_validada, infracciones_del_catalogo(reporte.infracciones), "reporte"


def armar_registro(*, reporte, video, usuario_id: int, secreto: str, documento: dict,
                   ia: dict, severidad, infracciones: list[dict]) -> dict:
    """Un registro de metadata.jsonl por clip. Sin identidad del conductor ni texto libre."""
    inicio = _punto(documento.get("inicio"))
    if inicio is None and video.lat is not None and video.lon is not None:
        inicio = {"lat": video.lat, "lon": video.lon, "precision_m": None}

    grabado_inicio = documento.get("grabado_inicio") or (
        video.grabado_en.isoformat() if video.grabado_en else None
    )
    return {
        "esquema": ESQUEMA,
        "clip_id": clip_id(reporte.id, video.orden),
        "evento_id": reporte.id,
        "conductor_id": seudonimo_conductor(usuario_id, secreto),
        "orden": video.orden,
        "archivo": None,          # se llena cuando el clip anonimizado se copia a clips/
        "anonimizado": False,
        "grabado_inicio": grabado_inicio,
        "grabado_fin": documento.get("grabado_fin"),
        "inicio": inicio,
        "fin": _punto(documento.get("fin")),
        "severidad": severidad,
        "infracciones": infracciones,
        "ia": ia,
    }
