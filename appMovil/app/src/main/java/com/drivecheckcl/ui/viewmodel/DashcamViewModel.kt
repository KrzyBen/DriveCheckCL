package com.drivecheckcl.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.drivecheckcl.data.DetectionResult
import com.drivecheckcl.data.local.LocalStorageManager
import com.drivecheckcl.data.model.VideoLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Estados posibles de la dashcam ───────────────────────────────────────────
sealed class DashcamState {
    object Idle      : DashcamState()
    object Recording : DashcamState()
    object Saving    : DashcamState()
    data class Saved(val rutaArchivo: String) : DashcamState()
    data class Error(val mensaje: String)     : DashcamState()
}

class DashcamViewModel : ViewModel() {

    private val _state = MutableStateFlow<DashcamState>(DashcamState.Idle)
    val state: StateFlow<DashcamState> = _state.asStateFlow()

    private val _detecciones = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detecciones: StateFlow<List<DetectionResult>> = _detecciones.asStateFlow()

    private val _tiempoGrabacion = MutableStateFlow(0)
    val tiempoGrabacion: StateFlow<Int> = _tiempoGrabacion.asStateFlow()

    // ── NUEVO: lista de videos grabados ──────────────────────────────────────
    private val _videosGrabados = MutableStateFlow<List<VideoLocal>>(emptyList())
    val videosGrabados: StateFlow<List<VideoLocal>> = _videosGrabados.asStateFlow()

    // ── Directorios ───────────────────────────────────────────────────────────

    fun getVideoDir(context: Context): File =
        LocalStorageManager.getVideosDir(context)

    fun generarNombreVideo(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "dashcam_${sdf.format(Date())}.mp4"
    }

    // ── NUEVO: refrescar lista de videos ──────────────────────────────────────
    fun refrescarVideos(context: Context) {
        _videosGrabados.value = LocalStorageManager.listarVideos(context)
    }

    // ── Eventos de grabación ──────────────────────────────────────────────────

    fun onGrabacionIniciada() {
        _state.value = DashcamState.Recording
        _tiempoGrabacion.value = 0
        _detecciones.value = emptyList()
    }

    fun onTiempoActualizado(segundos: Int) {
        _tiempoGrabacion.value = segundos
    }

    fun onDeteccion(resultado: DetectionResult) {
        _detecciones.value = _detecciones.value + resultado
    }

    fun onGrabacionGuardada(ruta: String, context: Context) {
        _state.value = DashcamState.Saved(ruta)
        refrescarVideos(context) // refrescar lista al guardar
    }

    fun onError(mensaje: String) {
        _state.value = DashcamState.Error(mensaje)
    }

    fun resetear() {
        _state.value = DashcamState.Idle
        _tiempoGrabacion.value = 0
        _detecciones.value = emptyList()
    }
}