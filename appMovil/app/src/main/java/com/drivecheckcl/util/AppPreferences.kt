package com.drivecheckcl.util

import android.content.Context
import androidx.camera.video.Quality

/**
 * Opciones de calidad de grabación que el usuario puede elegir en
 * Configuración. El valor por defecto (HD) es el mismo que ya se usaba
 * hardcodeado en DashcamScreen antes de este cambio.
 */
enum class CalidadGrabacion(val etiqueta: String, val cameraxQuality: Quality) {
    SD(etiqueta = "480p", cameraxQuality = Quality.SD),
    HD(etiqueta = "720p · Recomendado", cameraxQuality = Quality.HD),
    FHD(etiqueta = "1080p", cameraxQuality = Quality.FHD);

    companion object {
        fun porNombre(nombre: String?): CalidadGrabacion =
            values().find { it.name == nombre } ?: HD
    }
}

/**
 * Preferencias de la app que NO son parte de la sesión de usuario (a
 * diferencia de SessionManager, que solo maneja token/login). Viven en su
 * propio archivo de SharedPreferences para no mezclar responsabilidades.
 */
object AppPreferences {
    private const val PREFS_NAME  = "drivecheckcl_app_prefs"
    private const val KEY_CALIDAD = "calidad_grabacion"
    private const val KEY_SOLO_WIFI = "solo_wifi"
    private const val KEY_NOTIF_RESULTADOS = "notif_resultados"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun obtenerCalidadGrabacion(context: Context): CalidadGrabacion =
        CalidadGrabacion.porNombre(prefs(context).getString(KEY_CALIDAD, null))

    fun guardarCalidadGrabacion(context: Context, calidad: CalidadGrabacion) {
        prefs(context).edit().putString(KEY_CALIDAD, calidad.name).apply()
    }

    /** false por defecto: no bloquear el envío de reportes a menos que el usuario lo pida explícitamente. */
    fun soloWifiHabilitado(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOLO_WIFI, false)

    fun guardarSoloWifi(context: Context, habilitado: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOLO_WIFI, habilitado).apply()
    }

    /** true por defecto: avisar cuando termine la revisión de un reporte. */
    fun notificacionesResultadosHabilitadas(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIF_RESULTADOS, true)

    fun guardarNotificacionesResultados(context: Context, habilitado: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIF_RESULTADOS, habilitado).apply()
    }
}
