package com.drivecheckcl.data

// Resultado de una detección
data class DetectionResult(
    val label: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

// Interfaz que implementará el modelo TFLite real
interface InfractionDetector {
    fun detect(frameData: ByteArray): List<DetectionResult>
    fun close()
}

// Implementación simulada para el MVP — reemplazar con TFLite después
class MockInfractionDetector : InfractionDetector {

    private var frameCount = 0

    override fun detect(frameData: ByteArray): List<DetectionResult> {
        frameCount++
        // Simula una detección cada 150 frames (~5 seg a 30fps)
        return if (frameCount % 150 == 0) {
            listOf(
                DetectionResult(
                    label      = "Exceso de velocidad",
                    confidence = 0.87f
                )
            )
        } else {
            emptyList()
        }
    }

    override fun close() {
        frameCount = 0
    }
}