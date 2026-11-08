package com.drivecheckcl.data.repository

import com.drivecheckcl.data.model.ConsentimientoData
import com.drivecheckcl.data.model.ConsentimientoRequest
import com.drivecheckcl.data.network.ConsentimientoApiService
import com.drivecheckcl.data.network.RetrofitClient
import com.drivecheckcl.util.ConsentimientoTexto

object ConsentimientoRepository {

    private val api: ConsentimientoApiService by lazy {
        RetrofitClient.instance.create(ConsentimientoApiService::class.java)
    }

    suspend fun obtener(): Pair<ConsentimientoData?, String?> {
        return try {
            val response = api.obtener()
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Pair(data, null)
            } else {
                Pair(null, "No se pudo consultar tu decisión de privacidad")
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    /** Guarda la decisión sobre el texto que esta versión de la app muestra. */
    suspend fun guardar(aceptoEstudio: Boolean): Pair<ConsentimientoData?, String?> {
        return try {
            val response = api.guardar(ConsentimientoRequest(aceptoEstudio, ConsentimientoTexto.VERSION))
            val data = response.body()?.data
            when {
                response.isSuccessful && data != null -> Pair(data, null)
                response.code() == 409 ->
                    Pair(null, "El texto de privacidad cambió. Actualiza la aplicación para continuar.")
                else -> Pair(null, "No se pudo guardar tu decisión. Inténtalo de nuevo.")
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor. Inténtalo de nuevo.")
        }
    }
}
