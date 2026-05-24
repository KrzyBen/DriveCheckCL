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

data class Conductor(
    val puesto:     Int,
    val iniciales:  String,
    val nombre:     String,
    val revisiones: Int,
    val score:      Int,
    val esMiPuesto: Boolean = false
)

@Composable
fun RankingScreen(onBack: () -> Unit) {
    var filtroActivo by remember { mutableStateOf(0) }

    val conductores = listOf(
        Conductor(4,  "FS", "F. Salinas", 18, 89),
        Conductor(5,  "KV", "K. Vargas",   9, 87),
        Conductor(6,  "PM", "P. Muñoz",   21, 85),
        Conductor(7,  "DT", "D. Torres",   6, 83),
        Conductor(8,  "RA", "R. Ávila",   14, 81),
        Conductor(9,  "GL", "G. Lara",    11, 79),
        Conductor(10, "MS", "M. Silva",    8, 77),
    )

    val top3 = listOf(
        Conductor(2, "MP", "M. Pinto",  15, 94),
        Conductor(1, "CR", "C. Rojas",  20, 98),
        Conductor(3, "AL", "A. Lagos",  12, 91)
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
                Text("Ranking de conductores", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Podio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2do lugar
                PodiumItem(conductor = top3[0], height = 60.dp, avatarSize = 42.dp)
                Spacer(modifier = Modifier.width(8.dp))
                // 1er lugar
                PodiumItem(conductor = top3[1], height = 80.dp, avatarSize = 50.dp, esLider = true)
                Spacer(modifier = Modifier.width(8.dp))
                // 3er lugar
                PodiumItem(conductor = top3[2], height = 44.dp, avatarSize = 42.dp)
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
            // Filtros
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Nacional", "Regional", "Amigos").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (filtroActivo == i) ChileBlue else SurfaceWhite)
                            .clickable { filtroActivo = i }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text       = label,
                            fontSize   = 12.sp,
                            color      = if (filtroActivo == i) White else TextSecondary,
                            fontWeight = if (filtroActivo == i) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            // Mi posición
            SectionLabel("TU POSICIÓN")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChileBlue)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("#24", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = White)
                    Text("/ 1.2k", fontSize = 11.sp, color = White.copy(alpha = 0.5f))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ChileRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JP", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Juan Pérez", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                    Text("12 revisiones realizadas", fontSize = 11.sp, color = White.copy(alpha = 0.6f))
                }
                Text("78", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
            }

            // Tabla
            SectionLabel("TABLA GENERAL")
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column {
                    conductores.forEachIndexed { index, conductor ->
                        RankingRow(conductor = conductor)
                        if (index < conductores.size - 1) {
                            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumItem(
    conductor:  Conductor,
    height:     androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    esLider:    Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(RoundedCornerShape(avatarSize / 2))
                .background(if (esLider) ChileRed else White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(conductor.iniciales, fontSize = if (esLider) 16.sp else 13.sp, fontWeight = FontWeight.Medium, color = White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(conductor.nombre, fontSize = 10.sp, color = White.copy(alpha = 0.8f))
        Text("${conductor.score}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = White)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(White.copy(alpha = if (esLider) 0.25f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${conductor.puesto}", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun RankingRow(conductor: Conductor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "${conductor.puesto}",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = if (conductor.puesto <= 5) ChileBlue else TextSecondary,
            modifier   = Modifier.width(24.dp)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ChileBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(conductor.iniciales, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ChileBlue)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(conductor.nombre,           fontSize = 13.sp, color = TextPrimary)
            Text("${conductor.revisiones} revisiones", fontSize = 10.sp, color = TextSecondary)
        }
        Text("${conductor.score}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ChileBlue)
    }
}
