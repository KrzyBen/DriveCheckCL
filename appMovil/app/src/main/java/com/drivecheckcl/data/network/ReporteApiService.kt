package com.drivecheckcl.data.network

import com.drivecheckcl.data.model.ApiResponse
import com.drivecheckcl.data.model.ConteoReportesResponse
import com.drivecheckcl.data.model.InformeResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ReporteApiService {

    @Multipart
    @POST("api/app/reporte/crear")
    suspend fun crearReporte(
        @Part("titulo") titulo: RequestBody,
        @Part("comentario") comentario: RequestBody,
        @Part videos: List<MultipartBody.Part>
    ): Response<ApiResponse<InformeResponse>>

    @GET("api/app/reporte/mis-reportes")
    suspend fun misReportes(): Response<ApiResponse<List<InformeResponse>>>

    @GET("api/app/reporte/contar")
    suspend fun contarReportes(): Response<ApiResponse<ConteoReportesResponse>>

    @GET("api/app/reporte/{id}")
    suspend fun getReporte(@Path("id") id: Int): Response<ApiResponse<InformeResponse>>

    @DELETE("api/app/reporte/{id}")
    suspend fun eliminarReporte(@Path("id") id: Int): Response<ApiResponse<Map<String, Int>>>
}