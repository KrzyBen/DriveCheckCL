package com.drivecheckcl.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.data.model.EstadoInforme
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.data.model.etiqueta
import com.drivecheckcl.ui.theme.*
import com.drivecheckcl.ui.viewmodel.InformeViewModel

@Composable
fun MisInformesScreen(
    onBack:          () -> Unit,
    onCrearInforme:  () -> Unit,
    viewModel:       InformeViewModel = viewModel()
) {
    val informes by viewModel.informes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filtroActivo by remember { mutableIntStateOf(0) }
    var informeAEliminar by remember { mutableStateOf<InformeLocal?>(null) }

    LaunchedEffect(Unit) {
        viewModel.cargarInformes()
    }

    // Contadores para resumen
    val totalEnviados   = informes.count { it.estado == EstadoInforme.ENVIADO || it.estado == EstadoInforme.RECIBIDO }
    val totalAnalizando = informes.count { it.estado == EstadoInforme.ANALIZANDO }
    val totalAprobados  = informes.count { it.estado == EstadoInforme.APROBADO }
    val totalRechazados = informes.count { it.estado == EstadoInforme.RECHAZADO }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChileBlue)
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null, tint = White,
                    modifier = Modifier.size(24.dp).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Mis informes",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color      = White
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Esta semana", "Este mes").forEachIndexed { i, label ->
                    FilterPill(
                        label   = label,
                        active  = filtroActivo == i,
                        onClick = { filtroActivo = i }
                    )
                }
            }
        }

        // Bandas
        Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (informes.isEmpty()) {
                // ── Estado vacío ──────────────────────────────────────────────
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Assignment, null,
                        tint     = BorderGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No hay informes aún",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Crea tu primer informe con un video grabado",
                        fontSize = 12.sp,
                        color    = TextHint
                    )
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Resumen
                    item {
                        Row(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResumenCard(modifier = Modifier.weight(1f), valor = "$totalEnviados", label = "ENVIADOS", color = ChileBlue)
                            ResumenCard(modifier = Modifier.weight(1f), valor = "$totalAnalizando", label = "ANALIZANDO", color = WarningAmber)
                            ResumenCard(modifier = Modifier.weight(1f), valor = "$totalAprobados", label = "APROBADOS", color = SuccessGreen)
                            ResumenCard(modifier = Modifier.weight(1f), valor = "$totalRechazados", label = "RECHAZADOS", color = Color(0xFFD52B1E))
                        }
                    }

                    item { SectionLabel("HISTORIAL") }

                    items(informes, key = { it.id }) { informe ->
                        InformeCard(
                            informe        = informe,
                            onActualizar   = { viewModel.cargarInformes() },
                            onDescargarPdf = {
                                viewModel.descargarPdf(context, informe) { archivo ->
                                    abrirPdf(context, archivo)
                                }
                            },
                            onEliminar     = { informeAEliminar = informe }
                        )
                    }
                }
            }

            // ── FAB crear informe ─────────────────────────────────────────────
            ExtendedFloatingActionButton(
                onClick          = onCrearInforme,
                modifier         = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor   = ChileRed,
                contentColor     = White,
                icon             = { Icon(Icons.Default.Add, null) },
                text             = { Text("Crear informe", fontSize = 13.sp) }
            )
        }
    }

    // ── Diálogo de confirmación de eliminación ─────────────────────────────────
    informeAEliminar?.let { informe ->
        AlertDialog(
            onDismissRequest = { informeAEliminar = null },
            title            = { Text("Eliminar informe") },
            text             = { Text("¿Seguro que quieres eliminar el informe \"${informe.titulo}\"? Esta acción no se puede deshacer.") },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.eliminarInforme(informe.id)
                    informeAEliminar = null
                }) {
                    Text("Eliminar", color = Color(0xFFD52B1E))
                }
            },
            dismissButton = {
                TextButton(onClick = { informeAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ── Utilidad: abrir el PDF descargado con un Intent ────────────────────────────

private fun abrirPdf(context: android.content.Context, archivo: java.io.File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}

// ── InformeCard ───────────────────────────────────────────────────────────────

@Composable
fun InformeCard(
    informe:        InformeLocal,
    onActualizar:   () -> Unit,
    onDescargarPdf: () -> Unit,
    onEliminar:     () -> Unit
) {
    val (estadoColor, estadoBg) = estadoColores(informe.estado)

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

                // Badge de estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(estadoBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        "● ${informe.estado.etiqueta()}",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color      = estadoColor
                    )
                }

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

// ── Colores por estado ────────────────────────────────────────────────────────

@Composable
fun estadoColores(estado: EstadoInforme): Pair<Color, Color> = when (estado) {
    EstadoInforme.ENVIADO    -> Pair(ChileBlue, ChileBlue.copy(alpha = 0.1f))
    EstadoInforme.RECIBIDO   -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.12f))
    EstadoInforme.ANALIZANDO -> Pair(Color(0xFF6D28D9), Color(0xFF6D28D9).copy(alpha = 0.1f))
    EstadoInforme.APROBADO   -> Pair(SuccessGreen, SuccessGreen.copy(alpha = 0.12f))
    EstadoInforme.RECHAZADO  -> Pair(Color(0xFFD52B1E), Color(0xFFD52B1E).copy(alpha = 0.1f))
}
// ── Componentes auxiliares (FilterPill, ResumenCard) ───────────────────────────

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
fun ResumenCard(modifier: Modifier, valor: String, label: String, color: Color) {
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