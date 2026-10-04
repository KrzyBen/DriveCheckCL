package com.drivecheckcl.feature.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.local.LocalStorageManager
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.data.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ReportUiState {
    object Idle    : ReportUiState()
    object Loading : ReportUiState()
    object Success : ReportUiState()
    data class Error(val mensaje: String) : ReportUiState()
}

class ReportViewModel : ViewModel() {

    private val _informes = MutableStateFlow<List<InformeLocal>>(emptyList())
    val informes: StateFlow<List<InformeLocal>> = _informes.asStateFlow()

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun cargarInformes() {
        viewModelScope.launch {
            val (informes, error) = ReporteRepository.misReportes()
            if (informes != null) {
                _informes.value = informes
            } else if (error != null) {
                _uiState.value = ReportUiState.Error(error)
            }
        }
    }

    fun crearInforme(
        context:    Context,
        titulo:     String,
        videoPaths: List<String>,
        comentario: String,
        onSuccess:  () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            val (resultado, error) = ReporteRepository.crearReporte(context, titulo, comentario, videoPaths)

            if (resultado != null) {
                // Mover videos al directorio del reporte local
                val reporteId = resultado.id.toString()
                videoPaths.forEach { path ->
                    LocalStorageManager.moverVideoAReporte(context, path, reporteId)
                }

                cargarInformes()
                _uiState.value = ReportUiState.Success
                onSuccess()
            } else {
                _uiState.value = ReportUiState.Error(error ?: "Error al crear el informe")
            }
        }
    }

    fun descargarPdf(context: Context, reporte: InformeLocal, onSuccess: (File) -> Unit) {
        viewModelScope.launch {
            val path = reporte.pdfPath
            if (path == null) {
                _uiState.value = ReportUiState.Error("Este reporte aún no tiene PDF disponible")
                return@launch
            }
            val (archivo, error) = ReporteRepository.descargarPdf(context, path, reporte.id)
            if (archivo != null) {
                onSuccess(archivo)
            } else {
                _uiState.value = ReportUiState.Error(error ?: "Error al descargar")
            }
        }
    }

    fun eliminarInforme(id: Int) {
        viewModelScope.launch {
            val (exito, error) = ReporteRepository.eliminarReporte(id)
            if (exito) {
                cargarInformes()
            } else if (error != null) {
                _uiState.value = ReportUiState.Error(error)
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState.Idle
    }
}
