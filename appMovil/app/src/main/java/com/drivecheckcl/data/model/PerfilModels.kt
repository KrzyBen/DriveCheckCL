package com.drivecheckcl.data.model

import com.google.gson.annotations.SerializedName

/**
 * Distinto de UserData (usado en login/register): la respuesta de /perfil no
 * incluye created_at, y forzarlo con UserData produciría un campo no-nulo en
 * null vía Gson.
 */
data class PerfilData(
    val id: Int,
    @SerializedName("nombre_completo")
    val nombreCompleto: String,
    val rut: String,
    val email: String,
    val rol: String
)

data class UpdatePerfilRequest(
    @SerializedName("nombre_completo")
    val nombreCompleto: String? = null,
    val email: String? = null,
    val password: String? = null,
    @SerializedName("new_password")
    val newPassword: String? = null
)
