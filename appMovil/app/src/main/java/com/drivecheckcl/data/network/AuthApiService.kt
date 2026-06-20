package com.drivecheckcl.data.network

import com.drivecheckcl.data.model.ApiResponse
import com.drivecheckcl.data.model.LoginData
import com.drivecheckcl.data.model.LoginRequest
import com.drivecheckcl.data.model.RegisterRequest
import com.drivecheckcl.data.model.UserData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginData>>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<UserData>>
}