package com.drivecheckcl.ui.viewmodel

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
    data class Success(val user: UserData, val token: String) : AuthUiState()
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
                    saveSession(context, data.token, data.user.email, keepSession)
                    saveUserName(context, data.user.nombreCompleto)
                    _uiState.value = AuthUiState.Success(data.user, data.token)
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
                saveUserName(context, data.nombreCompleto)
                _uiState.value = AuthUiState.Success(data, "")
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
}

// ── SharedPreferences helpers (centralizados aquí) ────────────────────────────

private const val PREFS_NAME = "drivecheckcl_prefs"
const val KEY_KEEP_SESSION   = "keep_session"
const val KEY_TOKEN          = "auth_token"
const val KEY_USER_EMAIL     = "user_email"
const val KEY_USER_NAME      = "user_name"

fun saveSession(context: Context, token: String, email: String, keepSession: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_TOKEN, token)
        .putString(KEY_USER_EMAIL, email)
        .putBoolean(KEY_KEEP_SESSION, keepSession)
        .apply()
}

fun saveUserName(context: Context, nombreCompleto: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_USER_NAME, nombreCompleto)
        .apply()
}

fun clearSession(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
}

fun isSessionActive(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_KEEP_SESSION, false) &&
            !prefs.getString(KEY_TOKEN, null).isNullOrEmpty()
}

fun getSavedUserName(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
}