package com.drivecheckcl.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.ui.theme.*
import com.drivecheckcl.ui.viewmodel.clearSession

@Composable
fun ConfiguracionScreen(
    onBack:   () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var soloWifi        by remember { mutableStateOf(true) }
    var analisisAuto    by remember { mutableStateOf(true) }
    var notifResultados by remember { mutableStateOf(true) }
    var notifRanking    by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmación de cierre de sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Medium) },
            text  = { Text("¿Estás seguro que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    clearSession(context)
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

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChileBlue)
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ArrowBack, null, tint = White,
                    modifier = Modifier.size(24.dp).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Configuración", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de perfil
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(White.copy(alpha = 0.12f))
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
                    Text("JP", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Juan Pérez",             fontSize = 15.sp, fontWeight = FontWeight.Medium, color = White)
                    Text("juan.perez@correo.cl",   fontSize = 12.sp, color = White.copy(alpha = 0.65f))
                }
                Icon(Icons.Default.Edit, null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }

        // Bandas
        Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
        }

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
                    onClick  = { }
                )
                HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                SettingsRowNav(
                    icon     = Icons.Default.DirectionsCar,
                    iconBg   = SuccessGreen,
                    title    = "Mi vehículo",
                    subtitle = "BCDF-42 · Toyota Corolla 2019",
                    onClick  = { }
                )
            }

            // Dashcam
            SectionLabel("DASHCAM")
            SettingsGroup {
                SettingsRowNav(
                    icon     = Icons.Default.Videocam,
                    iconBg   = ChileRed,
                    title    = "Calidad de grabación",
                    subtitle = "720p · Recomendado",
                    onClick  = { }
                )
                HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                SettingsRowToggle(
                    icon     = Icons.Default.Wifi,
                    iconBg   = WarningAmber,
                    title    = "Enviar solo con WiFi",
                    subtitle = "Ahorra datos móviles",
                    checked  = soloWifi,
                    onCheckedChange = { soloWifi = it }
                )
                HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                SettingsRowToggle(
                    icon     = Icons.Default.AutoMode,
                    iconBg   = TextSecondary,
                    title    = "Análisis automático",
                    subtitle = "Al terminar el trayecto",
                    checked  = analisisAuto,
                    onCheckedChange = { analisisAuto = it }
                )
            }

            // Notificaciones
            SectionLabel("NOTIFICACIONES")
            SettingsGroup {
                SettingsRowToggle(
                    icon     = Icons.Default.Notifications,
                    iconBg   = ChileBlue,
                    title    = "Resultados de análisis",
                    subtitle = null,
                    checked  = notifResultados,
                    onCheckedChange = { notifResultados = it }
                )
                HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                SettingsRowToggle(
                    icon     = Icons.Default.EmojiEvents,
                    iconBg   = TextSecondary,
                    title    = "Cambios en el ranking",
                    subtitle = null,
                    checked  = notifRanking,
                    onCheckedChange = { notifRanking = it }
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

// ── Componentes de settings ───────────────────────────────────────────────────

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp),
        content   = { Column(content = content) }
    )
}

@Composable
fun SettingsRowNav(
    icon:     ImageVector,
    iconBg:   Color,
    title:    String,
    subtitle: String?,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = White, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = BorderGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsRowToggle(
    icon:            ImageVector,
    iconBg:          Color,
    title:           String,
    subtitle:        String?,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = White, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = White,
                checkedTrackColor   = ChileBlue,
                uncheckedThumbColor = White,
                uncheckedTrackColor = BorderGray
            )
        )
    }
}
