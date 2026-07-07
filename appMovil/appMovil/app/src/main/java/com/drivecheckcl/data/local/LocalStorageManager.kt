package com.drivecheckcl.data.local

import android.content.Context
import com.drivecheckcl.data.model.VideoLocal
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Environment

// ── Modelo de carpeta de reporte local ───────────────────────────────────────
data class ReporteLocal(
    val id: String,            // "20250612_143022"
    val nombreCarpeta: String, // "reporte_20250612_143022"
    val fechaCreacion: Long,
    val archivos: List<File>   // .mp4 y .pdf dentro de la carpeta
)

object LocalStorageManager {

    private const val DIR_VIDEOS   = "videos"
    private const val DIR_REPORTES = "reportes"

    // ── Inicialización ────────────────────────────────────────────────────────

    fun inicializarEstructura(context: Context) {
        getVideosDir(context).mkdirs()
        getReportesDir(context).mkdirs()
    }

    // ── Directorios base ──────────────────────────────────────────────────────

    private fun getRootDir(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "DriveCheckCL"
        )
    fun getVideosDir(context: Context): File =
        File(getRootDir(), DIR_VIDEOS)

    fun getReportesDir(context: Context): File =
        File(getRootDir(), DIR_REPORTES)

    fun getReporteDir(context: Context, reporteId: String): File =
        File(getRootDir(), "reportes/reporte_$reporteId")

    // ── Videos ───────────────────────────────────────────────────────────────

    fun listarVideos(context: Context): List<VideoLocal> {
        val dir = getVideosDir(context)
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                VideoLocal(
                    id               = file.nameWithoutExtension,
                    nombre           = file.name,
                    rutaArchivo      = file.absolutePath,
                    duracionSegundos = 0,
                    fechaGrabacion   = file.lastModified(),
                    subido           = false
                )
            } ?: emptyList()
    }

    fun eliminarVideo(rutaArchivo: String): Boolean =
        File(rutaArchivo).takeIf { it.exists() }?.delete() ?: false

    // ── Reportes ──────────────────────────────────────────────────────────────

    fun generarIdReporte(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun crearCarpetaReporte(context: Context, reporteId: String): File {
        val dir = getReporteDir(context, reporteId)
        dir.mkdirs()
        return dir
    }

    fun getPdfReporte(context: Context, reporteId: Int): File =
        File(getReporteDir(context, reporteId.toString()), "informe_$reporteId.pdf")

    fun moverVideoAReporte(
        context:   Context,
        videoPath: String,
        reporteId: String
    ): String? {
        val origen = File(videoPath)
        if (!origen.exists()) return null
        val destDir = crearCarpetaReporte(context, reporteId)
        val destino = File(destDir, origen.name)
        return if (origen.renameTo(destino)) destino.absolutePath else null
    }

    // ── NUEVO: listar reportes con sus archivos ───────────────────────────────

    fun listarReportesConArchivos(context: Context): List<ReporteLocal> {
        val dir = getReportesDir(context)
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.map { carpeta ->
                val archivos = carpeta
                    .listFiles { f -> f.extension == "mp4" || f.extension == "pdf" }
                    ?.sortedBy { it.name }
                    ?: emptyList()

                ReporteLocal(
                    id            = carpeta.name.removePrefix("reporte_"),
                    nombreCarpeta = carpeta.name,
                    fechaCreacion = carpeta.lastModified(),
                    archivos      = archivos
                )
            } ?: emptyList()
    }

    fun listarVideosDeReporte(context: Context, reporteId: String): List<File> {
        val dir = getReporteDir(context, reporteId)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.extension == "mp4" }?.toList() ?: emptyList()
    }
}