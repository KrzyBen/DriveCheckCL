package com.drivecheckcl.feature.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.EstadoInforme
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.theme.BackgroundGray
import com.drivecheckcl.theme.InputBackground
import com.drivecheckcl.theme.SuccessGreen
import com.drivecheckcl.theme.SurfaceWhite
import com.drivecheckcl.theme.TextHint
import com.drivecheckcl.theme.TextPrimary
import com.drivecheckcl.theme.TextSecondary
import com.drivecheckcl.theme.BorderGray

/** Tarjeta de un informe dentro de la lista "Mis informes". */
@Composable
fun ReportCard(
    informe:        InformeLocal,
    onActualizar:   () -> Unit,
    onDescargarPdf: () -> Unit,
    onEliminar:     () -> Unit
) {
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Número
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BackgroundGray)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        "#${informe.numeracion}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextSecondary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        informe.titulo,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary
                    )
                    Text(
                        "${informe.videoPaths.size} video${if (informe.videoPaths.size != 1) "s" else ""} adjunto${if (informe.videoPaths.size != 1) "s" else ""}",
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                }

                ReportStatusBadge(informe.estado)

                // Botón eliminar
                Icon(
                    Icons.Default.Delete, null,
                    tint     = TextHint,
                    modifier = Modifier.size(18.dp).clickable { onEliminar() }
                )
            }

            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            // Footer con acciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBackground)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Botón actualizar
                Row(
                    modifier  = Modifier.clickable { onActualizar() },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh, null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Actualizar", fontSize = 11.sp, color = TextSecondary)
                }

                // PDF, rechazado, o no disponible aún
                when {
                    informe.estado == EstadoInforme.RECHAZADO -> {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel, null,
                                tint     = Color(0xFFD52B1E),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                informe.motivoRechazo?.let { "Rechazado: $it" } ?: "Reporte rechazado",
                                fontSize   = 11.sp,
                                color      = Color(0xFFD52B1E),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    informe.pdfDisponible -> {
                        Row(
                            modifier  = Modifier.clickable { onDescargarPdf() },
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf, null,
                                tint     = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Descargar PDF",
                                fontSize   = 11.sp,
                                color      = SuccessGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        Text("PDF no disponible", fontSize = 11.sp, color = TextHint)
                    }
                }
            }
        }
    }
}
