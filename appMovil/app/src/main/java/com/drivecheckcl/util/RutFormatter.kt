package com.drivecheckcl.util

/** Da formato "12.345.678-9" a un RUT chileno ingresado sin puntos ni guion. */
fun formatRut(input: String): String {
    val clean = input.filter { it.isDigit() || it == 'k' || it == 'K' }.take(9)
    if (clean.length <= 1) return clean
    val body      = clean.dropLast(1)
    val dv        = clean.last()
    val formatted = body.reversed().chunked(3).joinToString(".").reversed()
    return "$formatted-$dv"
}
