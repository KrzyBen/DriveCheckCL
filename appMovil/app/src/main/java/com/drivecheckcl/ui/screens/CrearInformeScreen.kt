package com.drivecheckcl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.drivecheckcl.data.model.VideoLocal
import com.drivecheckcl.ui.theme.*
import com.drivecheckcl.ui.viewmodel.DashcamViewModel
import com.drivecheckcl.ui.viewmodel.InformeUiState
import com.drivecheckcl.ui.viewmodel.InformeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CrearInformeScreen(
    onBack:    () -> Unit,
    onSuccess: () -> Unit,
    informeViewModel: InformeViewModel  = viewModel(),
    dashcamViewModel: DashcamViewModel  = viewModel()
) {
    val context    = LocalContext.current
    val videos     by dashcamViewModel.videosGrabados.collectAsStateWithLifecycle()
    val uiState    by informeViewModel.uiState.collectAsStateWithLifecycle()

    var comentario          by remember { mutableStateOf("") }
    var videosSeleccionados by remember { mutableStateOf<Set<String>>(emptySet()) }

    val tituloAuto = remember {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))
        "Reporte ${sdf.format(Date())}"
    }

    val maxVideos    = 3
    val puedeEnviar  = videosSeleccionados.isNotEmpty()
    val isLoading    = uiState is InformeUiState.Loading

    // Cargar videos disponibles al entrar
    LaunchedEffect(Unit) {
        dashcamViewModel.refrescarVideos(context)
    }

    // Navegar al éxito
    LaunchedEffect(uiState) {
        if (uiState is InformeUiState.Success) {
            informeViewModel.resetState()
            onSuccess()
        }
    }

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
                    Icons.Default.ArrowBack, null, tint = White,
                    modifier = Modifier.size(24.dp).clickable {
                        if (!isLoading) onBack()
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Crear informe",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color      = White
                )
            }
        }

        // Bandas
        Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Título autogenerado ───────────────────────────────────────────
            item {
                SectionLabel("TÍTULO")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.DriveFileRenameOutline, null,
                            tint     = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                tituloAuto,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color      = TextPrimary
                            )
                            Text(
                                "Generado automáticamente",
                                fontSize = 11.sp,
                                color    = TextHint
                            )
                        }
                    }
                }
            }

            // ── Comentario ────────────────────────────────────────────────────
            item {
                SectionLabel("COMENTARIO (OPCIONAL)")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    TextField(
                        value         = comentario,
                        onValueChange = { comentario = it },
                        placeholder   = {
                            Text(
                                "Describe el trayecto, incidentes u observaciones relevantes...",
                                fontSize = 13.sp,
                                color    = TextHint
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        colors   = TextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceWhite,
                            focusedContainerColor   = SurfaceWhite,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                            cursorColor             = ChileBlue
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            color    = TextPrimary
                        )
                    )
                }
            }

            // ── Selección de videos ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SectionLabel("VIDEOS A ADJUNTAR")
                    Text(
                        "${videosSeleccionados.size} / $maxVideos seleccionado${if (videosSeleccionados.size != 1) "s" else ""}",
                        fontSize = 11.sp,
                        color    = if (videosSeleccionados.size == maxVideos) ChileRed else TextSecondary
                    )
                }
            }

            if (videos.isEmpty()) {
                item {
                    EstadoVacio(
                        icono    = Icons.Default.VideoLibrary,
                        texto    = "No hay videos disponibles",
                        subtexto = "Graba un trayecto con la dashcam primero"
                    )
                }
            } else {
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column {
                            videos.forEachIndexed { index, video ->
                                VideoSeleccionableRow(
                                    video       = video,
                                    seleccionado = video.rutaArchivo in videosSeleccionados,
                                    habilitado   = video.rutaArchivo in videosSeleccionados ||
                                            videosSeleccionados.size < maxVideos,
                                    onToggle    = {
                                        videosSeleccionados = if (video.rutaArchivo in videosSeleccionados) {
                                            videosSeleccionados - video.rutaArchivo
                                        } else {
                                            videosSeleccionados + video.rutaArchivo
                                        }
                                    }
                                )
                                if (index < videos.size - 1) {
                                    HorizontalDivider(
                                        modifier  = Modifier.padding(start = 60.dp),
                                        color     = BorderGray,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Mínimo 1 · Máximo $maxVideos videos por informe",
                        fontSize = 10.sp,
                        color    = TextHint,
                        modifier = Modifier.fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            if (uiState is InformeUiState.Error) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChileRed.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            (uiState as InformeUiState.Error).mensaje,
                            fontSize = 12.sp,
                            color    = ChileRed
                        )
                    }
                }
            }

            // ── Botón enviar ──────────────────────────────────────────────────
            item {
                Button(
                    onClick  = {
                        informeViewModel.crearInforme(
                            context    = context,
                            videoPaths = videosSeleccionados.toList(),
                            comentario = comentario,
                            onSuccess  = onSuccess
                        )
                    },
                    enabled  = puedeEnviar && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = ChileRed,
                        disabledContainerColor = BorderGray
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(20.dp),
                            color     = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar informe", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ── Fila de video seleccionable ───────────────────────────────────────────────

@Composable
fun VideoSeleccionableRow(
    video:        VideoLocal,
    seleccionado: Boolean,
    habilitado:   Boolean,
    onToggle:     () -> Unit
) {
    val nombreLegible = remember(video.nombre) {
        video.nombre
            .removePrefix("dashcam_")
            .removeSuffix(".mp4")
            .let { raw ->
                runCatching {
                    val sdfIn  = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    val sdfOut = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es", "CL"))
                    sdfOut.format(sdfIn.parse(raw)!!)
                }.getOrDefault(video.nombre)
            }
    }

    val tiempoRelativo = remember(video.fechaGrabacion) {
        val diff = System.currentTimeMillis() - video.fechaGrabacion
        val mins  = diff / 60000
        val horas = diff / 3600000
        val dias  = diff / 86400000
        when {
            mins  < 60  -> "hace $mins min"
            horas < 24  -> "hace $horas h"
            else        -> "hace $dias día${if (dias != 1L) "s" else ""}"
        }
    }

    val tamano = remember(video.rutaArchivo) {
        val bytes = java.io.File(video.rutaArchivo).length()
        "${"%.1f".format(bytes / (1024f * 1024f))} MB"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado) { onToggle() }
            .background(
                if (seleccionado) ChileBlue.copy(alpha = 0.04f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (seleccionado) ChileBlue else androidx.compose.ui.graphics.Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (seleccionado) ChileBlue
                    else if (!habilitado) BorderGray
                    else TextSecondary,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) {
                Icon(
                    Icons.Default.Check, null,
                    tint     = White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Ícono video
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (habilitado) ChileBlue.copy(alpha = 0.1f)
                    else BorderGray.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Videocam, null,
                tint     = if (habilitado) ChileBlue else TextHint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                nombreLegible,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = if (habilitado) TextPrimary else TextHint
            )
            Text(
                tiempoRelativo,
                fontSize = 10.sp,
                color    = TextSecondary
            )
        }

        Text(
            tamano,
            fontSize = 10.sp,
            color    = TextHint
        )
    }
}