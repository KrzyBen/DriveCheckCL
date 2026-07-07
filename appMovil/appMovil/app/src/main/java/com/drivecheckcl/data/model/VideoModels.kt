package com.drivecheckcl.data.model

// ── Video local guardado en la app ────────────────────────────────────────────
data class VideoLocal(
    val id: String,
    val nombre: String,
    val rutaArchivo: String,
    val duracionSegundos: Int,
    val fechaGrabacion: Long,
    val subido: Boolean = false
)

// ── Reporte recibido desde el servidor ───────────────────────────────────────
data class Reporte(
    val id: String,
    val videoId: String,
    val fechaAnalisis: String,
    val puntaje: Int,
    val infracciones: List<Infraccion>
)

data class Infraccion(
    val tipo: String,
    val descripcion: String,
    val articulo: String,
    val confianza: Float,
    val timestamp: Float
)

// ── Request/Response para API ─────────────────────────────────────────────────
data class UploadVideoResponse(
    val videoId: String,
    val mensaje: String
)