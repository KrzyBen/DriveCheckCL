package com.drivecheckcl.feature.home

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
import com.drivecheckcl.feature.home.components.NotificacionesSheet
import com.drivecheckcl.feature.home.components.QuickActionCard
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.layout.BottomNavBar
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName:          String = "Usuario",
    onGoToInformes:    () -> Unit,
    onGoToConfig:      () -> Unit,
    onGoToDashcam:     () -> Unit,
    onGoToMisArchivos: () -> Unit,
    notifViewModel:    NotificacionesViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var mostrarNotificaciones by remember { mutableStateOf(false) }

    val noLeidas       by notifViewModel.noLeidas.collectAsStateWithLifecycle()
    val notificaciones by notifViewModel.notificaciones.collectAsStateWithLifecycle()

    // Sin push real (ver decisión de alcance): esto solo avisa mientras el
    // usuario tiene la app abierta y vuelve a entrar/pasar por Home.
    LaunchedEffect(Unit) {
        if (AppPreferences.notificacionesResultadosHabilitadas(context)) {
            notifViewModel.refrescar()
        }
    }

    if (mostrarNotificaciones) {
        NotificacionesSheet(
            notificaciones = notificaciones,
            onDismiss      = { mostrarNotificaciones = false }
        )
    }

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Buenos días,"
            hour < 19 -> "Buenas tardes,"
            else      -> "Buenas noches,"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChileBlue)
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(greeting, fontSize = 13.sp, color = White.copy(alpha = 0.7f))
                        Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(White.copy(alpha = 0.15f))
                            .clickable {
                                mostrarNotificaciones = true
                                notifViewModel.marcarComoLeidas()
                            }
                            .padding(8.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (noLeidas > 0) {
                                    Badge(containerColor = ChileRed) {
                                        Text(if (noLeidas > 9) "9+" else "$noLeidas")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta score — mock hasta conectar backend de reportes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(White.copy(alpha = 0.12f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(ChileRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("--", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = White)
                            Text("/100", fontSize = 9.sp, color = White.copy(alpha = 0.6f))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Puntuación de conducción", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = White)
                        Text("Graba tu primer trayecto para ver tu puntaje", fontSize = 11.sp, color = White.copy(alpha = 0.6f))
                    }
                }
            }

            // Bandas bandera
            Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
            }

            // ── Contenido scrollable ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Botón Dashcam principal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChileBlue)
                        .clickable { onGoToDashcam() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChileRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, null, tint = White, modifier = Modifier.size(26.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Iniciar Dashcam", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = White)
                        Text("Grabar y analizar trayecto", fontSize = 12.sp, color = White.copy(alpha = 0.6f))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                }

                // Acciones
                SectionLabel("ACCIONES")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.Assignment,
                        iconBgColor = ChileRed,
                        title       = "Mis informes",
                        subtitle    = "Reportes de trayectos",
                        onClick     = onGoToInformes
                    )
                    QuickActionCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.FolderOpen,
                        iconBgColor = ChileBlue,
                        title       = "Mis archivos",
                        subtitle    = "Videos guardados",
                        onClick     = onGoToMisArchivos
                    )
                }
            }
        }

        // ── Bottom Nav ────────────────────────────────────────────────────────
        BottomNavBar(
            selected          = selectedTab,
            onSelect          = { selectedTab = it },
            modifier          = Modifier.align(Alignment.BottomCenter),
            onDashcam         = onGoToDashcam,
            onGoToConfig      = onGoToConfig,
            onGoToMisArchivos = onGoToMisArchivos
        )
    }
}
