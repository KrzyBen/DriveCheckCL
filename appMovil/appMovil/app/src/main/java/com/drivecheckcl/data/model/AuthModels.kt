package com.drivecheckcl.data.model

import com.google.gson.annotations.SerializedName

// ── Requests ──────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    @SerializedName("nombre_completo")
    val nombreCompleto: String,
    val rut: String,
    val email: String,
    val password: String
)

// ── Respuesta genérica del backend ────────────────────────────────────────────

data class ApiResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null,
    val details: Map<String, Any>? = null
)

// ── Datos del usuario autenticado ─────────────────────────────────────────────

data class UserData(
    val id: Int,
    @SerializedName("nombre_completo")
    val nombreCompleto: String,
    val rut: String,
    val email: String,
    val rol: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class LoginData(
    val token: String,
    val user: UserData
)