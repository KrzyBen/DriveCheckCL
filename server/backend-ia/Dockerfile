backend-ia:
    build: ../server/backend-ia
    container_name: drivecheck-backend-ia
    env_file:
      - ../server/backend-api/.env   # reutiliza DB_USERNAME, PASSWORD, DATABASE
    environment:
      DB_HOST: db
      DB_PORT: 5432
      STORAGE_ROOT: /storage
      ANALISIS_INTERVAL_MINUTES: 30
      MAX_VIDEOS_POR_REPORTE: 3
      N_FRAMES: 24
    volumes:
      - storage:/storage:ro   # solo lectura: backend-ia no debe escribir videos
    ports:
      - "8100:8100"
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8100/health"]
      interval: 60s
      timeout: 10s
      retries: 3