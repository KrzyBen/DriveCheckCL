package com.drivecheckcl.feature.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.EstadoInforme
import com.drivecheckcl.data.model.etiqueta
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.SuccessGreen
import com.drivecheckcl.theme.WarningAmber

/** Colores (texto, fondo) asociados a cada estado de un informe. */
fun estadoColores(estado: EstadoInforme): Pair<Color, Color> = when (estado) {
    EstadoInforme.ENVIADO    -> Pair(ChileBlue, ChileBlue.copy(alpha = 0.1f))
    EstadoInforme.RECIBIDO   -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.12f))
    EstadoInforme.ANALIZANDO -> Pair(Color(0xFF6D28D9), Color(0xFF6D28D9).copy(alpha = 0.1f))
    EstadoInforme.APROBADO   -> Pair(SuccessGreen, SuccessGreen.copy(alpha = 0.12f))
    EstadoInforme.RECHAZADO  -> Pair(Color(0xFFD52B1E), Color(0xFFD52B1E).copy(alpha = 0.1f))
}

/** Badge con punto de color + etiqueta de estado ("ENVIADO", "APROBADO", etc.). */
@Composable
fun ReportStatusBadge(estado: EstadoInforme) {
    val (estadoColor, estadoBg) = estadoColores(estado)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(estadoBg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            "● ${estado.etiqueta()}",
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            color      = estadoColor
        )
    }
}
