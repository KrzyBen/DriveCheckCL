import os

from config import STORAGE_ROOT, MAX_VIDEOS_POR_REPORTE
from analizadores.accidentes import AnalizadorAccidentes
from analizadores.imprudencias import AnalizadorImprudencias
from analizadores.patente import AnalizadorPatente

# Agregar una capa nueva es agregarla a esta lista. No hay que tocar
# nada mas de este archivo ni de main.py.
CAPAS = [
    AnalizadorAccidentes(),
    AnalizadorImprudencias(),
    AnalizadorPatente(),
]


def ejecutar_pipeline(video_paths: list[str]) -> dict:
    """
    Corre todas las capas habilitadas sobre los videos de un reporte y
    devuelve el diccionario que se guarda en resultados_ia.

    Si una capa falla (o su modelo todavia no esta disponible), no se
    cae el analisis completo: esa capa queda ausente del resultado, y
    las demas capas se guardan igual.
    """
    videos_a_analizar = video_paths[:MAX_VIDEOS_POR_REPORTE]
    resultados = {}

    for capa in CAPAS:
        if not capa.disponible:
            continue

        try:
            resultados_por_video = []
            for video_relativo in videos_a_analizar:
                ruta_absoluta = os.path.join(STORAGE_ROOT, video_relativo)
                if not os.path.isfile(ruta_absoluta):
                    print(f"[{capa.nombre}] No se encontro el video: {ruta_absoluta}")
                    continue
                resultados_por_video.append(capa.analizar(ruta_absoluta))

            if not resultados_por_video:
                continue

            # Si el reporte tiene mas de un video, se toma el resultado
            # con mayor confianza entre todos los videos analizados.
            resultados[capa.nombre] = max(
                resultados_por_video, key=lambda r: r.get("confianza", 0)
            )

        except Exception as error:
            print(f"[{capa.nombre}] Error durante el analisis: {error}")
            resultados[capa.nombre] = {"error": str(error)}

    return resultados
