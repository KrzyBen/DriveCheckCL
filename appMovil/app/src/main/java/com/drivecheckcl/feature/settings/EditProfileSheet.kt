package com.drivecheckcl.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.PerfilData
import com.drivecheckcl.feature.auth.components.PasswordHintChip
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.ErrorText
import com.drivecheckcl.ui.components.input.AppTextField

/** Bottom sheet de "Editar perfil": nombre, correo y, opcionalmente, cambio de contraseña. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    perfilActual: PerfilData,
    uiState:      PerfilUiState,
    onGuardar:    (nombreCompleto: String?, email: String?, password: String?, newPassword: String?) -> Unit,
    onDismiss:    () -> Unit
) {
    var nombre by remember { mutableStateOf(perfilActual.nombreCompleto) }
    var email  by remember { mutableStateOf(perfilActual.email) }

    var cambiarPassword   by remember { mutableStateOf(false) }
    var passwordActual    by remember { mutableStateOf("") }
    var passwordNueva     by remember { mutableStateOf("") }
    var passwordConfirmar by remember { mutableStateOf("") }
    var localError        by remember { mutableStateOf<String?>(null) }

    val hasMinLength    = passwordNueva.length >= 8
    val hasUppercase    = passwordNueva.any { it.isUpperCase() }
    val hasNumber       = passwordNueva.any { it.isDigit() }
    val passwordValido  = !cambiarPassword || (hasMinLength && hasUppercase && hasNumber)
    val passwordsMatch  = !cambiarPassword || (passwordConfirmar == passwordNueva)

    val guardando = uiState is PerfilUiState.Saving

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Editar perfil", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

            AppTextField(
                value         = nombre,
                onValueChange = { nombre = it; localError = null },
                label         = "NOMBRE COMPLETO",
                placeholder   = "Tu nombre completo"
            )

            AppTextField(
                value         = email,
                onValueChange = { email = it; localError = null },
                label         = "CORREO ELECTRÓNICO",
                placeholder   = "usuario@correo.cl",
                keyboardType  = KeyboardType.Email
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { cambiarPassword = !cambiarPassword },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = cambiarPassword, onCheckedChange = { cambiarPassword = it })
                Text("Cambiar contraseña", fontSize = 13.sp, color = TextPrimary)
            }

            if (cambiarPassword) {
                AppTextField(
                    value                = passwordActual,
                    onValueChange        = { passwordActual = it; localError = null },
                    label                = "CONTRASEÑA ACTUAL",
                    placeholder          = "••••••••",
                    visualTransformation = PasswordVisualTransformation()
                )
                AppTextField(
                    value                = passwordNueva,
                    onValueChange        = { passwordNueva = it; localError = null },
                    label                = "CONTRASEÑA NUEVA",
                    placeholder          = "••••••••",
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PasswordHintChip(text = "8+ caracteres", met = hasMinLength)
                    PasswordHintChip(text = "Mayúscula",     met = hasUppercase)
                    PasswordHintChip(text = "Número",        met = hasNumber)
                }
                AppTextField(
                    value                = passwordConfirmar,
                    onValueChange        = { passwordConfirmar = it; localError = null },
                    label                = "CONFIRMAR CONTRASEÑA NUEVA",
                    placeholder          = "••••••••",
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            val errorToShow = localError ?: (uiState as? PerfilUiState.Error)?.message
            if (errorToShow != null) ErrorText(errorToShow)

            Button(
                onClick = {
                    localError = when {
                        nombre.isBlank() -> "El nombre no puede estar vacío."
                        email.isBlank()  -> "El correo no puede estar vacío."
                        cambiarPassword && passwordActual.isBlank() -> "Ingresa tu contraseña actual."
                        cambiarPassword && !passwordValido -> "La contraseña nueva no cumple los requisitos."
                        cambiarPassword && !passwordsMatch -> "Las contraseñas nuevas no coinciden."
                        else -> null
                    }
                    if (localError == null) {
                        onGuardar(
                            nombre.takeIf { it != perfilActual.nombreCompleto },
                            email.takeIf { it != perfilActual.email },
                            if (cambiarPassword) passwordActual else null,
                            if (cambiarPassword) passwordNueva else null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ChileRed),
                enabled  = !guardando
            ) {
                if (guardando) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar cambios", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                }
            }
        }
    }
}
