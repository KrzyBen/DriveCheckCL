package com.drivecheckcl.util

import android.util.Base64
import org.json.JSONObject

/**
 * Utilidades mínimas para leer el claim "exp" de un JWT localmente, sin
 * verificar su firma (eso lo hace siempre el servidor en cada request).
 *
 * El único propósito de esto es evitar mandar al usuario a Home con un
 * token que ya sabemos que venció, ahorrando una llamada de red que de
 * todas formas va a fallar con 401. El servidor sigue siendo la única
 * fuente de verdad real sobre la validez del token.
 */

private fun decodePayload(token: String): JSONObject? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val bytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP)
        JSONObject(String(bytes, Charsets.UTF_8))
    } catch (e: Exception) {
        null
    }
}

/** Timestamp de expiración en segundos (epoch), o null si no se pudo leer. */
fun getTokenExpirationSeconds(token: String): Long? {
    val payload = decodePayload(token) ?: return null
    return if (payload.has("exp")) payload.optLong("exp") else null
}

/**
 * true solo si podemos confirmar que el token ya venció. Si no se puede
 * leer el claim "exp" (token antiguo emitido antes de este cambio, o
 * formato inesperado), se asume válido y se deja que el servidor decida.
 */
fun isTokenExpired(token: String): Boolean {
    val exp = getTokenExpirationSeconds(token) ?: return false
    val nowSeconds = System.currentTimeMillis() / 1000
    return nowSeconds >= exp
}
