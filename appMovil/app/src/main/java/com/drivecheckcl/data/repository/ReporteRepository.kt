package com.drivecheckcl.data.repository

import android.content.Context
import com.drivecheckcl.data.model.ConteoReportesResponse
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.data.model.InformeResponse
import com.drivecheckcl.data.network.ReporteApiService
import com.drivecheckcl.data.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ReporteRepository {

    private val api: ReporteApiService by lazy {
        RetrofitClient.instance.create(ReporteApiService::class.java)
    }

    suspend fun crearReporte(
        titulo: String,
        comentario: String,
        videoPaths: List<String>
    ): Pair<InformeResponse?, String?> {
        return try {
            val tituloBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
            val comentarioBody = comentario.toRequestBody("text/plain".toMediaTypeOrNull())

            val videoParts = videoPaths.mapIndexed { index, path ->
                val file = File(path)
                val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("videos", file.name, requestFile)
            }

            val response = api.crearReporte(tituloBody, comentarioBody, videoParts)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "Success" && body.data != null) {
                    Pair(body.data, null)
                } else {
                    Pair(null, body?.message ?: "Error desconocido")
                }
            } else {
                Pair(null, parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    suspend fun misReportes(): Pair<List<InformeLocal>?, String?> {
        return try {
            val response = api.misReportes()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "Success") {
                    val informes = (body.data ?: emptyList())
                        .mapIndexed { index, dto -> dto.toInformeLocal(numeracion = index + 1) }
                    Pair(informes, null)
                } else {
                    Pair(null, body?.message ?: "Error desconocido")
                }
            } else {
                Pair(null, parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    suspend fun contarReportes(): Pair<ConteoReportesResponse?, String?> {
        return try {
            val response = api.contarReportes()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "Success" && body.data != null) {
                    Pair(body.data, null)
                } else {
                    Pair(null, body?.message ?: "Error desconocido")
                }
            } else {
                Pair(null, parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Pair(null, "Sin conexión al servidor")
        }
    }

    suspend fun eliminarReporte(id: Int): Pair<Boolean, String?> {
        return try {
            val response = api.eliminarReporte(id)
            if (response.isSuccessful) {
                Pair(true, null)
            } else {
                Pair(false, parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Pair(false, "Sin conexión al servidor")
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

// ── Mapper: InformeResponse (servidor) → InformeLocal (UI) ────────────────────

private fun InformeResponse.toInformeLocal(numeracion: Int): InformeLocal {
    val fechaMillis = parseIsoFecha(this.createdAt)
    return InformeLocal(
        id            = this.id,
        titulo        = this.titulo,
        comentario    = this.comentario ?: "",
        videoPaths    = this.videos.map { it.path },
        fechaCreacion = fechaMillis,
        estado        = this.estado,
        numeracion    = numeracion,
        pdfDisponible = this.pdfDisponible
    )
}

private fun parseIsoFecha(fecha: String): Long {
    return try {
        // Postgres devuelve algo como "2026-06-21 16:51:36.801253+00:00"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(fecha.substring(0, 19))?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}