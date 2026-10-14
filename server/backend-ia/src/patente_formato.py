"""
Normalizacion y validacion de patentes chilenas leidas por OCR.

Sin dependencias pesadas para poder probarse solo. Los formatos validos estan
en FORMATOS: si hay que agregar o corregir uno, se hace solo ahi.
"""
import re

# Letras que usan las patentes actuales de auto (sin vocales ni M, N, Ñ, Q).
CONSONANTES = "BCDFGHJKLPRSTVWXYZ"

# (nombre, forma por posicion: L = letra, D = digito, regex estricto).
# El orden es la preferencia cuando dos formatos calzan por igual.
FORMATOS = [
    ("actual", "LLLLDD", re.compile(rf"^[{CONSONANTES}]{{4}}[0-9]{{2}}$")),
    ("antiguo", "LLDDDD", re.compile(r"^[A-Z]{2}[0-9]{4}$")),
    ("moto", "LLLDD", re.compile(r"^[A-Z]{3}[0-9]{2}$")),
]

# Confusiones tipicas del OCR, segun lo que la posicion espera.
LETRA_A_DIGITO = {"O": "0", "Q": "0", "D": "0", "I": "1", "L": "1", "Z": "2", "S": "5", "G": "6", "B": "8"}
DIGITO_A_LETRA = {"0": "O", "1": "I", "2": "Z", "5": "S", "6": "G", "8": "B"}
DIGITO_A_CONSONANTE = {"0": "D", "1": "L", "2": "Z", "5": "S", "6": "G", "8": "B"}


def _ajustar(ventana: str, forma: str, solo_consonantes: bool):
    """Corrige letras/digitos confundidos segun la forma. Devuelve (texto, correcciones) o None."""
    mapa_letra = DIGITO_A_CONSONANTE if solo_consonantes else DIGITO_A_LETRA
    resultado = []
    correcciones = 0
    for caracter, esperado in zip(ventana, forma):
        if esperado == "D":
            if caracter.isdigit():
                resultado.append(caracter)
            elif caracter in LETRA_A_DIGITO:
                resultado.append(LETRA_A_DIGITO[caracter])
                correcciones += 1
            else:
                return None
        else:
            if caracter.isalpha():
                resultado.append(caracter)
            elif caracter in mapa_letra:
                resultado.append(mapa_letra[caracter])
                correcciones += 1
            else:
                return None
    return "".join(resultado), correcciones


def normalizar_patente(texto_crudo: str | None) -> dict:
    """
    Limpia el texto del OCR (mayusculas, sin espacios ni signos), intenta
    ajustarlo a un formato chileno corrigiendo confusiones tipicas (O/0, I/1,
    B/8, S/5...) y descartando caracteres sobrantes en los bordes.

    Devuelve:
      texto          patente normalizada (o el texto limpio si no calza con ningun formato)
      formato_valido True si calza con un formato chileno
      formato        "actual", "antiguo" o "moto" (None si no es valido)
      correcciones   cuantos caracteres se corrigieron
      texto_crudo    lo que leyo el OCR, ya limpio
    """
    limpio = re.sub(r"[^A-Z0-9]", "", (texto_crudo or "").upper())

    candidatos = []
    for orden, (nombre, forma, regex) in enumerate(FORMATOS):
        largo = len(forma)
        for inicio in range(0, len(limpio) - largo + 1):
            ventana = limpio[inicio:inicio + largo]
            ajustado = _ajustar(ventana, forma, solo_consonantes=nombre == "actual")
            if ajustado is None:
                continue
            texto, correcciones = ajustado
            if regex.match(texto):
                descartados = len(limpio) - largo
                # Corregir un caracter y descartar uno sobrante "cuestan" lo mismo;
                # si empatan, gana el formato que va antes en FORMATOS.
                costo = correcciones + descartados
                candidatos.append(((costo, orden, correcciones, inicio), nombre, texto, correcciones))

    if candidatos:
        _, nombre, texto, correcciones = min(candidatos, key=lambda c: c[0])
        return {
            "texto": texto,
            "formato_valido": True,
            "formato": nombre,
            "correcciones": correcciones,
            "texto_crudo": limpio,
        }

    return {
        "texto": limpio or None,
        "formato_valido": False,
        "formato": None,
        "correcciones": 0,
        "texto_crudo": limpio,
    }
