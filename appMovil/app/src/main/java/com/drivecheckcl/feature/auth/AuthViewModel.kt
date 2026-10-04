package com.drivecheckcl.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivecheckcl.data.model.UserData
import com.drivecheckcl.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Estado de UI ──────────────────────────────────────────────────────────────

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class LoginSuccess(val user: UserData, val token: String) : AuthUiState()
    // El registro NO inicia sesión: el backend no emite token en /register
    // (decisión de alcance). El usuario se registra y debe iniciar sesión.
    object RegisterSuccess : AuthUiState()
    // "Olvidaste tu contraseña": se enviamos el código, y luego se confirma
    // con código + contraseña nueva. Son dos pasos, dos estados distintos.
    object ForgotPasswordEmailSent : AuthUiState()
    object PasswordResetSuccess : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, context: Context, keepSession: Boolean) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val (data, error) = repository.login(email, password)
            when {
                data?.token != null && data.user != null -> {
                    SessionManager.guardarSesion(context, data.token, data.user.email, keepSession)
                    SessionManager.guardarNombreUsuario(context, data.user.nombreCompleto)
                    _uiState.value = AuthUiState.LoginSuccess(data.user, data.token)
                }
                error != null -> {
                    _uiState.value = AuthUiState.Error(error)
                }
                else -> {
                    _uiState.value = AuthUiState.Error("Respuesta inesperada del servidor")
                }
            }
        }
    }

    fun register(
        nombre: String, apellido: String, rut: String,
        email: String, password: String, context: Context
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val (data, error) = repository.register(nombre, apellido, rut, email, password)
            if (data != null) {
                // No se guarda sesión aquí a propósito: el backend no
                // entrega token en /register, así que no hay nada que
                // guardar todavía. El usuario inicia sesión por su cuenta
                // en la pantalla de Login.
                _uiState.value = AuthUiState.RegisterSuccess
            } else {
                _uiState.value = AuthUiState.Error(error ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun solicitarRecuperacion(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val (success, error) = repository.solicitarRecuperacion(email)
            _uiState.value = if (success) {
                AuthUiState.ForgotPasswordEmailSent
            } else {
                AuthUiState.Error(error ?: "Error desconocido")
            }
        }
    }

    fun confirmarRecuperacion(email: String, code: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val (success, error) = repository.confirmarRecuperacion(email, code, newPassword)
            _uiState.value = if (success) {
                AuthUiState.PasswordResetSuccess
            } else {
                AuthUiState.Error(error ?: "Error desconocido")
            }
        }
    }
}
