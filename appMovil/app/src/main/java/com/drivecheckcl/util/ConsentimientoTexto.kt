package com.drivecheckcl.util

/**
 * Texto del consentimiento que ve el conductor.
 *
 * VERSION debe coincidir con CONSENTIMIENTO_VERSION_VIGENTE del backend
 * (server/backend-api/src/config/consentimiento.py). Si se cambia cualquier
 * texto de este archivo, se sube VERSION aquí y allá: a todos los usuarios
 * se les vuelve a pedir la decisión. La app envía SU versión al guardar, así
 * que nunca se registra un consentimiento sobre un texto que el usuario no vio.
 */
object ConsentimientoTexto {
    const val VERSION = 1

    const val TITULO = "Tus datos en DriveCheckCL"

    const val AVISO =
        "Al enviar un reporte se suben al servidor los videos que elijas y la " +
        "ubicación (GPS) y la hora de cada grabación. Esa información la revisan " +
        "el equipo validador y el análisis automático para evaluar el reporte."

    const val CASILLA =
        "Autorizo que mis videos y su ubicación se usen, anonimizados (rostros y " +
        "patentes de terceros difuminados, sin audio y sin mis datos personales), " +
        "en un estudio académico y en un dataset de conducción chilena de acceso " +
        "restringido. Puedo revocarlo cuando quiera en Ajustes."
}
