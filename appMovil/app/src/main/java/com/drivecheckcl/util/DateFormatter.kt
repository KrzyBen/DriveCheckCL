package com.drivecheckcl.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCALE_CL = Locale.Builder().setLanguage("es").setRegion("CL").build()

/** Convierte un id/nombre con formato "yyyyMMdd_HHmmss" a "dd/MM/yyyy HH:mm". Si falla, devuelve el original. */
fun formatTimestampId(rawId: String): String {
    return runCatching {
        val sdfIn  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val sdfOut = SimpleDateFormat("dd/MM/yyyy HH:mm", LOCALE_CL)
        sdfOut.format(sdfIn.parse(rawId)!!)
    }.getOrDefault(rawId)
}

/** Formatea milisegundos epoch como "dd MMM yyyy · HH:mm". */
fun formatEpochMillis(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", LOCALE_CL).format(Date(millis))

/** Fecha y hora actual formateada como "dd/MM/yyyy HH:mm" (para títulos autogenerados). */
fun formatNow(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", LOCALE_CL).format(Date())

/** Tiempo relativo simple ("hace 5 min", "hace 2 h", "hace 3 días") a partir de un timestamp epoch. */
fun relativeTime(millis: Long): String {
    val diff  = System.currentTimeMillis() - millis
    val mins  = diff / 60000
    val horas = diff / 3600000
    val dias  = diff / 86400000
    return when {
        mins  < 60 -> "hace $mins min"
        horas < 24 -> "hace $horas h"
        else       -> "hace $dias día${if (dias != 1L) "s" else ""}"
    }
}

/**
 * Parsea una fecha en formato Postgres (ej. "2026-06-21 16:51:36.801253+00:00")
 * a millis epoch. Si falla, devuelve el momento actual en vez de reventar.
 */
fun parseIsoFecha(fecha: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        sdf.parse(fecha.substring(0, 19))?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}
