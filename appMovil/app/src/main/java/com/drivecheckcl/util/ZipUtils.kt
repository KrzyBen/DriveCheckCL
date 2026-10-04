package com.drivecheckcl.util

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Comprime una lista de archivos en un único .zip. Se usa para exportar un
 * reporte completo (videos + PDF) como un solo archivo que el usuario pueda
 * compartir o guardar donde quiera, en vez de mandar cada archivo por separado.
 *
 * @return true si el .zip se generó correctamente.
 */
fun zipFiles(files: List<File>, outputZip: File): Boolean {
    return try {
        if (outputZip.exists()) outputZip.delete()

        ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->
            val buffer = ByteArray(8 * 1024)
            files.forEach { file ->
                if (file.exists()) {
                    BufferedInputStream(FileInputStream(file)).use { input ->
                        zipOut.putNextEntry(ZipEntry(file.name))
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            zipOut.write(buffer, 0, bytesRead)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
        }
        true
    } catch (e: Exception) {
        // Si algo falla a mitad de camino, no dejamos un .zip corrupto a medias.
        outputZip.delete()
        false
    }
}
