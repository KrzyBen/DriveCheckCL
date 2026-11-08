package com.drivecheckcl.data.model

import com.google.gson.annotations.SerializedName

data class ConsentimientoRequest(
    @SerializedName("acepto_estudio")
    val aceptoEstudio: Boolean,
    val version: Int
)

data class ConsentimientoData(
    @SerializedName("acepto_estudio")
    val aceptoEstudio: Boolean,
    @SerializedName("version_aceptada")
    val versionAceptada: Int?,
    val fecha: String?,
    @SerializedName("version_vigente")
    val versionVigente: Int,
    @SerializedName("requiere_decision")
    val requiereDecision: Boolean
)
