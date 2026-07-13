 # DriveCheckCL

Sistema de registro y análisis automatizado de conductas vehiculares imprudentes mediante inteligencia artificial. Proyecto de titulación desarrollado en la Universidad del Bío-Bío, orientado a la detección, registro y análisis de infracciones de tránsito en Chile a partir de video captado por dashcam, con referencia directa a la Ley N° 18.290 de Tránsito.

## Tabla de contenidos

* [Descripción general](#descripción-general)
  * [App móvil](#app-móvil)
  * [Backend API](#backend-api)
  * [Backend IA](#backend-ia)
  * [Frontend (panel admin)](#frontend-panel-admin)
* [Arquitectura del proyecto](#arquitectura-del-proyecto)
  * [Estructura de la app móvil](#estructura-de-la-app-móvil)
  * [Estructura de backend-api](#estructura-de-backend-api)
  * [Estructura de backend-ia](#estructura-de-backend-ia)
  * [Estructura del frontend](#estructura-del-frontend)
* [Instalación y configuración](#instalación-y-configuración)
  * [Prerrequisitos](#prerrequisitos)
  * [Clonación del repositorio](#clonación-del-repositorio)
  * [Levantar todo con Docker Compose](#levantar-todo-con-docker-compose)
  * [Variables de entorno](#variables-de-entorno)
  * [Modelos de IA (backend-ia)](#modelos-de-ia-backend-ia)
  * [App móvil (fuera de Docker)](#app-móvil-fuera-de-docker)
* [Flujo general del sistema](#flujo-general-del-sistema)
* [Tecnologías](#tecnologías)

## Descripción general

DriveCheckCL está compuesto por cuatro partes independientes que se comunican entre sí: una app móvil para que el conductor registre el trayecto, un backend central que administra usuarios y reportes, un servicio de inteligencia artificial que analiza los videos por capas, y un panel web para que administradores y validadores revisen y aprueben los reportes.

### App móvil

Aplicación Android que graba el entorno de conducción mediante la cámara delantera del teléfono o una dashcam conectada, y permite al conductor generar y enviar reportes con los videos, un comentario, y (cuando corresponda) la ubicación del evento.

Funciones principales:
- Grabación de clips de video con CameraX.
- Creación de informes con uno o más videos asociados.
- Envío de los informes al backend y seguimiento de su estado (enviado, recibido, analizando, aprobado, rechazado).
- Descarga del PDF una vez que un reporte es aprobado.

### Backend API

Servicio central (FastAPI + PostgreSQL) que administra la autenticación, los usuarios, los reportes y el catálogo de infracciones según la Ley 18.290. Es el único componente con acceso directo a la base de datos.

Funciones principales:
- Autenticación con JWT y control de acceso por rol (administrador, validador, conductor).
- Creación, listado, validación y rechazo de reportes.
- Generación de PDF del informe final con WeasyPrint.
- Servido de archivos de video y PDF.
- Canal interno (protegido con una clave compartida) para recibir los resultados del análisis de IA.

### Backend IA

Servicio (FastAPI + Ultralytics/YOLO + ONNX Runtime) que analiza los videos de un reporte en capas independientes. Cada capa es un modelo entrenado por separado; agregar una capa nueva no requiere modificar el resto del servicio.

Capas actuales:
- **Imprudencias**: detecta elementos de control de tránsito (semáforos, señales, límites de velocidad) y aplica una heurística simple para sugerir infracciones.
- **Patente**: detecta la caja de la patente en el frame y usa EasyOCR para leer el texto.

Este servicio no tiene acceso directo a la base de datos: recibe la orden de análisis desde backend-api, procesa los videos de forma asíncrona, y devuelve el resultado a backend-api mediante un canal interno autenticado con una clave compartida.

### Frontend (panel admin)

Panel web (React + Vite) para administradores y validadores. Permite gestionar usuarios, revisar reportes, disparar el análisis con IA, y validar o rechazar reportes con la infracción y severidad correspondiente.

Funciones principales:
- Gestión de usuarios (crear, editar, eliminar, asignar rol).
- Listado y filtro de reportes.
- Detalle de reporte: reproducción de los clips, resultados de las capas de IA, selección de infracciones del catálogo, notas del validador.
- Vista de solo lectura una vez que el reporte queda aprobado o rechazado.

## Arquitectura del proyecto

El proyecto se organiza en un monorepo con cuatro componentes principales y una carpeta de configuración de Docker.

```bash
├── dockerConfig
│   └── docker-compose.yml
├── server
│   ├── backend-api
│   ├── backend-ia
│   ├── frontend
│   └── storage
└── appMovil
```

### Estructura de la app móvil

```bash
appMovil
└── app/src/main/java/com/drivecheckcl
    ├── MainActivity.kt
    ├── data
    │   ├── InfractionDetector.kt
    │   ├── local
    │   │   ├── LocalStorageManager.kt
    │   │   ├── SettingsPrefs.kt
    │   │   └── NotificationHelper.kt
    │   ├── model
    │   │   ├── AuthModels.kt
    │   │   ├── ReporteModels.kt
    │   │   └── VideoModels.kt
    │   ├── network
    │   │   ├── AuthApiService.kt
    │   │   ├── ReporteApiService.kt
    │   │   └── RetrofitClient.kt
    │   └── repository
    │       ├── AuthRepository.kt
    │       └── ReporteRepository.kt
    └── ui
        ├── screens
        ├── theme
        └── viewmodel
            ├── AuthViewModel.kt
            ├── DashcamViewModel.kt
            └── InformeViewModel.kt
```

### Estructura de backend-api

```bash
backend-api
├── Dockerfile
├── requirements.txt
├── .env
└── src
    ├── main.py
    ├── auth
    │   └── passport_auth.py
    ├── config
    │   ├── configDb.py
    │   ├── configEnv.py
    │   └── initialSetup.py
    ├── controllers
    ├── entity
    │   ├── user_entity.py
    │   ├── reporte_entity.py
    │   └── reporte_validacion_entity.py
    ├── handlers
    ├── helpers
    ├── middlewares
    ├── routes
    ├── services
    └── validations
```

### Estructura de backend-ia

```bash
backend-ia
├── Dockerfile
├── requirements.txt
├── modelos
│   ├── accidentes
│   ├── imprudencias
│   └── patente
└── src
    ├── main.py
    ├── config.py
    ├── orquestador.py
    ├── callback_client.py
    └── analizadores
        ├── base.py
        ├── accidentes.py
        ├── imprudencias.py
        └── patente.py
```

Cada carpeta dentro de `modelos/` debe contener dos archivos para que su capa quede activa: `modelo.onnx` y `metadata.json`. Si faltan, esa capa simplemente se omite del análisis; el servicio no deja de funcionar por eso.

### Estructura del frontend

```bash
frontend
├── package.json
├── vite.config.js
└── src
    ├── main.jsx
    ├── components
    ├── context
    │   └── AuthContext.jsx
    ├── hooks
    │   └── reportes
    ├── pages
    ├── services
    │   ├── root.service.js
    │   ├── auth.service.js
    │   ├── reporte.service.js
    │   └── infraction.service.js
    └── styles
```

## Instalación y configuración

### Prerrequisitos

- [Docker](https://www.docker.com/) y Docker Compose (o un entorno como GitHub Codespaces, que ya lo trae configurado).
- [Node.js](https://nodejs.org/) 20.x si se quiere correr el frontend fuera de Docker.
- [Android Studio](https://developer.android.com/studio) para compilar y probar la app móvil.
- Cuenta y API key de [Kaggle](https://www.kaggle.com/) o [Google Colab](https://colab.research.google.com/) solo si se van a reentrenar los modelos de IA.

### Clonación del repositorio

```bash
git clone https://github.com/KrzyBen/DriveCheckCL.git
cd DriveCheckCL
```

### Levantar todo con Docker Compose

Todo el backend (base de datos, backend-api, backend-ia y frontend) se levanta con un solo comando desde la carpeta `dockerConfig`:

```bash
cd dockerConfig
docker compose up --build
```

Al terminar, quedan disponibles (cambiar la ip y puertos según convenga):
- Backend API en `http://localhost:8000/api`
- Frontend en `http://localhost:5173`
- Backend IA en el puerto interno `8100` (no se expone al host a propósito; solo backend-api puede llamarlo dentro de la red de Docker)

Para reconstruir solo un servicio puntual, en vez de todo el stack:

```bash
docker compose up --build backend-api
```

Para un reinicio completo (borra también los datos de la base de datos):

```bash
docker compose down -v
docker compose up --build
```

### Variables de entorno

`server/backend-api/.env` (crear a partir de `.env.example`):

```bash
DB_USERNAME=postgres
PASSWORD=postgres
DATABASE=drivecheckcl
DB_HOST=db
DB_PORT=5432
ACCESS_TOKEN_SECRET=<secreto para firmar el JWT>
FRONTEND_URL=http://localhost:5173
CORS_ORIGINS=http://localhost:5173
STORAGE_PATH=/storage
BACKEND_IA_URL=http://backend-ia:8100
INTERNAL_API_KEY=<secreto compartido con backend-ia>
```

`server/frontend/.env` (crear a partir de `.env.example`):

```bash
VITE_BASE_URL=http://localhost:8000/api
```

`INTERNAL_API_KEY` debe tener el mismo valor en `backend-api` y `backend-ia`; es lo que autentica el canal interno entre ambos servicios (no usa JWT de usuario).

### Modelos de IA (backend-ia)

Los tres modelos se entrenan por separado (ver los notebooks de entrenamiento del proyecto) y se copian manualmente a `server/backend-ia/modelos/<capa>/`, con estos dos archivos exactos en cada carpeta:

```bash
modelos/accidentes/modelo.onnx
modelos/accidentes/metadata.json
modelos/imprudencias/modelo.onnx
modelos/imprudencias/metadata.json
modelos/patente/modelo.onnx
modelos/patente/metadata.json
```

Como esa carpeta está montada como volumen, agregar o reemplazar un modelo solo requiere reiniciar el contenedor, no reconstruir la imagen:

```bash
docker compose restart backend-ia
```

Para confirmar qué capas quedaron activas:

```bash
docker compose exec backend-ia curl http://localhost:8100/health
```

### App móvil (fuera de Docker)

1. Abrir la carpeta `appMovil` en Android Studio.
2. Ajustar la URL base del backend en `RetrofitClient.kt` (debe apuntar a donde esté corriendo `backend-api`, por ejemplo la URL pública del Codespace).
3. Compilar e instalar en un dispositivo o emulador con Android 8.0 (API 26) o superior.

## Flujo general del sistema

1. El conductor graba clips desde la app y crea un reporte con uno o más videos.
2. Un administrador o validador abre el reporte en el panel web; al abrirlo, el reporte pasa automáticamente de "enviado" a "recibido".
3. El validador presiona "Analizar": backend-api le avisa a backend-ia, que procesa los videos en segundo plano por cada capa disponible y guarda el resultado en `reporte_validacion`.
4. El validador revisa el resultado de las capas, selecciona la severidad y las infracciones correspondientes del catálogo (Ley 18.290), y aprueba o rechaza el reporte.
5. Al aprobar, se genera un PDF con el informe final, disponible para que el conductor lo descargue desde la app.

## Tecnologías

### App móvil

- **Kotlin** con **Jetpack Compose** y Material3 para la interfaz.
- **CameraX** para la captura de video.
- **Retrofit** para el consumo de la API.
- Arquitectura **MVVM**.

### Backend API

- **FastAPI**: framework de Python para construir la API.
- **PostgreSQL 16** con **SQLAlchemy** como ORM.
- **python-jose** para JWT y **passlib/bcrypt** para el hash de contraseñas.
- **WeasyPrint** para la generación de los PDF de los informes.

### Backend IA

- **FastAPI** como servidor del servicio de análisis.
- **Ultralytics (YOLOv8)** y **ONNX Runtime** para la inferencia de los modelos.
- **OpenCV** para el muestreo de frames de video.
- **EasyOCR** para la lectura del texto de la patente.

### Frontend

- **React** con **Vite**.
- **Tabulator.js** para las tablas de usuarios y reportes.
- **lucide-react** para los íconos.

### Infraestructura

- **Docker** y **Docker Compose** para levantar los cuatro servicios juntos.
