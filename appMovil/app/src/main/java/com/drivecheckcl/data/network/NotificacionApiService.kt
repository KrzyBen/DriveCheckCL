package com.drivecheckcl.data.network

import com.drivecheckcl.data.model.ApiResponse
import com.drivecheckcl.data.model.NotificacionesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH

interface NotificacionApiService {

    @GET("api/app/notificaciones")
    suspend fun listar(): Response<ApiResponse<NotificacionesResponse>>

    @PATCH("api/app/notificaciones/marcar-leidas")
    suspend fun marcarLeidas(): Response<ApiResponse<Any>>
}
