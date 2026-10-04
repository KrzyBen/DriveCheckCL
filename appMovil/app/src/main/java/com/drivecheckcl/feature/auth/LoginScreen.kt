package com.drivecheckcl.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.ErrorText
import com.drivecheckcl.ui.components.input.AppTextField
import com.drivecheckcl.ui.components.layout.FlagAccentBar

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToForgotPassword: () -> Unit,
    infoMessage: String? = null,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepSession     by remember { mutableStateOf(SessionManager.preferenciaMantenerSesion(context)) }

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Reaccionar al estado del ViewModel
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.LoginSuccess -> {
                authViewModel.resetState()
                onLoginSuccess()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Header azul con logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChileBlue)
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChileRed),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "★", fontSize = 32.sp, color = White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("DriveCheckCL", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = White)
            Text("Plataforma de revisión vehicular", fontSize = 13.sp, color = White.copy(alpha = 0.65f))
        }

        FlagAccentBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            if (infoMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(infoMessage, fontSize = 13.sp, color = SuccessGreen)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text       = "Iniciar sesión",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = ChileBlue,
                        modifier   = Modifier.fillMaxWidth(),
                        textAlign  = TextAlign.Center
                    )

                    AppTextField(
                        value         = email,
                        onValueChange = { email = it; authViewModel.resetState() },
                        label         = "CORREO ELECTRÓNICO",
                        placeholder   = "usuario@correo.cl",
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                        keyboardType  = KeyboardType.Email
                    )

                    AppTextField(
                        value                = password,
                        onValueChange        = { password = it; authViewModel.resetState() },
                        label                = "CONTRASEÑA",
                        placeholder          = "••••••••",
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                        keyboardType         = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector        = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                    tint               = TextHint,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    Text(
                        text      = "¿Olvidaste tu contraseña?",
                        fontSize  = 12.sp,
                        color     = ChileBlue,
                        modifier  = Modifier.fillMaxWidth().clickable { onGoToForgotPassword() },
                        textAlign = TextAlign.End
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked         = keepSession,
                            onCheckedChange = { keepSession = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = White,
                                checkedTrackColor   = ChileBlue,
                                uncheckedThumbColor = White,
                                uncheckedTrackColor = BorderGray
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Mantener sesión iniciada", fontSize = 13.sp, color = TextSecondary)
                    }

                    // Mensaje de error del servidor
                    if (uiState is AuthUiState.Error) {
                        ErrorText((uiState as AuthUiState.Error).message)
                    }

                    Button(
                        onClick  = {
                            if (validateLoginFields(email, password)) {
                                authViewModel.login(email, password, context, keepSession)
                            } else {
                                authViewModel.setError("Revisa tu correo y contraseña e inténtalo de nuevo.")
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
                            Text("Ingresar", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("¿No tienes cuenta? ", fontSize = 13.sp, color = TextSecondary)
                Text(
                    text       = "Crear cuenta",
                    fontSize   = 13.sp,
                    color      = ChileRed,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.clickable { onGoToRegister() }
                )
            }
        }
    }
}

private fun validateLoginFields(email: String, password: String): Boolean {
    return email.isNotBlank()
            && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            && password.length >= 6
}
