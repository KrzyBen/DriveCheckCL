package com.drivecheckcl.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.model.EstadoInforme
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.data.repository.InformeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun cargarInformes(context: Context) {
        _informes.value = InformeRepository.listarInformes(context)
    }

    fun crearInforme(
        context:    Context,
        videoPaths: List<String>,
        comentario: String,
        onSuccess:  () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = InformeUiState.Loading
            try {
                InformeRepository.crearInformeLocal(context, videoPaths, comentario)
                cargarInformes(context)
                _uiState.value = InformeUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = InformeUiState.Error("Error al crear el informe")
            }
        }
    }

    fun actualizarEstado(context: Context, informeId: String) {
        // Por ahora cicla al siguiente estado — reemplazar con llamada al backend
        val informe = _informes.value.find { it.id == informeId } ?: return
        val nuevoEstado = when (informe.estado) {
            EstadoInforme.ENVIADO    -> EstadoInforme.RECIBIDO
            EstadoInforme.RECIBIDO   -> EstadoInforme.ANALIZANDO
            EstadoInforme.ANALIZANDO -> EstadoInforme.VALIDANDO
            EstadoInforme.VALIDANDO  -> EstadoInforme.RESULTADOS
            EstadoInforme.RESULTADOS -> EstadoInforme.RESULTADOS
        }
        val pdfDisponible = nuevoEstado == EstadoInforme.RESULTADOS
        InformeRepository.actualizarEstado(context, informeId, nuevoEstado, pdfDisponible)
        cargarInformes(context)
    }

    fun resetState() {
        _uiState.value = InformeUiState.Idle
    }
}