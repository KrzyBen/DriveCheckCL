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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.ui.theme.*

@Composable
fun HomeScreen(
    onGoToInformes:     () -> Unit,
    onGoToRanking:      () -> Unit,
    onGoToProgreso:     () -> Unit,
    onGoToConfig:       () -> Unit,
    onGoToDashcam:      () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

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
                        Text("Buenos días,", fontSize = 13.sp, color = White.copy(alpha = 0.7f))
                        Text("Juan Pérez", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(White.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = White, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta score
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
                            Text("78", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = White)
                            Text("/100", fontSize = 9.sp, color = White.copy(alpha = 0.6f))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Puntuación de conducción", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = White)
                        Text("Basado en tus últimas 5 revisiones", fontSize = 11.sp, color = White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(White.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("🏆  Puesto #24 nacional", fontSize = 11.sp, color = White)
                        }
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
                // Botón Dashcam
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
                    ActionCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.Assignment,
                        iconBgColor = ChileRed,
                        title       = "Mis informes",
                        subtitle    = "Faltas detectadas",
                        onClick     = onGoToInformes
                    )
                    ActionCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.Leaderboard,
                        iconBgColor = SuccessGreen,
                        title       = "Ranking",
                        subtitle    = "Tabla conductores",
                        onClick     = onGoToRanking
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.Timeline,
                        iconBgColor = ChileBlue,
                        title       = "Mi progreso",
                        subtitle    = "Evolución en el tiempo",
                        onClick     = onGoToProgreso
                    )
                    ActionCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.Settings,
                        iconBgColor = TextSecondary,
                        title       = "Configuración",
                        subtitle    = "Cuenta y app",
                        onClick     = onGoToConfig
                    )
                }

                // Último trayecto
                SectionLabel("ÚLTIMO TRAYECTO")
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Revisión del 12 may, 08:34", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("78/100", fontSize = 12.sp, color = WarningAmber, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        InfraccionRow(color = ChileRed,      nombre = "Exceso de velocidad",   ley = "Art. 144 — Ley 18.290")
                        InfraccionRow(color = WarningAmber,  nombre = "No respetar distancia", ley = "Art. 150 — Ley 18.290")
                        InfraccionRow(color = SuccessGreen,  nombre = "Uso de cinturón",       ley = "Correcto")
                    }
                }
            }
        }

        // ── Bottom Nav ────────────────────────────────────────────────────────
        BottomNav(
            selected  = selectedTab,
            onSelect  = { selectedTab = it },
            modifier  = Modifier.align(Alignment.BottomCenter),
            onDashcam = onGoToDashcam,
            onRanking = onGoToRanking
        )
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Medium,
        color         = TextSecondary,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun ActionCard(
    modifier:    Modifier,
    icon:        androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: androidx.compose.ui.graphics.Color,
    title:       String,
    subtitle:    String,
    onClick:     () -> Unit
) {
    Card(
        modifier  = modifier.clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = White, modifier = Modifier.size(18.dp))
            }
            Text(title,    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun InfraccionRow(color: androidx.compose.ui.graphics.Color, nombre: String, ley: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, fontSize = 12.sp, color = TextPrimary)
            Text(ley,    fontSize = 10.sp, color = TextSecondary)
        }
    }
    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
}

@Composable
fun BottomNav(
    selected:  Int,
    onSelect:  (Int) -> Unit,
    modifier:  Modifier,
    onDashcam: () -> Unit,
    onRanking: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomNavItem(icon = Icons.Default.Home,        label = "Inicio",   selected = selected == 0, onClick = { onSelect(0) })
        BottomNavItem(icon = Icons.Default.Videocam,    label = "Dashcam",  selected = selected == 1, onClick = { onSelect(1); onDashcam() })
        BottomNavItem(icon = Icons.Default.Leaderboard, label = "Ranking",  selected = selected == 2, onClick = { onSelect(2); onRanking() })
        BottomNavItem(icon = Icons.Default.Person,      label = "Perfil",   selected = selected == 3, onClick = { onSelect(3) })
    }
}

@Composable
fun BottomNavItem(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 12.dp)
    ) {
        Icon(icon, null, tint = if (selected) ChileBlue else TextHint, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 9.sp, color = if (selected) ChileBlue else TextHint)
    }
}
