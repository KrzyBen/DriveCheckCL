package com.drivecheckcl.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.data.local.LocalStorageManager
import com.drivecheckcl.data.local.ReporteLocal
import com.drivecheckcl.data.model.VideoLocal
import com.drivecheckcl.ui.theme.*
import com.drivecheckcl.ui.viewmodel.DashcamViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MisArchivosScreen(
    onBack: () -> Unit,
    viewModel: DashcamViewModel = viewModel()
) {
    val context = LocalContext.current
    val videos  by viewModel.videosGrabados.collectAsStateWithLifecycle()

    var reportes       by remember { mutableStateOf<List<ReporteLocal>>(emptyList()) }
    var videoAEliminar by remember { mutableStateOf<VideoLocal?>(null) }

    // Cargar ambas secciones al entrar
    LaunchedEffect(Unit) {
        viewModel.refrescarVideos(context)
        reportes = LocalStorageManager.listarReportesConArchivos(context)
    }

    // Diálogo confirmación eliminar video
    videoAEliminar?.let { video ->
        AlertDialog(
            onDismissRequest = { videoAEliminar = null },
            title = { Text("Eliminar video", fontWeight = FontWeight.Medium) },
            text  = { Text("¿Eliminar \"${video.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    LocalStorageManager.eliminarVideo(video.rutaArchivo)
                    viewModel.refrescarVideos(context)
                    videoAEliminar = null
                }) {
                    Text("Eliminar", color = ChileRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoAEliminar = null }) {
                    Text("Cancelar", color = ChileBlue)
                }
            }
        )
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
                    modifier = Modifier.size(24.dp).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Mis archivos",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color      = White,
                    modifier   = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resumen rápido
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContadorBadge(
                    icono  = Icons.Default.Videocam,
                    valor  = videos.size,
                    label  = "video${if (videos.size != 1) "s" else ""}"
                )
                ContadorBadge(
                    icono  = Icons.Default.Folder,
                    valor  = reportes.size,
                    label  = "reporte${if (reportes.size != 1) "s" else ""}"
                )
            }
        }

        // Bandas
        Row(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
        }

        // ── Contenido ─────────────────────────────────────────────────────────
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding  = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {

            // ── Sección VIDEOS ────────────────────────────────────────────────
            item {
                SectionLabel("VIDEOS")
            }

            if (videos.isEmpty()) {
                item { EstadoVacio(icono = Icons.Default.VideoLibrary, texto = "No hay videos grabados") }
            } else {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        video          = video,
                        onReproducir   = { abrirArchivo(context, video.rutaArchivo, "video/mp4") },
                        onCrearReporte = { /* placeholder futuro */ },
                        onEliminar     = { videoAEliminar = video }
                    )
                }
            }

            // ── Separador ─────────────────────────────────────────────────────
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // ── Sección REPORTES ──────────────────────────────────────────────
            item {
                SectionLabel("REPORTES")
            }

            if (reportes.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Default.FolderOff,
                        texto = "No hay reportes aún",
                        subtexto = "Los reportes aparecerán aquí al crearlos"
                    )
                }
            } else {
                items(reportes, key = { it.id }) { reporte ->
                    ReporteCard(
                        reporte = reporte,
                        onAbrirArchivo = { archivo ->
                            val mime = if (archivo.extension == "pdf") "application/pdf" else "video/mp4"
                            abrirArchivo(context, archivo.absolutePath, mime)
                        }
                    )
                }
            }
        }
    }
}

// ── ReporteCard expandible ────────────────────────────────────────────────────

@Composable
fun ReporteCard(
    reporte: ReporteLocal,
    onAbrirArchivo: (File) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    val tituloFormateado = remember(reporte.id) {
        runCatching {
            val sdfIn  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val sdfOut = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))
            "Reporte ${sdfOut.format(sdfIn.parse(reporte.id)!!)}"
        }.getOrDefault("Reporte ${reporte.id}")
    }

    val fechaFormateada = remember(reporte.fechaCreacion) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "CL"))
            .format(Date(reporte.fechaCreacion))
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
                        MiniTag(texto = "$cantVideos mp4", color = ChileBlue)
                    }
                    if (tienePdf) {
                        MiniTag(texto = "PDF", color = SuccessGreen)
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
                            ArchivoRow(
                                archivo        = archivo,
                                onAbrir        = { onAbrirArchivo(archivo) }
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
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    video:          VideoLocal,
    onReproducir:   () -> Unit,
    onCrearReporte: () -> Unit,
    onEliminar:     () -> Unit
) {
    val fechaFormateada = remember(video.fechaGrabacion) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "CL"))
            .format(Date(video.fechaGrabacion))
    }

    val nombreLegible = remember(video.nombre) {
        video.nombre
            .removePrefix("dashcam_")
            .removeSuffix(".mp4")
            .let { raw ->
                runCatching {
                    val sdfIn  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val sdfOut = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))
                    sdfOut.format(sdfIn.parse(raw)!!)
                }.getOrDefault(video.nombre)
            }
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

// ── Fila de archivo dentro del reporte ───────────────────────────────────────

@Composable
fun ArchivoRow(archivo: File, onAbrir: () -> Unit) {
    val esPdf     = archivo.extension == "pdf"
    val iconColor = if (esPdf) SuccessGreen else ChileBlue
    val icono     = if (esPdf) Icons.Default.PictureAsPdf else Icons.Default.Videocam

    val tamanoTexto = remember(archivo) {
        val bytes = archivo.length()
        when {
            bytes < 1024         -> "$bytes B"
            bytes < 1024 * 1024  -> "${"%.1f".format(bytes / 1024f)} KB"
            else                 -> "${"%.1f".format(bytes / (1024f * 1024f))} MB"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAbrir() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                archivo.name,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary
            )
            Text(
                tamanoTexto,
                fontSize = 10.sp,
                color    = TextSecondary
            )
        }

        Icon(
            Icons.Default.OpenInNew, null,
            tint     = TextHint,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
fun ContadorBadge(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    valor: Int,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(White.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icono, null, tint = White, modifier = Modifier.size(13.dp))
        Text("$valor $label", fontSize = 11.sp, color = White)
    }
}

@Composable
fun MiniTag(texto: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(texto, fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EstadoVacio(
    icono:    androidx.compose.ui.graphics.vector.ImageVector,
    texto:    String,
    subtexto: String = ""
) {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icono, null, tint = BorderGray, modifier = Modifier.size(44.dp))
            Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            if (subtexto.isNotEmpty()) {
                Text(subtexto, fontSize = 11.sp, color = TextHint)
            }
        }
    }
}

// ── Intent para abrir cualquier archivo ──────────────────────────────────────

fun abrirArchivo(context: Context, rutaArchivo: String, mimeType: String) {
    val file = File(rutaArchivo)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}

// Mantener compatibilidad con llamadas existentes desde DashcamScreen
fun reproducirVideo(context: Context, rutaArchivo: String) =
    abrirArchivo(context, rutaArchivo, "video/mp4")