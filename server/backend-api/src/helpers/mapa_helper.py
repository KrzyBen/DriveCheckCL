"""Agregación espacio-temporal de eventos para el mapa de riesgo.

Funciones puras (sin base de datos): reciben eventos ya armados y devuelven
conteos por celda y por hora, más la lista de eventos para los marcadores.
"""
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

# 3 decimales ≈ 110 m de alto y ≈ 90 m de ancho en el Biobío. El calor se arma con celdas
# y no con puntos exactos; los marcadores (solo administrador) sí traen el punto del reporte.
DECIMALES_CELDA = 3
MAX_EVENTOS = 2000  # tope de marcadores por respuesta

try:
    ZONA_CHILE = ZoneInfo("America/Santiago")
except ZoneInfoNotFoundError:  # sin tzdata: UTC-3 aproximado
    ZONA_CHILE = timezone(timedelta(hours=-3))


def a_hora_chile(momento: datetime) -> datetime:
    if momento.tzinfo is None:
        momento = momento.replace(tzinfo=timezone.utc)
    return momento.astimezone(ZONA_CHILE)


def en_franja(hora: int, hora_desde: int, hora_hasta: int) -> bool:
    """hora_desde inclusive, hora_hasta exclusive. Si desde > hasta cruza la medianoche
    (22 a 6 = 22, 23, 0..5). Si desde == hasta no filtra (todo el día)."""
    if hora_desde == hora_hasta:
        return True
    if hora_desde < hora_hasta:
        return hora_desde <= hora < hora_hasta
    return hora >= hora_desde or hora < hora_hasta


def agregar(eventos, desde=None, hasta=None, hora_desde=0, hora_hasta=24, dias=None,
            severidades=None, infraccion_id=None) -> dict:
    """eventos: lista de dicts con
         id, momento (datetime), lat, lon (float|None), severidad (str|None),
         infraccion_ids (lista de ids del catálogo), infracciones (lista de textos),
         titulo (str), prueba (bool: dato sintético).

    Filtros: desde/hasta (date, hora de Chile, inclusivas); dias (días ISO, 1 = lunes);
    severidades (conjunto de textos); infraccion_id (id del catálogo); franja horaria.

    - por_hora ignora la franja (así se ve dónde cae la franja elegida);
      celdas, total y eventos sí la respetan.
    - sin_ubicacion: eventos que cumplen los filtros pero no traen GPS.
    - incluye_prueba: cuántos de los eventos devueltos son datos de prueba.
    """
    por_hora = [0] * 24
    celdas: dict[tuple, int] = {}
    lista = []
    total = 0
    sin_ubicacion = 0
    prueba = 0
    for e in eventos:
        local = a_hora_chile(e["momento"])
        if desde and local.date() < desde:
            continue
        if hasta and local.date() > hasta:
            continue
        if dias and local.isoweekday() not in dias:
            continue
        if severidades and e.get("severidad") not in severidades:
            continue
        if infraccion_id is not None and infraccion_id not in (e.get("infraccion_ids") or ()):
            continue
        if e["lat"] is None or e["lon"] is None:
            sin_ubicacion += 1
            continue
        por_hora[local.hour] += 1
        if not en_franja(local.hour, hora_desde, hora_hasta):
            continue
        clave = (round(e["lat"], DECIMALES_CELDA), round(e["lon"], DECIMALES_CELDA))
        celdas[clave] = celdas.get(clave, 0) + 1
        total += 1
        if e.get("prueba"):
            prueba += 1
        if len(lista) < MAX_EVENTOS:
            lista.append({
                "id": e.get("id"),
                "lat": e["lat"],
                "lon": e["lon"],
                "fecha_hora": local.isoformat(),
                "severidad": e.get("severidad"),
                "infracciones": list(e.get("infracciones") or []),
                "titulo": e.get("titulo") or "",
            })
    return {
        "total": total,
        "sin_ubicacion": sin_ubicacion,
        "incluye_prueba": prueba,
        "por_hora": por_hora,
        "celdas": [
            {"lat": lat, "lon": lon, "total": n}
            for (lat, lon), n in sorted(celdas.items(), key=lambda kv: -kv[1])
        ],
        "eventos": lista,
        "eventos_recortados": total > len(lista),
    }
