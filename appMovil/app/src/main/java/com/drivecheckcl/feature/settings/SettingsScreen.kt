package com.drivecheckcl.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.data.model.PerfilData
import com.drivecheckcl.feature.auth.SessionManager
import com.drivecheckcl.feature.settings.components.SettingsGroup
import com.drivecheckcl.feature.settings.components.SettingsRowNav
import com.drivecheckcl.feature.settings.components.SettingsRowToggle
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.layout.AppTopBar
import com.drivecheckcl.ui.components.layout.FlagAccentBar
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.AppPreferences

private fun inicialesDe(nombre: String): String {
    val partes = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.size >= 2 -> "${partes[0].first()}${partes[1].first()}".uppercase()
        partes.size == 1 -> partes[0].take(2).uppercase()
        else              -> "?"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack:   () -> Unit,
    onLogout: () -> Unit,
    perfilViewModel: PerfilViewModel = viewModel()
) {
    val context = LocalContext.current

    var soloWifi        by remember { mutableStateOf(AppPreferences.soloWifiHabilitado(context)) }
    var notifResultados by remember { mutableStateOf(AppPreferences.notificacionesResultadosHabilitadas(context)) }
    var calidadActual   by remember { mutableStateOf(AppPreferences.obtenerCalidadGrabacion(context)) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfile  by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    // Snapshot estable del perfil para mostrar en pantalla, separado del
    // uiState transitorio (que además maneja Loading/Saving/Error).
    var perfil by remember { mutableStateOf<PerfilData?>(null) }
    var perfilError by remember { mutableStateOf<String?>(null) }

    val uiState by perfilViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { perfilViewModel.cargarPerfil() }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is PerfilUiState.Loaded -> {
                perfil = s.perfil
                perfilError = null
            }
            is PerfilUiState.SaveSuccess -> {
                perfil = s.perfil
                SessionManager.guardarNombreUsuario(context, s.perfil.nombreCompleto)
                showEditProfile = false
                perfilViewModel.resetError()
            }
            is PerfilUiState.Error -> {
                if (perfil == null) perfilError = s.message
            }
            else -> Unit
        }
    }

    // Diálogo de confirmación de cierre de sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Medium) },
            text  = { Text("¿Estás seguro que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    SessionManager.cerrarSesion(context)
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Cerrar sesión", color = ChileRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = ChileBlue)
                }
            }
        )
    }

    if (showEditProfile && perfil != null) {
        EditProfileSheet(
            perfilActual = perfil!!,
            uiState      = uiState,
            onGuardar    = { nombre, email, password, newPassword ->
                perfilViewModel.actualizarPerfil(nombre, email, password, newPassword)
            },
            onDismiss = {
                showEditProfile = false
                perfilViewModel.resetError()
            }
        )
    }

    if (showQualitySheet) {
        RecordingQualitySheet(
            seleccionActual = calidadActual,
            onSeleccionar   = {
                calidadActual = it
                AppPreferences.guardarCalidadGrabacion(context, it)
            },
            onDismiss = { showQualitySheet = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        AppTopBar(
            title  = "Configuración",
            onBack = onBack,
            extraContent = {
                // Tarjeta de perfil
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(White.copy(alpha = 0.12f))
                        .clickable(enabled = perfil != null) { showEditProfile = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ChileRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            perfil?.let { inicialesDe(it.nombreCompleto) } ?: "··",
                            fontSize = 16.sp, fontWeight = FontWeight.Medium, color = White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            perfil?.nombreCompleto ?: (perfilError ?: "Cargando perfil..."),
                            fontSize = 15.sp, fontWeight = FontWeight.Medium, color = White
                        )
                        if (perfil != null) {
                            Text(perfil!!.email, fontSize = 12.sp, color = White.copy(alpha = 0.65f))
                        }
                    }
                    if (perfil != null) {
                        Icon(Icons.Default.Edit, null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        )

        FlagAccentBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cuenta
            SectionLabel("CUENTA")
            SettingsGroup {
                SettingsRowNav(
                    icon     = Icons.Default.Person,
                    iconBg   = ChileBlue,
                    title    = "Editar perfil",
                    subtitle = "Nombre, correo, contraseña",
                    onClick  = { if (perfil != null) showEditProfile = true }
                )
            }

            // Dashcam
            SectionLabel("DASHCAM")
            SettingsGroup {
                SettingsRowNav(
                    icon     = Icons.Default.Videocam,
                    iconBg   = ChileRed,
                    title    = "Calidad de grabación",
                    subtitle = calidadActual.etiqueta,
                    onClick  = { showQualitySheet = true }
                )
                HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                SettingsRowToggle(
                    icon     = Icons.Default.Wifi,
                    iconBg   = WarningAmber,
                    title    = "Enviar solo con WiFi",
                    subtitle = "Ahorra datos móviles",
                    checked  = soloWifi,
                    onCheckedChange = {
                        soloWifi = it
                        AppPreferences.guardarSoloWifi(context, it)
                    }
                )
            }

            // Notificaciones
            SectionLabel("NOTIFICACIONES")
            SettingsGroup {
                SettingsRowToggle(
                    icon     = Icons.Default.Notifications,
                    iconBg   = ChileBlue,
                    title    = "Resultados de análisis",
                    subtitle = "Avisar cuando se revise un reporte",
                    checked  = notifResultados,
                    onCheckedChange = {
                        notifResultados = it
                        AppPreferences.guardarNotificacionesResultados(context, it)
                    }
                )
            }

            // Cerrar sesión
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .clickable { showLogoutDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, tint = ChileRed, modifier = Modifier.size(22.dp))
                Text("Cerrar sesión", fontSize = 13.sp, color = ChileRed, fontWeight = FontWeight.Medium)
            }

            Text(
                text     = "DriveCheckCL v1.0.0 · Hecho en Chile 🇨🇱",
                fontSize = 11.sp,
                color    = TextSecondary,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}
