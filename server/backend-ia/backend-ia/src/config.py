import os

# Canal interno hacia backend-api (mismo secreto en ambos lados)
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY")
BACKEND_API_URL = os.getenv("BACKEND_API_URL", "http://backend-api:8000/api")

# Carpeta de videos compartida con backend-api (montada como solo lectura aca)
STORAGE_ROOT = os.getenv("STORAGE_ROOT", "/storage")

# Cuantos frames se muestrean por video para el analisis. Un clip de ~30s
# no necesita analizarse frame por frame: con N_FRAMES repartidos a lo
# largo del video alcanza para detectar la mayoria de los eventos.
N_FRAMES = int(os.getenv("N_FRAMES", 24))

# Si un reporte tiene mas de un video, hasta cuantos se analizan.
MAX_VIDEOS_POR_REPORTE = int(os.getenv("MAX_VIDEOS_POR_REPORTE", 3))

# Carpeta donde viven los modelos .onnx + metadata.json de cada capa
MODELOS_DIR = os.getenv("MODELOS_DIR", "/app/modelos")
