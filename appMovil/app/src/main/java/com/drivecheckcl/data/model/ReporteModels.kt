package com.drivecheckcl.data.model

import com.google.gson.annotations.SerializedName

// ── Estado del informe ────────────────────────────────────────────────────────
enum class EstadoInforme {
    @SerializedName("enviado")    ENVIADO,
    @SerializedName("recibido")   RECIBIDO,
    @SerializedName("analizando") ANALIZANDO,
    @SerializedName("validando")  VALIDANDO,
    @SerializedName("resultados") RESULTADOS
}

fun EstadoInforme.etiqueta(): String = when (this) {
    EstadoInforme.ENVIADO    -> "Enviado"
    EstadoInforme.RECIBIDO   -> "Recibido"
    EstadoInforme.ANALIZANDO -> "Analizando"
    EstadoInforme.VALIDANDO  -> "Validando"
    EstadoInforme.RESULTADOS -> "Resultados"
}

// ── Modelo usado por la UI (mapeado desde la respuesta del servidor) ──────────
data class InformeLocal(
    val id: Int,
    val titulo: String,
    val comentario: String,
    val videoPaths: List<String>,
    val fechaCreacion: Long,
    val estado: EstadoInforme,
    val numeracion: Int,
    val pdfDisponible: Boolean = false
)

// ── Video dentro de un reporte (respuesta del servidor) ───────────────────────
data class VideoReporteResponse(
    val id: Int,
    val orden: Int,
    val path: String
)

// ── Respuesta completa de un reporte (igual para crear y listar) ──────────────
data class InformeResponse(
    val id: Int,
    val titulo: String,
    val comentario: String?,
    val estado: EstadoInforme,
    @SerializedName("pdf_disponible")
    val pdfDisponible: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val videos: List<VideoReporteResponse>
)

// ── Conteo de reportes (para validar límite antes de crear) ───────────────────
data class ConteoReportesResponse(
    val total: Int,
    val limite: Int
)