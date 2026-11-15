package com.drivecheckcl.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileRed
import com.drivecheckcl.theme.ErrorRed

/** Mensaje de error en texto plano centrado (usado en Login y Register bajo el formulario). */
@Composable
fun ErrorText(message: String) {
    Text(
        text      = message,
        fontSize  = 12.sp,
        color     = ErrorRed,
        modifier  = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

/** Mensaje de error dentro de una caja con fondo (usado en Crear informe). */
@Composable
fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ChileRed.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(message, fontSize = 12.sp, color = ChileRed)
    }
}
