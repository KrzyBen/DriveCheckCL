import os

from config import STORAGE_ROOT, MAX_VIDEOS_POR_REPORTE
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
    videos_a_analizar = video_paths[:MAX_VIDEOS_POR_REPORTE]
    resultados = {}

    for capa in CAPAS:
        if not capa.disponible:
            continue

        try:
            resultados_por_video = []
            for video_relativo in videos_a_analizar:
                ruta_absoluta = _resolver_ruta_video(video_relativo)
                if ruta_absoluta is None:
                    print(f"[{capa.nombre}] Ruta fuera del storage, se ignora: {video_relativo!r}")
                    continue
                if not os.path.isfile(ruta_absoluta):
                    print(f"[{capa.nombre}] No se encontro el video: {ruta_absoluta}")
                    continue
                resultados_por_video.append(capa.analizar(ruta_absoluta))

            if not resultados_por_video:
                continue

            resultados[capa.nombre] = max(
                resultados_por_video, key=lambda r: r.get("confianza", 0)
            )

        except Exception as error:
            print(f"[{capa.nombre}] Error durante el analisis: {error}")
            resultados[capa.nombre] = {"error": str(error)}

    return resultados