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

COPY src/ ./src
COPY modelos/ ./modelos

WORKDIR /app/src

EXPOSE 8100

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8100"]
