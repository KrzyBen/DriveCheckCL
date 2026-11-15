package com.drivecheckcl.feature.files.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.local.ReporteLocal
import com.drivecheckcl.theme.*
import com.drivecheckcl.util.formatEpochMillis
import com.drivecheckcl.util.formatTimestampId
import java.io.File

/** Tarjeta expandible de una carpeta de reporte, con sus archivos (videos + PDF). */
@Composable
fun ReportFolderCard(
    reporte:            ReporteLocal,
    onAbrirArchivo:     (File) -> Unit,
    onEliminarArchivo:  (File) -> Unit,
    onEliminarCarpeta:  () -> Unit,
    onExportar:         () -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    val tituloFormateado = remember(reporte.id) {
        "Reporte ${formatTimestampId(reporte.id)}"
    }

    val fechaFormateada = remember(reporte.fechaCreacion) {
        formatEpochMillis(reporte.fechaCreacion)
    }

    val cantVideos = reporte.archivos.count { it.extension == "mp4" }
    val tienePdf   = reporte.archivos.any  { it.extension == "pdf" }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {

            // Cabecera — click para expandir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningAmber.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder, null,
                        tint     = WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tituloFormateado,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary
                    )
                    Text(
                        fechaFormateada,
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                }

                // Badges de contenido
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cantVideos > 0) {
                        FileTypeTag(texto = "$cantVideos mp4", color = ChileBlue)
                    }
                    if (tienePdf) {
                        FileTypeTag(texto = "PDF", color = SuccessGreen)
                    }
                }

                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Contenido expandible
            AnimatedVisibility(
                visible = expandido,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

                    if (reporte.archivos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Carpeta vacía",
                                fontSize = 12.sp,
                                color    = TextHint
                            )
                        }
                    } else {
                        reporte.archivos.forEach { archivo ->
                            FileEntryRow(
                                archivo    = archivo,
                                onAbrir    = { onAbrirArchivo(archivo) },
                                onEliminar = { onEliminarArchivo(archivo) }
                            )
                            if (archivo != reporte.archivos.last()) {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(start = 54.dp),
                                    color     = BorderGray,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

                    // Acciones sobre la carpeta completa: se dejan aparte del
                    // header para no saturarlo de íconos cuando está cerrada.
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = onExportar, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, null, tint = ChileBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar", fontSize = 12.sp, color = ChileBlue)
                        }
                        VerticalDivider(modifier = Modifier.height(20.dp), color = BorderGray)
                        TextButton(onClick = onEliminarCarpeta, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = ChileRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eliminar reporte", fontSize = 12.sp, color = ChileRed)
                        }
                    }
                }
            }
        }
    }
}
