package com.drivecheckcl.feature.auth

import android.content.Context
import com.drivecheckcl.util.isTokenExpired
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Punto único de manejo de sesión: guardar, recuperar, verificar validez
 * (incluye expiración del JWT) y cerrar sesión.
 *
 * "Mantener sesión iniciada" = conservar el TOKEN entre reinicios de la
 * app. Nunca se guarda ni se reutiliza la contraseña del usuario.
 */
object SessionManager {

    private const val PREFS_NAME = "drivecheckcl_prefs"
    private const val KEY_KEEP_SESSION = "keep_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"

    /**
     * Se emite cuando la sesión se invalida en tiempo de ejecución (ej. el
     * servidor devolvió 401 en medio de una operación, no solo al abrir la
     * app). MainActivity escucha esto para forzar la vuelta a Login sin
     * importar en qué pantalla esté el usuario en ese momento.
     */
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun guardarSesion(context: Context, token: String, email: String, keepSession: Boolean) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_EMAIL, email)
            .putBoolean(KEY_KEEP_SESSION, keepSession)
            .apply()
    }

    fun guardarNombreUsuario(context: Context, nombreCompleto: String) {
        prefs(context).edit().putString(KEY_USER_NAME, nombreCompleto).apply()
    }

    fun obtenerNombreUsuario(context: Context): String =
        prefs(context).getString(KEY_USER_NAME, "Usuario") ?: "Usuario"

    fun obtenerToken(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)

    /** Refleja la última elección del switch "Mantener sesión iniciada" (para prellenarlo en Login). */
    fun preferenciaMantenerSesion(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEEP_SESSION, false)

    /**
     * true solo si: existe token, el usuario eligió "mantener sesión" Y el
     * token no está vencido (verificado localmente). Si está vencido, se
     * limpia la sesión de una vez para no dejar basura en SharedPreferences.
     *
     * Se llama al arrancar la app para decidir Home vs Login.
     */
    fun verificarSesion(context: Context): Boolean {
        val keepSession = preferenciaMantenerSesion(context)
        val token = obtenerToken(context)

        if (!keepSession || token.isNullOrEmpty()) return false

        if (isTokenExpired(token)) {
            eliminarSesion(context)
            return false
        }
        return true
    }

    fun eliminarSesion(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Alias explícito para el botón de "Cerrar sesión" en Configuración. */
    fun cerrarSesion(context: Context) {
        eliminarSesion(context)
    }

    /**
     * Llamado desde la capa de red cuando cualquier respuesta llega con 401:
     * limpia la sesión y avisa a la UI para volver a Login de inmediato,
     * en vez de dejar que el usuario siga navegando con una sesión muerta.
     */
    fun notificarSesionInvalida(context: Context) {
        eliminarSesion(context)
        _sessionExpired.tryEmit(Unit)
    }
}
