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

data class Informe(
    val fecha:    String,
    val hora:     String,
    val duracion: String,
    val score:    Int,
    val faltas:   List<Falta>
)

data class Falta(
    val nombre:   String,
    val articulo: String,
    val nivel:    NivelFalta
)

enum class NivelFalta { GRAVE, MODERADA, OK }

@Composable
fun MisInformesScreen(onBack: () -> Unit) {
    var filtroActivo by remember { mutableStateOf(0) }

    val informes = listOf(
        Informe("12 mayo 2025", "08:34", "14 min", 78, listOf(
            Falta("Exceso de velocidad",   "Art. 144 — Ley 18.290", NivelFalta.GRAVE),
            Falta("No respetar distancia", "Art. 150 — Ley 18.290", NivelFalta.MODERADA)
        )),
        Informe("10 mayo 2025", "17:51", "22 min", 91, listOf(
            Falta("Cambio de carril brusco", "Art. 155 — Ley 18.290", NivelFalta.MODERADA),
            Falta("Sin infracciones graves", "Conducción correcta",   NivelFalta.OK)
        )),
        Informe("7 mayo 2025", "07:12", "9 min", 61, listOf(
            Falta("No detuvo en señal PARE", "Art. 163 — Ley 18.290", NivelFalta.GRAVE),
            Falta("Exceso de velocidad",      "Art. 144 — Ley 18.290", NivelFalta.GRAVE)
        ))
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
                Text("Mis informes", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Esta semana", "Este mes").forEachIndexed { i, label ->
                    FilterPill(label = label, active = filtroActivo == i, onClick = { filtroActivo = i })
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard(modifier = Modifier.weight(1f), valor = "12",   label = "REVISIONES", color = ChileBlue)
                ResumenCard(modifier = Modifier.weight(1f), valor = "8",    label = "FALTAS",     color = ChileRed)
                ResumenCard(modifier = Modifier.weight(1f), valor = "78",   label = "SCORE PROM", color = WarningAmber)
            }

            SectionLabel("HISTORIAL")

            informes.forEach { informe ->
                InformeCard(informe = informe)
            }
        }
    }
}

@Composable
fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) White else White.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = if (active) ChileBlue else White.copy(alpha = 0.8f),
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun ResumenCard(modifier: Modifier, valor: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = color)
            Text(label, fontSize = 9.sp,  color = TextSecondary, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
fun InformeCard(informe: Informe) {
    val scoreColor = when {
        informe.score >= 85 -> SuccessGreen
        informe.score >= 70 -> WarningAmber
        else                -> ChileRed
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(informe.fecha, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("${informe.hora} · ${informe.duracion} de trayecto", fontSize = 11.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scoreColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${informe.score}/100", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = scoreColor)
                }
            }

            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            // Faltas
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                informe.faltas.forEach { falta ->
                    val dotColor = when (falta.nivel) {
                        NivelFalta.GRAVE    -> ChileRed
                        NivelFalta.MODERADA -> WarningAmber
                        NivelFalta.OK       -> SuccessGreen
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(falta.nombre,   fontSize = 12.sp, color = TextPrimary)
                            Text(falta.articulo, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBackground)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ver informe completo", fontSize = 12.sp, color = ChileBlue, fontWeight = FontWeight.Medium)
                Icon(Icons.Default.ChevronRight, null, tint = ChileBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}
