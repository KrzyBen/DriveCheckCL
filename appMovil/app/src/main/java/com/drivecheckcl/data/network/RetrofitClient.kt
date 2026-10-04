package com.drivecheckcl.data.network

import android.content.Context
import com.drivecheckcl.feature.auth.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    const val BASE_URL =
        "https://ideal-cod-wpq4jxqqq6vc55vv-8000.app.github.dev/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val token = if (::appContext.isInitialized) {
            SessionManager.obtenerToken(appContext)
        } else null

        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    // Si cualquier respuesta llega con 401 (token vencido o revocado del
    // lado del servidor), se limpia la sesión local y se avisa a la UI
    // (SessionManager.sessionExpired) para volver a Login de inmediato,
    // en vez de dejar al usuario navegando con una sesión muerta.
    private val sessionInterceptor = okhttp3.Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.code == 401 && ::appContext.isInitialized) {
            SessionManager.notificarSesionInvalida(appContext)
        }
        response
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
