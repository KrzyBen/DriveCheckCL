package com.drivecheckcl.data.network

import com.drivecheckcl.data.model.ApiResponse
import com.drivecheckcl.data.model.ConsentimientoData
import com.drivecheckcl.data.model.ConsentimientoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ConsentimientoApiService {

    @GET("api/app/consentimiento")
    suspend fun obtener(): Response<ApiResponse<ConsentimientoData>>

    @PUT("api/app/consentimiento")
    suspend fun guardar(@Body body: ConsentimientoRequest): Response<ApiResponse<ConsentimientoData>>
}
