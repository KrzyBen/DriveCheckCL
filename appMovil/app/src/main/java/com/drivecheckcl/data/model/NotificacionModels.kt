package com.drivecheckcl.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionData(
    val id: Int,
    @SerializedName("reporte_id")
    val reporteId: Int?,
    val mensaje: String,
    val leida: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class NotificacionesResponse(
    @SerializedName("no_leidas")
    val noLeidas: Int,
    val notificaciones: List<NotificacionData>
)
