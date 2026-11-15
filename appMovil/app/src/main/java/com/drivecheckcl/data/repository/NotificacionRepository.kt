package com.drivecheckcl.data.repository

import com.drivecheckcl.data.model.NotificacionesResponse
import com.drivecheckcl.data.network.NotificacionApiService
import com.drivecheckcl.data.network.RetrofitClient

object NotificacionRepository {

    private val api: NotificacionApiService by lazy {
        RetrofitClient.instance.create(NotificacionApiService::class.java)
    }

    suspend fun listar(): Pair<NotificacionesResponse?, String?> {
        return try {
            val response = api.listar()
            if (response.isSuccessful && response.body()?.data != null) {
                Pair(response.body()!!.data, null)
            } else {
                Pair(null, "No se pudieron cargar las notificaciones")
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    /** true si se marcaron correctamente. Se ignora el detalle del error: si falla,
     *  el usuario simplemente vuelve a ver el mismo contador la próxima vez. */
    suspend fun marcarLeidas(): Boolean {
        return try {
            api.marcarLeidas().isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
