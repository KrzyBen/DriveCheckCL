package com.drivecheckcl.data.repository

import com.drivecheckcl.data.model.PerfilData
import com.drivecheckcl.data.model.UpdatePerfilRequest
import com.drivecheckcl.data.network.RetrofitClient
import com.drivecheckcl.data.network.UserApiService

class UserRepository {

    private val api: UserApiService by lazy {
        RetrofitClient.instance.create(UserApiService::class.java)
    }

    suspend fun obtenerPerfil(): Pair<PerfilData?, String?> {
        return try {
            val response = api.obtenerPerfil()
            if (response.isSuccessful && response.body()?.data != null) {
                Pair(response.body()!!.data, null)
            } else {
                Pair(null, parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    suspend fun actualizarPerfil(request: UpdatePerfilRequest): Pair<PerfilData?, String?> {
        return try {
            val response = api.actualizarPerfil(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Pair(response.body()!!.data, null)
            } else {
                Pair(null, parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Error del servidor"
        return try {
            val json = org.json.JSONObject(errorBody)
            json.optString("message", "Error del servidor")
        } catch (e: Exception) {
            "Error del servidor"
        }
    }
}
