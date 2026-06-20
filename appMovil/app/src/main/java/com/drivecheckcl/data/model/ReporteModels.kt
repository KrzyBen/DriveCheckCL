package com.drivecheckcl.data.model

import com.google.gson.annotations.SerializedName

// ── Estado del informe ────────────────────────────────────────────────────────
enum class EstadoInforme {
    ENVIADO,
    RECIBIDO,
    ANALIZANDO,
    VALIDANDO,
    RESULTADOS
}

fun EstadoInforme.etiqueta(): String = when (this) {
    EstadoInforme.ENVIADO    -> "Enviado"
    EstadoInforme.RECIBIDO   -> "Recibido"
    EstadoInforme.ANALIZANDO -> "Analizando"
    EstadoInforme.VALIDANDO  -> "Validando"
    EstadoInforme.RESULTADOS -> "Resultados"
}

// ── Informe local (guardado en SharedPreferences mientras no hay backend) ─────
data class InformeLocal(
    val id: String,                    // timestamp "yyyyMMdd_HHmmss"
    val titulo: String,                // "Reporte DD/MM/YYYY HH:mm"
    val comentario: String,
    val videoPaths: List<String>,      // rutas en filesDir/reportes/reporte_{id}/
    val fechaCreacion: Long,
    val estado: EstadoInforme,
    val numeracion: Int,               // posición en la lista del usuario
    val pdfDisponible: Boolean = false
)

// ── Request para el servidor (futuro) ─────────────────────────────────────────
data class CrearInformeRequest(
    val titulo: String,
    val comentario: String
)

// ── Response del servidor (futuro) ────────────────────────────────────────────
data class InformeResponse(
    val id: String,
    val titulo: String,
    val comentario: String,
    @SerializedName("fecha_creacion")
    val fechaCreacion: String,
    val estado: String,
    val numeracion: Int,
    @SerializedName("pdf_disponible")
    val pdfDisponible: Boolean
)