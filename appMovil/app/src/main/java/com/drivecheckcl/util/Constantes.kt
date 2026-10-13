package com.drivecheckcl.util

/**
 * Valores fijos de la app que antes estaban repetidos o sueltos en las
 * pantallas. Si cambia alguno, cámbialo aquí.
 */
object Constantes {

    /** Duración máxima de un clip de la dashcam; al llegar a ella la grabación se detiene sola. */
    const val DURACION_MAX_CLIP_SEGUNDOS = 30

    /** Máximo de videos por informe (el backend-api también lo valida). */
    const val MAX_VIDEOS_POR_REPORTE = 3
}
