MAX_LARGO_PATENTE = 8


def patente_sugerida(resultados_ia) -> str | None:
    """
    Patente que leyó backend-ia, lista para precargar en el reporte.

    Solo se devuelve si la capa de patente terminó bien, detectó una patente y
    esta cumple un formato chileno válido: una lectura dudosa no se precarga
    (el admin igual la ve en los resultados de la IA y puede escribirla).
    Acepta cualquier cosa como entrada y nunca lanza error.
    """
    try:
        capa = resultados_ia["capas"]["patente"]
        if capa["estado"] != "ok" or capa["hallazgo"] != "detectada":
            return None
        detalle = capa["detalle"]
        if detalle.get("formato_valido") is not True:
            return None
        texto = detalle.get("texto")
    except (KeyError, TypeError, AttributeError):
        return None

    if not isinstance(texto, str):
        return None
    texto = texto.strip().upper()
    return texto if 0 < len(texto) <= MAX_LARGO_PATENTE else None
