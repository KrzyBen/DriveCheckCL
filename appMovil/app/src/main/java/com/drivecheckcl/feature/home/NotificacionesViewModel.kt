package com.drivecheckcl.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.model.NotificacionData
import com.drivecheckcl.data.repository.NotificacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificacionesViewModel : ViewModel() {

    private val _noLeidas = MutableStateFlow(0)
    val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    private val _notificaciones = MutableStateFlow<List<NotificacionData>>(emptyList())
    val notificaciones: StateFlow<List<NotificacionData>> = _notificaciones.asStateFlow()

    /**
     * No hay push real (ver decisión de alcance): esto se llama cada vez que
     * el usuario entra o vuelve a Home, así que solo se entera de novedades
     * mientras tiene la app abierta.
     */
    fun refrescar() {
        viewModelScope.launch {
            val (data, _) = NotificacionRepository.listar()
            if (data != null) {
                _noLeidas.value = data.noLeidas
                _notificaciones.value = data.notificaciones
            }
        }
    }

    fun marcarComoLeidas() {
        if (_noLeidas.value == 0) return
        viewModelScope.launch {
            if (NotificacionRepository.marcarLeidas()) {
                _noLeidas.value = 0
                _notificaciones.value = _notificaciones.value.map { it.copy(leida = true) }
            }
        }
    }
}
