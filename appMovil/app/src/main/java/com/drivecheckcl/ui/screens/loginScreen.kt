package com.drivecheckcl.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.drivecheckcl.ui.theme.*

private const val PREFS_NAME       = "drivecheckcl_prefs"
private const val KEY_KEEP_SESSION = "keep_session"
private const val KEY_TOKEN        = "auth_token"
private const val KEY_USER_EMAIL   = "user_email"

fun saveSession(context: Context, token: String, email: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_TOKEN, token)
        .putString(KEY_USER_EMAIL, email)
        .apply()
}

fun clearSession(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .remove(KEY_TOKEN)
        .remove(KEY_USER_EMAIL)
        .apply()
}

fun isSessionActive(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_KEEP_SESSION, false) &&
            !prefs.getString(KEY_TOKEN, null).isNullOrEmpty()
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepSession     by remember { mutableStateOf(prefs.getBoolean(KEY_KEEP_SESSION, false)) }
    var isLoading       by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf<String?>(null) }

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

        // Bandas bandera chilena
        Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
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

                    DriveCheckTextField(
                        value         = email,
                        onValueChange = { email = it; errorMessage = null },
                        label         = "CORREO ELECTRÓNICO",
                        placeholder   = "usuario@correo.cl",
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                        keyboardType  = KeyboardType.Email
                    )

                    DriveCheckTextField(
                        value                = password,
                        onValueChange        = { password = it; errorMessage = null },
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
                        modifier  = Modifier.fillMaxWidth().clickable { },
                        textAlign = TextAlign.End
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked         = keepSession,
                            onCheckedChange = {
                                keepSession = it
                                prefs.edit().putBoolean(KEY_KEEP_SESSION, it).apply()
                            },
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

                    if (errorMessage != null) {
                        Text(
                            text      = errorMessage!!,
                            fontSize  = 12.sp,
                            color     = ErrorRed,
                            modifier  = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick  = {
                            if (validateLoginFields(email, password)) {
                                isLoading = true
                                // TODO: llamar AuthRepository.login(email, password)
                                onLoginSuccess()
                            } else {
                                errorMessage = "Completa todos los campos correctamente."
                            }
                            //Efecto de pruebas - Eliminar cuando se pase a producción
                            if (email == "test@drivecheckcl.cl" && password == "Test1234") {
                                if (keepSession) saveSession(context, "token_prueba", email)
                                onLoginSuccess()
                            } else {
                                errorMessage = "Credenciales incorrectas."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ChileRed),
                        enabled  = !isLoading
                    ) {
                        if (isLoading) {
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

@Composable
fun DriveCheckTextField(
    value:                String,
    onValueChange:        (String) -> Unit,
    label:                String,
    placeholder:          String,
    leadingIcon:          @Composable (() -> Unit)? = null,
    trailingIcon:         @Composable (() -> Unit)? = null,
    keyboardType:         KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextSecondary, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            placeholder          = { Text(placeholder, fontSize = 13.sp, color = TextHint) },
            leadingIcon          = leadingIcon,
            trailingIcon         = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth(),
            shape                = RoundedCornerShape(8.dp),
            colors               = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = InputBackground,
                focusedContainerColor   = InputBackground,
                unfocusedBorderColor    = InputBorder,
                focusedBorderColor      = ChileBlue,
                cursorColor             = ChileBlue
            )
        )
    }
}

private fun validateLoginFields(email: String, password: String): Boolean {
    return email.isNotBlank()
            && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            && password.length >= 6
}