package com.drivecheckcl.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.feature.auth.components.PasswordHintChip
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.ErrorText
import com.drivecheckcl.ui.components.input.AppTextField
import com.drivecheckcl.ui.components.layout.FlagAccentBar

private enum class RecuperarStep { EMAIL, CODIGO }

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onPasswordReset: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var step by remember { mutableStateOf(RecuperarStep.EMAIL) }

    var email           by remember { mutableStateOf("") }
    var code            by remember { mutableStateOf("") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError      by remember { mutableStateOf<String?>(null) }
    var infoMessage     by remember { mutableStateOf<String?>(null) }

    val hasMinLength   = newPassword.length >= 8
    val hasUppercase   = newPassword.any { it.isUpperCase() }
    val hasNumber      = newPassword.any { it.isDigit() }
    val passwordValid  = hasMinLength && hasUppercase && hasNumber
    val passwordsMatch = confirmPassword.isNotEmpty() && newPassword == confirmPassword

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.ForgotPasswordEmailSent -> {
                authViewModel.resetState()
                step = RecuperarStep.CODIGO
                infoMessage = "Te enviamos un código de 6 dígitos a $email."
            }
            is AuthUiState.PasswordResetSuccess -> {
                authViewModel.resetState()
                onPasswordReset()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Header azul
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChileBlue)
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChileRed)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 18.sp, color = White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Recuperar contraseña", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
                    Text("DriveCheckCL", fontSize = 12.sp, color = White.copy(alpha = 0.65f))
                }
            }
        }

        FlagAccentBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (step) {
                        RecuperarStep.EMAIL -> {
                            Text(
                                text      = "Ingresa el correo con el que te registraste. Te enviaremos un código para restablecer tu contraseña.",
                                fontSize  = 13.sp,
                                color     = TextSecondary
                            )

                            AppTextField(
                                value         = email,
                                onValueChange = { email = it; localError = null; authViewModel.resetState() },
                                label         = "CORREO ELECTRÓNICO",
                                placeholder   = "usuario@correo.cl",
                                leadingIcon   = { Icon(Icons.Default.Email, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                                keyboardType  = KeyboardType.Email
                            )

                            val errorToShow = localError ?: (uiState as? AuthUiState.Error)?.message
                            if (errorToShow != null) ErrorText(errorToShow)

                            Button(
                                onClick = {
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        localError = "Ingresa un correo válido."
                                    } else {
                                        localError = null
                                        authViewModel.solicitarRecuperacion(email)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = ChileRed),
                                enabled  = uiState !is AuthUiState.Loading
                            ) {
                                if (uiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Enviar código", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                                }
                            }
                        }

                        RecuperarStep.CODIGO -> {
                            if (infoMessage != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SuccessGreen.copy(alpha = 0.12f))
                                        .padding(12.dp)
                                ) {
                                    Text(infoMessage!!, fontSize = 13.sp, color = SuccessGreen)
                                }
                            }

                            AppTextField(
                                value         = code,
                                onValueChange = {
                                    code = it.filter { c -> c.isDigit() }.take(6)
                                    localError = null; authViewModel.resetState()
                                },
                                label         = "CÓDIGO DE 6 DÍGITOS",
                                placeholder   = "123456",
                                leadingIcon   = { Icon(Icons.Default.ConfirmationNumber, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                                keyboardType  = KeyboardType.Number
                            )

                            AppTextField(
                                value                = newPassword,
                                onValueChange        = { newPassword = it; localError = null },
                                label                = "CONTRASEÑA NUEVA",
                                placeholder          = "••••••••",
                                leadingIcon          = { Icon(Icons.Default.Lock, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                                keyboardType         = KeyboardType.Password,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon         = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            null, tint = TextHint, modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PasswordHintChip(text = "8+ caracteres", met = hasMinLength)
                                PasswordHintChip(text = "Mayúscula",     met = hasUppercase)
                                PasswordHintChip(text = "Número",        met = hasNumber)
                            }

                            AppTextField(
                                value                = confirmPassword,
                                onValueChange        = { confirmPassword = it; localError = null },
                                label                = "CONFIRMAR CONTRASEÑA",
                                placeholder          = "••••••••",
                                leadingIcon          = { Icon(Icons.Default.Lock, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                                keyboardType         = KeyboardType.Password,
                                visualTransformation = PasswordVisualTransformation()
                            )

                            val errorToShow = localError ?: (uiState as? AuthUiState.Error)?.message
                            if (errorToShow != null) ErrorText(errorToShow)

                            Button(
                                onClick = {
                                    localError = when {
                                        code.length != 6 -> "Ingresa el código de 6 dígitos que te enviamos."
                                        !passwordValid -> "La contraseña no cumple los requisitos."
                                        !passwordsMatch -> "Las contraseñas no coinciden."
                                        else -> null
                                    }
                                    if (localError == null) {
                                        authViewModel.confirmarRecuperacion(email, code, newPassword)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = ChileRed),
                                enabled  = uiState !is AuthUiState.Loading
                            ) {
                                if (uiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Restablecer contraseña", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                                }
                            }

                            Text(
                                text      = "¿No te llegó? Reenviar código",
                                fontSize  = 12.sp,
                                color     = ChileBlue,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        localError = null
                                        authViewModel.solicitarRecuperacion(email)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
