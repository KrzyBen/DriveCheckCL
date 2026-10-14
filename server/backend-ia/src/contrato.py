"""
Contrato de resultados_ia (version 1).

backend-ia entrega un solo documento por reporte:

    {"version": 1, "capas": {
        "imprudencias": {"estado": "ok", "hallazgo": "posible_luz_roja", "confianza": 0.6,
                         "video": 2, "detalle": {...}},
        "patente":      {"estado": "ok", "hallazgo": "detectada", "confianza": 0.77,
                         "video": 1, "detalle": {"texto": "BCDF12", "formato_valido": true}},
        "otra_capa":    {"estado": "error", "codigo": "video_ilegible"}}}

- `video` es el numero del clip (1, 2, 3...) del que sale el hallazgo.
- Cuando algun clip se omitio (no existe o no se pudo leer) pero otros si se
  analizaron, la capa lleva ademas `omitidos: [{"video": 2, "codigo": "..."}]`.
- Este modulo no importa nada pesado (ni cv2 ni ultralytics) para poder
  probarse solo.
"""

VERSION_CONTRATO = 1

# Codigos de error de una capa
VIDEO_NO_ENCONTRADO = "video_no_encontrado"
VIDEO_ILEGIBLE = "video_ilegible"
ERROR_INTERNO = "error_interno"

# Jerarquia de hallazgos por capa: a mayor numero, mas relevante. Entre varios
# clips gana el hallazgo de mayor jerarquia; la confianza solo desempata.
JERARQUIA = {
    "imprudencias": {
        "posible_luz_roja": 3,
        "senales_detectadas": 2,
        "sin_evidencia": 1,
    },
    "patente": {
        "detectada": 3,
        "sin_lectura": 2,
        "sin_deteccion": 1,
    },
}


def _clave_de_orden(capa: str, resultado: dict) -> tuple:
    """(jerarquia, desempate propio de la capa, confianza). Mayor es mejor."""
    jerarquia = JERARQUIA.get(capa, {}).get(resultado.get("hallazgo"), 0)
    # Una patente con formato chileno valido gana a una que no lo cumple,
    # aunque esta ultima tenga mas confianza.
    formato_valido = 0
    if capa == "patente":
        formato_valido = 1 if (resultado.get("detalle") or {}).get("formato_valido") else 0
    return (jerarquia, formato_valido, resultado.get("confianza", 0.0))


def elegir_mejor(capa: str, resultados: list[dict]) -> dict:
    """
    Elige el resultado que representa al reporte entre los de cada clip.
    Cada resultado trae `video`, `hallazgo`, `confianza` y `detalle`.
    Si hay empate total, gana el primer clip.
    """
    return max(resultados, key=lambda r: _clave_de_orden(capa, r))


def capa_ok(resultado: dict, omitidos: list[dict] | None = None) -> dict:
    capa = {
        "estado": "ok",
        "hallazgo": resultado["hallazgo"],
        "confianza": resultado["confianza"],
        "video": resultado["video"],
        "detalle": resultado.get("detalle") or {},
    }
    if omitidos:
        capa["omitidos"] = omitidos
    return capa


def capa_error(codigo: str) -> dict:
    return {"estado": "error", "codigo": codigo}


def armar_contrato(capas: dict) -> dict:
    return {"version": VERSION_CONTRATO, "capas": capas}


def hay_capa_exitosa(contrato: dict) -> bool:
    """True si al menos una capa devolvio un resultado real (un contrato lleno de errores no lo es)."""
    return any(capa.get("estado") == "ok" for capa in contrato.get("capas", {}).values())
