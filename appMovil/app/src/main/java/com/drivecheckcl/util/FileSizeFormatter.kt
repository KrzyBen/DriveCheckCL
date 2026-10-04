package com.drivecheckcl.util

/** Formatea un tamaño en bytes como "512 B", "3.4 KB" o "12.1 MB". */
fun formatFileSize(bytes: Long): String = when {
    bytes < 1024        -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024f)} KB"
    else                 -> "${"%.1f".format(bytes / (1024f * 1024f))} MB"
}
