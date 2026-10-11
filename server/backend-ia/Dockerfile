FROM python:3.11-slim

# Dependencias del sistema que necesitan opencv y easyocr para leer
# video/imagenes (sin esto, cv2 falla al importarse en una imagen slim).
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pesos de EasyOCR (detector CRAFT + reconocimiento en ingles, ~95 MB). Se bajan
# aqui, al construir la imagen: asi el contenedor no necesita internet cuando
# analiza la primera patente ni los vuelve a descargar al recrearse. Esta capa
# va antes de copiar src/, por lo que solo se repite si cambia requirements.txt.
RUN python -c "import easyocr; easyocr.Reader(['en'], gpu=False, verbose=False)"

COPY src/ ./src
COPY modelos/ ./modelos

WORKDIR /app/src

EXPOSE 8100

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8100"]