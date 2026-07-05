package com.drivecheckcl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.data.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.drivecheckcl.data.local.LocalStorageManager

sealed class InformeUiState {
    object Idle    : InformeUiState()
    object Loading : InformeUiState()
    object Success : InformeUiState()
    data class Error(val mensaje: String) : InformeUiState()
}

class InformeViewModel : ViewModel() {

    private val _informes = MutableStateFlow<List<InformeLocal>>(emptyList())
    val informes: StateFlow<List<InformeLocal>> = _informes.asStateFlow()

    private val _uiState = MutableStateFlow<InformeUiState>(InformeUiState.Idle)
    val uiState: StateFlow<InformeUiState> = _uiState.asStateFlow()

    fun cargarInformes() {
        viewModelScope.launch {
            val (informes, error) = ReporteRepository.misReportes()
            if (informes != null) {
                _informes.value = informes
            } else if (error != null) {
                _uiState.value = InformeUiState.Error(error)
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
            _uiState.value = InformeUiState.Loading
            val (resultado, error) = ReporteRepository.crearReporte(titulo, comentario, videoPaths)

            if (resultado != null) {
                // Mover videos al directorio del reporte local
                val reporteId = resultado.id.toString()
                videoPaths.forEach { path ->
                    LocalStorageManager.moverVideoAReporte(context, path, reporteId)
                }

                cargarInformes()
                _uiState.value = InformeUiState.Success
                onSuccess()
            } else {
                _uiState.value = InformeUiState.Error(error ?: "Error al crear el informe")
            }
        }
    }

    fun eliminarInforme(id: Int) {
        viewModelScope.launch {
            val (exito, error) = ReporteRepository.eliminarReporte(id)
            if (exito) {
                cargarInformes()
            } else if (error != null) {
                _uiState.value = InformeUiState.Error(error)
            }
        }
    }

    fun resetState() {
        _uiState.value = InformeUiState.Idle
    }
}