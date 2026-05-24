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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.ui.theme.*

@Composable
fun MiProgresoScreen(onBack: () -> Unit) {
    var periodoActivo by remember { mutableStateOf(0) }

    // Datos de ejemplo del gráfico (score por día)
    val scores = listOf(61, 68, 74, 71, 88, 91, 78)
    val dias   = listOf("7 may", "8 may", "9 may", "10 may", "11 may", "12 may", "Hoy")

    val faltasFrecuentes = listOf(
        Triple("Exceso velocidad",      0.80f, "4x"),
        Triple("Distancia corta",       0.50f, "2x"),
        Triple("Cambio carril brusco",  0.30f, "1x")
    )

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
                Text("Mi progreso", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("7 días", "30 días", "3 meses").forEachIndexed { i, label ->
                    FilterPill(label = label, active = periodoActivo == i, onClick = { periodoActivo = i })
                }
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
            // Gráfico de barras simple
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Evolución del score", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Barras del gráfico
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        scores.forEachIndexed { index, score ->
                            val esFinal = index == scores.size - 1
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$score", fontSize = 8.sp, color = if (esFinal) ChileRed else ChileBlue, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height((score * 1.0f / 100 * 90).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (esFinal) ChileRed else ChileBlue.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Etiquetas días
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dias.forEach { dia ->
                            Text(dia, fontSize = 8.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Métricas
            SectionLabel("MÉTRICAS")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricaCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.TrendingUp,
                    iconColor = SuccessGreen,
                    valor     = "+17",
                    label     = "Puntos ganados",
                    tendencia = "↑ Esta semana",
                    tendenciaColor = SuccessGreen
                )
                MetricaCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.Warning,
                    iconColor = WarningAmber,
                    valor     = "3",
                    label     = "Faltas semana",
                    tendencia = "↓ Menos que antes",
                    tendenciaColor = SuccessGreen
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricaCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.DirectionsCar,
                    iconColor = ChileBlue,
                    valor     = "5",
                    label     = "Trayectos",
                    tendencia = "Últimos 7 días",
                    tendenciaColor = TextSecondary
                )
                MetricaCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.AccessTime,
                    iconColor = TextSecondary,
                    valor     = "1h 20m",
                    label     = "Tiempo analizado",
                    tendencia = "Esta semana",
                    tendenciaColor = TextSecondary
                )
            }

            // Faltas frecuentes
            SectionLabel("FALTAS MÁS FRECUENTES")
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val barColors = listOf(ChileRed, WarningAmber, ChileBlue)
                    faltasFrecuentes.forEachIndexed { index, (nombre, porcentaje, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(nombre, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.width(130.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(InputBackground)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(porcentaje)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(barColors[index])
                                )
                            }
                            Text(count, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricaCard(
    modifier:       Modifier,
    icon:           ImageVector,
    iconColor:      androidx.compose.ui.graphics.Color,
    valor:          String,
    label:          String,
    tendencia:      String,
    tendenciaColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            Text(valor,    fontSize = 22.sp, fontWeight = FontWeight.Medium, color = ChileBlue)
            Text(label,    fontSize = 10.sp, color = TextSecondary)
            Text(tendencia, fontSize = 10.sp, color = tendenciaColor)
        }
    }
}
