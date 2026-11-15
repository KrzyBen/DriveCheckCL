package com.drivecheckcl.feature.dashcam

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.drivecheckcl.data.local.LocalStorageManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

/** Un punto GPS con la hora a la que se obtuvo. */
data class PuntoGps(
    val lat: Double,
    val lon: Double,
    val precisionMetros: Float?,
    val fechaHoraMs: Long
)

/** Lo que se sabe de un clip al terminar de grabarlo. `inicio`/`fin` son null si no hubo GPS. */
data class MetadatosClip(
    val grabadoInicioMs: Long,
    val grabadoFinMs: Long,
    val inicio: PuntoGps?,
    val fin: PuntoGps?
)

/** Estado del GPS, para mostrarlo en la pantalla de la dashcam. */
enum class EstadoGps { SIN_PERMISO, EN_ESPERA, BUSCANDO, ACTIVO, NO_DISPONIBLE }

/**
 * Toma la ubicación mientras se graba un clip y guarda, junto al video, un
 * JSON con la hora y los puntos GPS de inicio y de fin.
 *
 * - Mientras graba escucha la ubicación cada ~2 s. Conserva solo el primer y
 *   el último punto; el de inicio es el punto focal del evento.
 * - Al presionar grabar también consulta la última ubicación conocida como
 *   respaldo, pero solo si es reciente (para no usar una posición vieja).
 * - Si no hay permiso o señal, el clip se graba igual y los puntos quedan
 *   en null.
 */
class ClipMetadataRecorder(context: Context) {

    private val appContext = context.applicationContext
    private val cliente = LocationServices.getFusedLocationProviderClient(appContext)

    private var inicioMs: Long = 0L
    private var primerPunto: PuntoGps? = null
    private var ultimoPunto: PuntoGps? = null
    private var escuchando = false

    var estado by mutableStateOf(
        if (tienePermiso(appContext)) EstadoGps.EN_ESPERA else EstadoGps.SIN_PERMISO
    )
        private set

    private val callback = object : LocationCallback() {
        override fun onLocationResult(resultado: LocationResult) {
            resultado.locations.forEach { registrar(it) }
        }
    }

    private fun registrar(ubicacion: Location) {
        val punto = PuntoGps(
            lat             = ubicacion.latitude,
            lon             = ubicacion.longitude,
            precisionMetros = if (ubicacion.hasAccuracy()) ubicacion.accuracy else null,
            fechaHoraMs     = ubicacion.time
        )
        if (primerPunto == null) primerPunto = punto
        ultimoPunto = punto
        estado = EstadoGps.ACTIVO
    }

    /** Llamar al presionar grabar. Reinicia los datos del clip anterior. */
    @SuppressLint("MissingPermission") // se comprueba con tienePermiso() justo antes
    fun iniciar() {
        inicioMs = System.currentTimeMillis()
        primerPunto = null
        ultimoPunto = null

        if (!tienePermiso(appContext)) {
            estado = EstadoGps.SIN_PERMISO
            return
        }

        estado = EstadoGps.BUSCANDO
        try {
            val peticion = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                INTERVALO_MS
            ).build()
            cliente.requestLocationUpdates(peticion, callback, Looper.getMainLooper())
                .addOnFailureListener { if (escuchando) estado = EstadoGps.NO_DISPONIBLE }
            escuchando = true

            // Respaldo: última ubicación conocida, solo si es reciente y todavía
            // no llegó un punto "de verdad".
            cliente.lastLocation.addOnSuccessListener { ubicacion ->
                if (ubicacion != null &&
                    escuchando &&
                    primerPunto == null &&
                    System.currentTimeMillis() - ubicacion.time <= EDAD_MAX_ULTIMA_UBICACION_MS
                ) {
                    registrar(ubicacion)
                }
            }
        } catch (_: SecurityException) {
            estado = EstadoGps.SIN_PERMISO
        }
    }

    /** Llamar cuando el clip terminó de grabarse. Deja de escuchar y devuelve los datos del clip. */
    fun detener(): MetadatosClip {
        liberar()
        return MetadatosClip(
            grabadoInicioMs = inicioMs,
            grabadoFinMs    = System.currentTimeMillis(),
            inicio          = primerPunto,
            fin             = ultimoPunto
        )
    }

    /**
     * Deja de escuchar la ubicación (por ejemplo, al salir de la pantalla)
     * sin borrar lo ya capturado, por si el clip termina de guardarse después.
     */
    fun liberar() {
        if (escuchando) {
            cliente.removeLocationUpdates(callback)
            escuchando = false
        }
        estado = if (tienePermiso(appContext)) EstadoGps.EN_ESPERA else EstadoGps.SIN_PERMISO
    }

    /** Guarda el JSON de metadatos junto al video (mismo nombre, extensión .json). */
    fun guardarJson(video: File, datos: MetadatosClip): Boolean {
        return try {
            val json = JSONObject().apply {
                put("version", VERSION_JSON)
                put("clip", video.name)
                put("grabado_inicio", iso(datos.grabadoInicioMs))
                put("grabado_fin", iso(datos.grabadoFinMs))
                put("inicio", datos.inicio?.aJson() ?: JSONObject.NULL)
                put("fin", datos.fin?.aJson() ?: JSONObject.NULL)
            }
            LocalStorageManager.getArchivoMetadatos(video).writeText(json.toString(2))
            true
        } catch (e: IOException) {
            // Sin JSON el video sigue siendo válido: el reporte se enviará sin ubicación.
            Log.w(TAG, "No se pudo guardar el JSON de metadatos de ${video.name}", e)
            false
        }
    }

    private fun PuntoGps.aJson(): JSONObject = JSONObject().apply {
        put("fecha_hora", iso(fechaHoraMs))
        put("lat", lat)
        put("lon", lon)
        put(
            "precision_m",
            precisionMetros?.let { Math.round(it * 10) / 10.0 } ?: JSONObject.NULL
        )
    }

    companion object {
        private const val TAG = "ClipMetadata"
        private const val VERSION_JSON = 1
        private const val INTERVALO_MS = 2_000L
        private const val EDAD_MAX_ULTIMA_UBICACION_MS = 15_000L

        fun tienePermiso(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        /** Fecha y hora ISO-8601 con zona horaria, ej. 2026-10-11T15:30:12-03:00. */
        private fun iso(ms: Long): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(ms)
    }
}
