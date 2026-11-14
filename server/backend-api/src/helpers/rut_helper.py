import re

# Formatos aceptados: con puntos (xx.xxx.xxx-x) o sin puntos (xxxxxxxx-x), entre 1.000.000 y 29.999.999
_PATRON_RUT = re.compile(r'^(?:(?:[1-9]\d{0}|[1-2]\d{1})(\.\d{3}){2}|[1-9]\d{6}|[1-2]\d{7}|29\.999\.999|29999999)-[\dkK]$')

MENSAJE_DV_INCORRECTO = "El dígito verificador del rut es incorrecto."


def formato_rut_valido(rut: str) -> bool:
    # fullmatch evita aceptar un salto de línea al final, que re.match con $ sí deja pasar
    return bool(_PATRON_RUT.fullmatch(rut))


def calcular_dv(cuerpo: str) -> str:
    # Algoritmo módulo 11: multiplica los dígitos de derecha a izquierda por 2, 3, ..., 7, 2, 3, ...
    suma, factor = 0, 2
    for digito in reversed(cuerpo):
        suma += int(digito) * factor
        factor = 2 if factor == 7 else factor + 1
    resto = 11 - suma % 11
    if resto == 11:
        return "0"
    if resto == 10:
        return "K"
    return str(resto)


def normalizar_rut(rut: str, mensaje_formato: str = "Formato rut inválido.") -> str:
    """Valida formato y dígito verificador, y devuelve el RUT como xx.xxx.xxx-x con la K en mayúscula.

    Lanza ValueError con mensaje_formato si el formato no es válido, o con
    MENSAJE_DV_INCORRECTO si el dígito verificador no corresponde.
    """
    if not formato_rut_valido(rut):
        raise ValueError(mensaje_formato)

    cuerpo, dv = rut.replace(".", "").split("-")
    dv = dv.upper()
    if calcular_dv(cuerpo) != dv:
        raise ValueError(MENSAJE_DV_INCORRECTO)

    # Agrupa el cuerpo en miles con puntos: 21308770 -> 21.308.770
    return f"{int(cuerpo):,}".replace(",", ".") + f"-{dv}"
