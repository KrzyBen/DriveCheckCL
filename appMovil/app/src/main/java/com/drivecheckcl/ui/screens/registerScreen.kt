package com.drivecheckcl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.drivecheckcl.ui.theme.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit
) {
    var nombre           by remember { mutableStateOf("") }
    var apellido         by remember { mutableStateOf("") }
    var rut              by remember { mutableStateOf("") }
    var email            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var confirmPassword  by remember { mutableStateOf("") }
    var passwordVisible  by remember { mutableStateOf(false) }
    var confirmVisible   by remember { mutableStateOf(false) }
    var errorMessage     by remember { mutableStateOf<String?>(null) }
    var isLoading        by remember { mutableStateOf(false) }

    // Validaciones de contraseña en tiempo real
    val hasMinLength     = password.length >= 8
    val hasUppercase     = password.any { it.isUpperCase() }
    val hasNumber        = password.any { it.isDigit() }

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
                        .clickable { onGoToLogin() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 18.sp, color = White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Crear cuenta", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
                    Text("DriveCheckCL", fontSize = 12.sp, color = White.copy(alpha = 0.65f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicador de pasos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDot(active = true)
                StepLine(active = false)
                StepDot(active = false)
                StepLine(active = false)
                StepDot(active = false)
            }
        }

        // Bandas bandera
        Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
        }

        // Formulario con scroll
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card datos personales
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = ChileBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Datos personales", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ChileBlue)
                    }

                    // Nombre y Apellido en dos columnas
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DriveCheckTextField(
                                value         = nombre,
                                onValueChange = { nombre = it },
                                label         = "NOMBRE",
                                placeholder   = "Juan"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DriveCheckTextField(
                                value         = apellido,
                                onValueChange = { apellido = it },
                                label         = "APELLIDO",
                                placeholder   = "Pérez"
                            )
                        }
                    }

                    DriveCheckTextField(
                        value         = rut,
                        onValueChange = { rut = formatRut(it) },
                        label         = "RUT",
                        placeholder   = "12.345.678-9",
                        leadingIcon   = { Icon(Icons.Default.Badge, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) }
                    )

                    DriveCheckTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = "CORREO ELECTRÓNICO",
                        placeholder   = "usuario@correo.cl",
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                        keyboardType  = KeyboardType.Email
                    )
                }
            }

            // Card contraseña
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = ChileBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contraseña", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ChileBlue)
                    }

                    DriveCheckTextField(
                        value                = password,
                        onValueChange        = { password = it },
                        label                = "CONTRASEÑA",
                        placeholder          = "••••••••",
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                        keyboardType         = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector        = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint               = TextHint,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    // Pills de validación
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PasswordHintPill(text = "8+ caracteres", met = hasMinLength)
                        PasswordHintPill(text = "Mayúscula",     met = hasUppercase)
                        PasswordHintPill(text = "Número",        met = hasNumber)
                    }

                    DriveCheckTextField(
                        value                = confirmPassword,
                        onValueChange        = { confirmPassword = it },
                        label                = "CONFIRMAR CONTRASEÑA",
                        placeholder          = "••••••••",
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = ChileBlue, modifier = Modifier.size(18.dp)) },
                        keyboardType         = KeyboardType.Password,
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon         = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    imageVector        = if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint               = TextHint,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Error
            if (errorMessage != null) {
                Text(
                    text      = errorMessage!!,
                    fontSize  = 12.sp,
                    color     = ErrorRed,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Botón continuar
            Button(
                onClick  = {
                    errorMessage = validateRegisterFields(nombre, apellido, rut, email, password, confirmPassword)
                    if (errorMessage == null) {
                        isLoading = true
                        // TODO: llamar AuthRepository.register(...)
                        onRegisterSuccess()
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
                    Text("Continuar →", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                }
            }

            // Términos
            Text(
                text      = "Al registrarte aceptas los Términos de uso y la Política de privacidad",
                fontSize  = 11.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun StepDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) White else White.copy(alpha = 0.3f))
    )
}

@Composable
private fun StepLine(active: Boolean) {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(2.dp)
            .background(if (active) White else White.copy(alpha = 0.3f))
    )
}

@Composable
private fun PasswordHintPill(text: String, met: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (met) ChileBlue.copy(alpha = 0.1f) else InputBackground)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text     = text,
            fontSize = 10.sp,
            color    = if (met) ChileBlue else TextHint
        )
    }
}

// ── Formato RUT chileno ───────────────────────────────────────────────────────
private fun formatRut(input: String): String {
    val clean = input.filter { it.isDigit() || it == 'k' || it == 'K' }.take(9)
    if (clean.length <= 1) return clean
    val body    = clean.dropLast(1)
    val dv      = clean.last()
    val formatted = body.reversed().chunked(3).joinToString(".").reversed()
    return "$formatted-$dv"
}

// ── Validación ────────────────────────────────────────────────────────────────
private fun validateRegisterFields(
    nombre: String, apellido: String, rut: String,
    email: String, password: String, confirmPassword: String
): String? {
    if (nombre.isBlank() || apellido.isBlank()) return "Ingresa tu nombre y apellido."
    if (rut.isBlank()) return "Ingresa tu RUT."
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Correo inválido."
    if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres."
    if (!password.any { it.isUpperCase() }) return "La contraseña debe tener al menos una mayúscula."
    if (!password.any { it.isDigit() }) return "La contraseña debe tener al menos un número."
    if (password != confirmPassword) return "Las contraseñas no coinciden."
    return null
}