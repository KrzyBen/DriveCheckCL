package com.drivecheckcl.feature.consentimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.model.ConsentimientoData
import com.drivecheckcl.data.repository.ConsentimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lo usan la pantalla posterior al login y la hoja de Ajustes. Como los
 * ViewModel de esta app se obtienen a nivel de Activity, la misma instancia
 * sobrevive entre pantallas (y entre usuarios): por eso el estado se vuelve a
 * leer del servidor cada vez que se entra a alguna de las dos.
 */
class ConsentimientoViewModel : ViewModel() {

    private val _estado = MutableStateFlow<ConsentimientoData?>(null)
    val estado: StateFlow<ConsentimientoData?> = _estado.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Consulta al servidor si hay que mostrar la pantalla de decisión.
     * Si no hay conexión o falla la consulta devuelve false: no se bloquea
     * el uso de la app, y se vuelve a preguntar en el próximo inicio.
     */
    suspend fun requiereDecision(): Boolean {
        _error.value = null
        _estado.value = null
        val (data, _) = ConsentimientoRepository.obtener()
        _estado.value = data
        return data?.requiereDecision == true
    }

    /** Para Ajustes: refresca el estado mostrado. Si falla, deja el anterior. */
    fun cargarEstado() {
        viewModelScope.launch {
            val (data, _) = ConsentimientoRepository.obtener()
            if (data != null) _estado.value = data
        }
    }

    /** [onExito] se llama solo si el servidor confirmó el guardado. */
    fun guardar(aceptoEstudio: Boolean, onExito: () -> Unit = {}) {
        if (_guardando.value) return
        _guardando.value = true
        _error.value = null
        viewModelScope.launch {
            val (data, error) = ConsentimientoRepository.guardar(aceptoEstudio)
            if (data != null) {
                _estado.value = data
                onExito()
            } else {
                _error.value = error
            }
            _guardando.value = false
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}
