"""
Cliente síncrono de POST /anonimizar en backend-ia.

backend-ia devuelve el clip anonimizado (rostros y patentes de terceros difuminados,
sin audio). Aquí se descarga por trozos a un archivo .part y solo cuando llegó completo
se renombra al destino final: un corte a medias nunca deja un video incompleto.

Errores:
- AnonimizacionFallida: falló este clip (no se exporta y se cuenta como omitido).
- ServicioNoDisponible: falla que afectaría a todos los clips (backend-ia caído, clave
  interna incorrecta, modelos no disponibles): quien llama debe abortar la exportación.
"""
import os
from typing import Optional

import httpx

from config.configEnv import ANONIMIZAR_TIMEOUT_S, BACKEND_IA_URL, INTERNAL_API_KEY

SUFIJO_PARCIAL = ".part"
TAMANO_TROZO = 1024 * 1024   # 1 MiB por escritura

# Código HTTP de backend-ia -> motivo con que se registra el clip omitido
_MOTIVOS_CLIP = {
    400: "ruta_invalida",
    404: "video_no_encontrado",
    422: "solicitud_invalida",
    500: "error_anonimizacion",
}
# Códigos que no dependen del clip: con ellos fallarían todos, así que se aborta
_MOTIVOS_SERVICIO = {
    401: "clave_invalida",
    503: "servicio_no_disponible",
}


class AnonimizacionFallida(Exception):
    """Este clip no se pudo anonimizar. `motivo` es un código corto para el resumen."""

    def __init__(self, motivo: str, detalle: str = ""):
        super().__init__(f"{motivo}: {detalle}" if detalle else motivo)
        self.motivo = motivo


class ServicioNoDisponible(Exception):
    """backend-ia no puede anonimizar ningún clip ahora mismo: la exportación se aborta."""

    def __init__(self, motivo: str, detalle: str = ""):
        super().__init__(f"{motivo}: {detalle}" if detalle else motivo)
        self.motivo = motivo


def _timeout_por_defecto() -> httpx.Timeout:
    # Conectar debe ser rápido; leer puede tardar minutos (unos 0,2 s por frame en CPU y,
    # si backend-ia está ocupado con otro clip, también la espera de su semáforo).
    return httpx.Timeout(30.0, connect=10.0, read=float(ANONIMIZAR_TIMEOUT_S))


def _borrar(ruta: str) -> None:
    try:
        os.remove(ruta)
    except FileNotFoundError:
        pass


class ClienteAnonimizador:
    """
    Callable (video_path, destino, patente_evento) -> None que usa exportar_dataset.
    Reutiliza una sola conexión HTTP para todos los clips; cerrarlo con close() o usarlo
    con `with`. `cliente` permite inyectar un httpx.Client en las pruebas.
    """

    def __init__(self, cliente: Optional[httpx.Client] = None,
                 url: Optional[str] = None, clave: Optional[str] = None):
        self._propio = cliente is None
        self._cliente = cliente or httpx.Client(timeout=_timeout_por_defecto())
        self._url = (url or BACKEND_IA_URL).rstrip("/") + "/anonimizar"
        self._clave = INTERNAL_API_KEY if clave is None else clave

    def close(self) -> None:
        if self._propio:
            self._cliente.close()

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()

    def __call__(self, video_path: str, destino: str, patente_evento: Optional[str] = None) -> None:
        """
        video_path: ruta del video RELATIVA al storage (backend-ia la resuelve en su montaje).
        destino: ruta final del .mp4 anonimizado.
        patente_evento: patente confirmada del vehículo del evento; esa placa queda visible.
        Sin ella se difuminan todas.
        """
        if not self._clave:
            raise ServicioNoDisponible("sin_clave", "falta INTERNAL_API_KEY en backend-api")

        cuerpo = {"video_path": video_path}
        if patente_evento:
            cuerpo["patente_evento"] = patente_evento
        parcial = destino + SUFIJO_PARCIAL

        try:
            with self._cliente.stream("POST", self._url, json=cuerpo,
                                      headers={"X-Internal-Key": self._clave}) as respuesta:
                if respuesta.status_code != 200:
                    respuesta.read()
                    self._fallo_por_estado(respuesta)

                tipo = respuesta.headers.get("content-type", "")
                if not tipo.startswith("video/mp4"):
                    raise AnonimizacionFallida("respuesta_invalida", f"content-type {tipo!r}")

                escritos = 0
                with open(parcial, "wb") as archivo:
                    for trozo in respuesta.iter_bytes(TAMANO_TROZO):
                        archivo.write(trozo)
                        escritos += len(trozo)

                esperado = respuesta.headers.get("content-length")
                if esperado is not None and esperado.isdigit() and int(esperado) != escritos:
                    raise AnonimizacionFallida("respuesta_incompleta",
                                               f"llegaron {escritos} de {esperado} bytes")
                if escritos == 0:
                    raise AnonimizacionFallida("respuesta_vacia")

            os.replace(parcial, destino)
        except (httpx.ConnectError, httpx.ConnectTimeout) as error:
            raise ServicioNoDisponible("sin_conexion", str(error)) from error
        except httpx.ReadTimeout as error:
            raise AnonimizacionFallida("tiempo_agotado", str(error)) from error
        except httpx.HTTPError as error:
            # Por ejemplo, la conexión se cortó a mitad de la descarga
            raise AnonimizacionFallida("error_comunicacion", f"{type(error).__name__}: {error}") from error
        finally:
            # Si se llegó a renombrar ya no existe; en cualquier otro caso no debe quedar
            _borrar(parcial)

    @staticmethod
    def _fallo_por_estado(respuesta: httpx.Response) -> None:
        codigo = respuesta.status_code
        try:
            detalle = str(respuesta.json().get("detail", ""))
        except ValueError:
            detalle = respuesta.text[:200]
        detalle = f"HTTP {codigo} {detalle}".strip()
        if codigo in _MOTIVOS_SERVICIO:
            raise ServicioNoDisponible(_MOTIVOS_SERVICIO[codigo], detalle)
        raise AnonimizacionFallida(_MOTIVOS_CLIP.get(codigo, f"http_{codigo}"), detalle)
