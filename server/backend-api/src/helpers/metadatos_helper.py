import json
import math
from datetime import datetime

# Tope de tamaño del campo "metadatos" que envía la app (3 clips ocupan ~1 KB).
MAX_CARACTERES_METADATOS = 20_000


def _numero(valor, minimo: float, maximo: float):
    """Devuelve el valor como float si es un número finito dentro del rango; si no, None."""
    if isinstance(valor, bool) or not isinstance(valor, (int, float)):
        return None
    valor = float(valor)
    if not math.isfinite(valor) or valor < minimo or valor > maximo:
        return None
    return valor


def _fecha(valor):
    """Fecha ISO-8601 con zona horaria (ej. 2026-10-11T15:30:12-03:00). Sin zona o inválida: None."""
    if not isinstance(valor, str):
        return None
    try:
        fecha = datetime.fromisoformat(valor.strip())
    except ValueError:
        return None
    if fecha.tzinfo is None:
        return None
    return fecha


def _punto(dato):
    """Punto GPS limpio ({fecha_hora, lat, lon, precision_m}) o None si no es utilizable."""
    if not isinstance(dato, dict):
        return None
    lat = _numero(dato.get("lat"), -90, 90)
    lon = _numero(dato.get("lon"), -180, 180)
    if lat is None or lon is None:
        return None
    fecha = _fecha(dato.get("fecha_hora"))
    return {
        "fecha_hora": fecha.isoformat() if fecha else None,
        "lat": lat,
        "lon": lon,
        "precision_m": _numero(dato.get("precision_m"), 0, 100_000),
    }


def _vacio() -> dict:
    return {"lat": None, "lon": None, "grabado_en": None, "documento": None}


def _normalizar_clip(item) -> dict:
    """
    Convierte el JSON de un clip en los datos que se guardan:
    lat/lon (punto focal = inicio del clip), grabado_en (hora de inicio) y el
    "documento" limpio que se escribe como video_N.json en el storage.
    Solo se conservan los campos conocidos; el resto se descarta.
    """
    if not isinstance(item, dict):
        return _vacio()

    inicio = _punto(item.get("inicio"))
    fin = _punto(item.get("fin"))
    grabado_inicio = _fecha(item.get("grabado_inicio"))
    grabado_fin = _fecha(item.get("grabado_fin"))

    if not (inicio or fin or grabado_inicio):
        return _vacio()

    clip = item.get("clip")
    return {
        "lat": inicio["lat"] if inicio else None,
        "lon": inicio["lon"] if inicio else None,
        "grabado_en": grabado_inicio,
        "documento": {
            "version": 1,
            "clip": clip[:255] if isinstance(clip, str) else None,
            "grabado_inicio": grabado_inicio.isoformat() if grabado_inicio else None,
            "grabado_fin": grabado_fin.isoformat() if grabado_fin else None,
            "inicio": inicio,
            "fin": fin,
        },
    }


def parsear_metadatos(texto, cantidad_videos: int) -> list[dict]:
    """
    Interpreta el campo "metadatos" que envía la app: un arreglo JSON con un
    elemento por video, en el mismo orden que los videos.

    Nunca lanza error: los metadatos son opcionales y un dato inválido no debe
    impedir que el reporte se cree. Si el campo falta, es inválido o no tiene
    exactamente un elemento por video (no se puede saber qué dato es de qué
    clip), todos los clips quedan sin metadatos.
    """
    vacios = [_vacio() for _ in range(cantidad_videos)]

    if not texto or len(texto) > MAX_CARACTERES_METADATOS:
        return vacios
    try:
        lista = json.loads(texto)
    except ValueError:
        return vacios
    if not isinstance(lista, list) or len(lista) != cantidad_videos:
        return vacios

    return [_normalizar_clip(item) for item in lista]
