package com.drivecheckcl.data.network

import com.drivecheckcl.data.model.ApiResponse
import com.drivecheckcl.data.model.PerfilData
import com.drivecheckcl.data.model.UpdatePerfilRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApiService {

    // Vive bajo /admin en el backend aunque es de autoservicio para
    // cualquier usuario autenticado (solo requiere authenticate_jwt, no
    // is_admin) — quedó anotado como reorganización pendiente del lado
    // del backend, no bloquea su uso.
    @GET("api/admin/perfil")
    suspend fun obtenerPerfil(): Response<ApiResponse<PerfilData>>

    @PATCH("api/admin/perfil")
    suspend fun actualizarPerfil(
        @Body request: UpdatePerfilRequest
    ): Response<ApiResponse<PerfilData>>
}
