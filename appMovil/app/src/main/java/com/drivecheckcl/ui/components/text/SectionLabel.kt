package com.drivecheckcl.ui.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.TextSecondary

/** Etiqueta pequeña de sección en mayúsculas ("ACCIONES", "VIDEOS", "HISTORIAL", etc.). */
@Composable
fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = androidx.compose.ui.text.font.FontWeight.Medium,
        color         = TextSecondary,
        letterSpacing = 0.5.sp
    )
}
