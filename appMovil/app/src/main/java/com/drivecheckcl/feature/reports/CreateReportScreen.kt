package com.drivecheckcl.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.feature.dashcam.DashcamViewModel
import com.drivecheckcl.feature.reports.components.VideoSelectableRow
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.EmptyState
import com.drivecheckcl.ui.components.feedback.ErrorBox
import com.drivecheckcl.ui.components.layout.AppTopBar
import com.drivecheckcl.ui.components.layout.FlagAccentBar
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.formatNow

@Composable
fun CreateReportScreen(
    onBack:    () -> Unit,
    onSuccess: () -> Unit,
    videoPreseleccionado: String? = null,
    reportViewModel:  ReportViewModel  = viewModel(),
    dashcamViewModel: DashcamViewModel = viewModel()
) {
    val context = LocalContext.current
    val videos  by dashcamViewModel.videosGrabados.collectAsStateWithLifecycle()
    val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()

    var comentario          by remember { mutableStateOf("") }
    var videosSeleccionados by remember {
        mutableStateOf(videoPreseleccionado?.let { setOf(it) } ?: emptySet())
    }

    val tituloAuto = remember { "Reporte ${formatNow()}" }

    val maxVideos   = 3
    val puedeEnviar = videosSeleccionados.isNotEmpty()
    val isLoading   = uiState is ReportUiState.Loading

    // Cargar videos disponibles al entrar
    LaunchedEffect(Unit) {
        dashcamViewModel.refrescarVideos(context)
    }

    // Navegar al éxito
    LaunchedEffect(uiState) {
        if (uiState is ReportUiState.Success) {
            reportViewModel.resetState()
            onSuccess()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        AppTopBar(
            title       = "Crear informe",
            onBack      = onBack,
            backEnabled = !isLoading
        )

        FlagAccentBar()

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
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            cursorColor             = ChileBlue
                        ),
                        textStyle = TextStyle(
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
                    EmptyState(
                        icon    = Icons.Default.VideoLibrary,
                        text    = "No hay videos disponibles",
                        subtext = "Graba un trayecto con la dashcam primero"
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
                                VideoSelectableRow(
                                    video        = video,
                                    seleccionado = video.rutaArchivo in videosSeleccionados,
                                    habilitado   = video.rutaArchivo in videosSeleccionados ||
                                            videosSeleccionados.size < maxVideos,
                                    onToggle     = {
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
            if (uiState is ReportUiState.Error) {
                item {
                    ErrorBox((uiState as ReportUiState.Error).mensaje)
                }
            }

            // ── Botón enviar ──────────────────────────────────────────────────
            item {
                Button(
                    onClick  = {
                        reportViewModel.crearInforme(
                            context    = context,
                            titulo     = tituloAuto,
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
                            modifier    = Modifier.size(20.dp),
                            color       = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar informe", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
