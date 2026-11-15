package com.drivecheckcl.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.model.PerfilData
import com.drivecheckcl.data.model.UpdatePerfilRequest
import com.drivecheckcl.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PerfilUiState {
    object Idle : PerfilUiState()
    object Loading : PerfilUiState()
    data class Loaded(val perfil: PerfilData) : PerfilUiState()
    object Saving : PerfilUiState()
    data class SaveSuccess(val perfil: PerfilData) : PerfilUiState()
    data class Error(val message: String) : PerfilUiState()
}

class PerfilViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Idle)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    fun cargarPerfil() {
        viewModelScope.launch {
            _uiState.value = PerfilUiState.Loading
            val (perfil, error) = repository.obtenerPerfil()
            _uiState.value = if (perfil != null) {
                PerfilUiState.Loaded(perfil)
            } else {
                PerfilUiState.Error(error ?: "No se pudo cargar el perfil")
            }
        }
    }

    fun actualizarPerfil(
        nombreCompleto: String?,
        email: String?,
        password: String?,
        newPassword: String?
    ) {
        viewModelScope.launch {
            _uiState.value = PerfilUiState.Saving
            val (perfil, error) = repository.actualizarPerfil(
                UpdatePerfilRequest(nombreCompleto, email, password, newPassword)
            )
            _uiState.value = if (perfil != null) {
                PerfilUiState.SaveSuccess(perfil)
            } else {
                PerfilUiState.Error(error ?: "No se pudo actualizar el perfil")
            }
        }
    }

    fun resetError() {
        if (_uiState.value is PerfilUiState.Error) {
            _uiState.value = PerfilUiState.Idle
        }
    }
}
