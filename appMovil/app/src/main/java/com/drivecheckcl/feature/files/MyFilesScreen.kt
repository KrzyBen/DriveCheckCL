package com.drivecheckcl.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.data.local.LocalStorageManager
import com.drivecheckcl.data.local.ReporteLocal
import com.drivecheckcl.data.model.VideoLocal
import com.drivecheckcl.feature.dashcam.DashcamViewModel
import com.drivecheckcl.feature.files.components.FileCountBadge
import com.drivecheckcl.feature.files.components.ReportFolderCard
import com.drivecheckcl.feature.files.components.VideoRow
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.EmptyState
import com.drivecheckcl.ui.components.layout.AppTopBar
import com.drivecheckcl.ui.components.layout.FlagAccentBar
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.mimeTypeFor
import com.drivecheckcl.util.openLocalFile
import com.drivecheckcl.util.openVideoFile
import com.drivecheckcl.util.shareFile
import java.io.File

// Qué se está por borrar, para mostrar el diálogo de confirmación correcto
// (el mensaje y el nivel de advertencia cambian según el caso).
private sealed class Eliminacion {
    data class Video(val video: VideoLocal) : Eliminacion()
    data class ArchivoDeReporte(val reporte: ReporteLocal, val archivo: File) : Eliminacion()
    data class CarpetaCompleta(val reporte: ReporteLocal) : Eliminacion()
}

@Composable
fun MyFilesScreen(
    onBack: () -> Unit,
    onCrearReporte: (videoPath: String) -> Unit,
    viewModel: DashcamViewModel = viewModel()
) {
    val context = LocalContext.current
    val videos  by viewModel.videosGrabados.collectAsStateWithLifecycle()

    var reportes     by remember { mutableStateOf<List<ReporteLocal>>(emptyList()) }
    var pendiente    by remember { mutableStateOf<Eliminacion?>(null) }

    fun recargarReportes() {
        reportes = LocalStorageManager.listarReportesConArchivos(context)
    }

    // Cargar ambas secciones al entrar
    LaunchedEffect(Unit) {
        viewModel.refrescarVideos(context)
        recargarReportes()
    }

    // ── Diálogo de confirmación (uno solo, el texto cambia según el caso) ──────
    pendiente?.let { caso ->
        val (titulo, mensaje) = when (caso) {
            is Eliminacion.Video ->
                "Eliminar video" to "¿Eliminar \"${caso.video.nombre}\"? Esta acción no se puede deshacer."
            is Eliminacion.ArchivoDeReporte ->
                "Eliminar archivo" to "¿Eliminar \"${caso.archivo.name}\" de este reporte? Esta acción no se puede deshacer."
            is Eliminacion.CarpetaCompleta -> {
                val cantidad = caso.reporte.archivos.size
                "Eliminar reporte completo" to
                    "Esto borra el PDF y los $cantidad archivo${if (cantidad != 1) "s" else ""} de este reporte de tu " +
                    "dispositivo. El reporte ya enviado al servidor no se ve afectado. Esta acción no se puede deshacer."
            }
        }

        AlertDialog(
            onDismissRequest = { pendiente = null },
            title = { Text(titulo, fontWeight = FontWeight.Medium) },
            text  = { Text(mensaje) },
            confirmButton = {
                TextButton(onClick = {
                    when (caso) {
                        is Eliminacion.Video -> {
                            LocalStorageManager.eliminarArchivo(caso.video.rutaArchivo)
                            viewModel.refrescarVideos(context)
                        }
                        is Eliminacion.ArchivoDeReporte -> {
                            LocalStorageManager.eliminarArchivo(caso.archivo.absolutePath)
                            recargarReportes()
                        }
                        is Eliminacion.CarpetaCompleta -> {
                            LocalStorageManager.eliminarCarpetaReporte(context, caso.reporte.id)
                            recargarReportes()
                        }
                    }
                    pendiente = null
                }) {
                    Text("Eliminar", color = ChileRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendiente = null }) {
                    Text("Cancelar", color = ChileBlue)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        AppTopBar(
            title  = "Mis archivos",
            onBack = onBack,
            extraContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileCountBadge(
                        icon  = Icons.Default.Videocam,
                        valor = videos.size,
                        label = "video${if (videos.size != 1) "s" else ""}"
                    )
                    FileCountBadge(
                        icon  = Icons.Default.Folder,
                        valor = reportes.size,
                        label = "reporte${if (reportes.size != 1) "s" else ""}"
                    )
                }
            }
        )

        FlagAccentBar()

        // ── Contenido ─────────────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding      = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {

            // ── Sección VIDEOS ────────────────────────────────────────────────
            item { SectionLabel("VIDEOS") }

            if (videos.isEmpty()) {
                item { EmptyState(icon = Icons.Default.VideoLibrary, text = "No hay videos grabados") }
            } else {
                items(videos, key = { it.id }) { video ->
                    VideoRow(
                        video          = video,
                        onReproducir   = { openVideoFile(context, video.rutaArchivo) },
                        onCrearReporte = { onCrearReporte(video.rutaArchivo) },
                        onEliminar     = { pendiente = Eliminacion.Video(video) }
                    )
                }
            }

            // ── Separador ─────────────────────────────────────────────────────
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // ── Sección REPORTES ──────────────────────────────────────────────
            item { SectionLabel("REPORTES") }

            if (reportes.isEmpty()) {
                item {
                    EmptyState(
                        icon    = Icons.Default.FolderOff,
                        text    = "No hay reportes aún",
                        subtext = "Los reportes aparecerán aquí al crearlos"
                    )
                }
            } else {
                items(reportes, key = { it.id }) { reporte ->
                    ReportFolderCard(
                        reporte           = reporte,
                        onAbrirArchivo    = { archivo ->
                            openLocalFile(context, archivo.absolutePath, mimeTypeFor(archivo))
                        },
                        onEliminarArchivo = { archivo ->
                            pendiente = Eliminacion.ArchivoDeReporte(reporte, archivo)
                        },
                        onEliminarCarpeta = {
                            pendiente = Eliminacion.CarpetaCompleta(reporte)
                        },
                        onExportar = {
                            val zip = LocalStorageManager.exportarReporteComoZip(context, reporte)
                            if (zip != null) {
                                shareFile(context, zip.absolutePath, "application/zip")
                            }
                        }
                    )
                }
            }
        }
    }
}
