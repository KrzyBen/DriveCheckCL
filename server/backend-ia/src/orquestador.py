import os

from config import STORAGE_ROOT, MAX_VIDEOS_POR_REPORTE
from contrato import (
    ERROR_INTERNO,
    VIDEO_ILEGIBLE,
    VIDEO_NO_ENCONTRADO,
    armar_contrato,
    capa_error,
    capa_ok,
    elegir_mejor,
)
from analizadores.base import VideoIlegibleError
from analizadores.imprudencias import AnalizadorImprudencias
from analizadores.patente import AnalizadorPatente


CAPAS = [
    AnalizadorImprudencias(),
    AnalizadorPatente(),
]


def _resolver_ruta_video(video_relativo: str) -> str | None:
    try:
        raiz = os.path.realpath(STORAGE_ROOT)
        ruta = os.path.realpath(os.path.join(raiz, video_relativo))
        if os.path.commonpath([raiz, ruta]) != raiz:
            return None
    except ValueError:
        return None
    return ruta


def ejecutar_pipeline(video_paths: list[str]) -> dict:
    """
    Corre cada capa disponible sobre los videos del reporte y devuelve el
    contrato de resultados_ia (ver contrato.py). Entre los videos de un
    reporte se elige por la jerarquia del hallazgo, no por la confianza, y
    cada capa indica de que video viene su resultado.
    """
    videos_a_analizar = video_paths[:MAX_VIDEOS_POR_REPORTE]
    capas_resultado = {}

    for capa in CAPAS:
        if not capa.disponible:
            continue

        try:
            resultados_por_video = []
            omitidos = []

            for numero_video, video_relativo in enumerate(videos_a_analizar, start=1):
                ruta_absoluta = _resolver_ruta_video(video_relativo)
                if ruta_absoluta is None:
                    print(f"[{capa.nombre}] Ruta fuera del storage, se ignora: {video_relativo!r}")
                    omitidos.append({"video": numero_video, "codigo": VIDEO_NO_ENCONTRADO})
                    continue
                if not os.path.isfile(ruta_absoluta):
                    print(f"[{capa.nombre}] No se encontro el video: {ruta_absoluta}")
                    omitidos.append({"video": numero_video, "codigo": VIDEO_NO_ENCONTRADO})
                    continue

                try:
                    resultado = capa.analizar(ruta_absoluta)
                except VideoIlegibleError:
                    print(f"[{capa.nombre}] No se pudieron leer frames de: {ruta_absoluta}")
                    omitidos.append({"video": numero_video, "codigo": VIDEO_ILEGIBLE})
                    continue

                resultado["video"] = numero_video
                resultados_por_video.append(resultado)

            if not resultados_por_video:
                hubo_ilegible = any(o["codigo"] == VIDEO_ILEGIBLE for o in omitidos)
                capas_resultado[capa.nombre] = capa_error(VIDEO_ILEGIBLE if hubo_ilegible else VIDEO_NO_ENCONTRADO)
                continue

            capas_resultado[capa.nombre] = capa_ok(
                elegir_mejor(capa.nombre, resultados_por_video), omitidos
            )

        except Exception as error:
            # El detalle va al log del servidor; al reporte solo llega el codigo.
            print(f"[{capa.nombre}] Error durante el analisis: {type(error).__name__}: {error}")
            capas_resultado[capa.nombre] = capa_error(ERROR_INTERNO)

    return armar_contrato(capas_resultado)
