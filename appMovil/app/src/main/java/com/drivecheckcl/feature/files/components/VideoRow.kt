package com.drivecheckcl.feature.files.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.VideoLocal
import com.drivecheckcl.theme.*
import com.drivecheckcl.util.formatEpochMillis
import com.drivecheckcl.util.formatTimestampId

/** Tarjeta de un video grabado en la sección "Videos" de Mis archivos. */
@Composable
fun VideoRow(
    video:          VideoLocal,
    onReproducir:   () -> Unit,
    onCrearReporte: () -> Unit,
    onEliminar:     () -> Unit
) {
    val fechaFormateada = remember(video.fechaGrabacion) { formatEpochMillis(video.fechaGrabacion) }

    val nombreLegible = remember(video.nombre) {
        formatTimestampId(video.nombre.removePrefix("dashcam_").removeSuffix(".mp4"))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReproducir() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChileBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Videocam, null, tint = ChileBlue, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombreLegible, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(fechaFormateada, fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = ChileRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBackground)
                    .clickable { onCrearReporte() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Upload, null, tint = ChileBlue, modifier = Modifier.size(16.dp))
                    Text("Crear reporte", fontSize = 12.sp, color = ChileBlue, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Default.ChevronRight, null, tint = ChileBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}
