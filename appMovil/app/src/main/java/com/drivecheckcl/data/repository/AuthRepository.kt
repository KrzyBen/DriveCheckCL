package com.drivecheckcl.data.repository

import com.drivecheckcl.data.model.LoginData
import com.drivecheckcl.data.model.LoginRequest
import com.drivecheckcl.data.model.RegisterRequest
import com.drivecheckcl.data.model.UserData
import com.drivecheckcl.data.network.AuthApiService
import com.drivecheckcl.data.network.RetrofitClient

class AuthRepository {

    private val api: AuthApiService by lazy {
        RetrofitClient.instance.create(AuthApiService::class.java)
    }

    suspend fun login(email: String, password: String): Pair<LoginData?, String?> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "Success" && body.data != null) {
                    Pair(body.data, null)
                } else {
                    Pair(null, body?.message ?: "Error desconocido")
                }
            } else {
                // El backend devuelve JSON de error incluso en 4xx
                val errorBody = response.errorBody()?.string()
                Pair(null, parseErrorMessage(errorBody))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    suspend fun register(
        nombre: String,
        apellido: String,
        rut: String,
        email: String,
        password: String
    ): Pair<UserData?, String?> {
        return try {
            val nombreCompleto = "$nombre $apellido"
            val response = api.register(RegisterRequest(nombreCompleto, rut, email, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "Success" && body.data != null) {
                    Pair(body.data, null)
                } else {
                    Pair(null, body?.message ?: "Error desconocido")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Pair(null, parseErrorMessage(errorBody))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Error del servidor"
        return try {
            // Extrae el campo "message" del JSON de error
            val json = org.json.JSONObject(errorBody)
            json.optString("message", "Error del servidor")
        } catch (e: Exception) {
            "Error del servidor"
        }
    }
}