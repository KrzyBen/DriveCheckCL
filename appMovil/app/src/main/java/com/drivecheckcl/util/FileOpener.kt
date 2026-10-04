package com.drivecheckcl.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Abre un archivo local (video o PDF) con un Intent.ACTION_VIEW a través de FileProvider.
 *
 * Antes esta lógica estaba duplicada: `abrirArchivo()` en MisArchivosScreen y `abrirPdf()`
 * en MisInformesScreen hacían básicamente lo mismo, y ninguna de las dos manejaba
 * ActivityNotFoundException ni FileUriExposedException. Ahora vive en un solo lugar.
 *
 * @return true si se pudo lanzar el Intent, false si el archivo no existe, la ruta
 *         no está cubierta por file_paths.xml, o no hay ninguna app capaz de abrirlo.
 */
fun openLocalFile(context: Context, filePath: String, mimeType: String): Boolean {
    val file = File(filePath)
    if (!file.exists()) {
        Toast.makeText(context, "El archivo ya no existe", Toast.LENGTH_SHORT).show()
        return false
    }

    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        // Cubre ActivityNotFoundException (no hay app instalada para el tipo de archivo)
        // y FileUriExposedException/IllegalArgumentException (problema con FileProvider)
        Toast.makeText(context, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
        false
    }
}

fun openVideoFile(context: Context, filePath: String): Boolean =
    openLocalFile(context, filePath, "video/mp4")

fun openPdfFile(context: Context, filePath: String): Boolean =
    openLocalFile(context, filePath, "application/pdf")

/** Determina el mime type según la extensión del archivo (usado para archivos dentro de un reporte). */
fun mimeTypeFor(file: File): String =
    if (file.extension == "pdf") "application/pdf" else "video/mp4"

/**
 * Comparte un archivo local a través del selector de Android (ACTION_SEND),
 * usando el mismo FileProvider que openLocalFile. Pensado para exportar el
 * .zip de un reporte, pero sirve para cualquier archivo.
 */
fun shareFile(context: Context, filePath: String, mimeType: String): Boolean {
    val file = File(filePath)
    if (!file.exists()) {
        Toast.makeText(context, "El archivo ya no existe", Toast.LENGTH_SHORT).show()
        return false
    }

    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
        true
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo compartir el archivo", Toast.LENGTH_SHORT).show()
        false
    }
}
