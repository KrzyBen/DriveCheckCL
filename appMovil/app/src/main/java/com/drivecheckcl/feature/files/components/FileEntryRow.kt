package com.drivecheckcl.feature.files.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.ChileRed
import com.drivecheckcl.theme.SuccessGreen
import com.drivecheckcl.theme.TextHint
import com.drivecheckcl.theme.TextPrimary
import com.drivecheckcl.theme.TextSecondary
import com.drivecheckcl.util.formatFileSize
import java.io.File

/** Fila de un archivo (video o PDF) dentro de una carpeta de reporte expandida. */
@Composable
fun FileEntryRow(archivo: File, onAbrir: () -> Unit, onEliminar: () -> Unit) {
    val esPdf     = archivo.extension == "pdf"
    val iconColor = if (esPdf) SuccessGreen else ChileBlue
    val icono     = if (esPdf) Icons.Default.PictureAsPdf else Icons.Default.Videocam

    val tamanoTexto = remember(archivo) { formatFileSize(archivo.length()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAbrir() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                archivo.name,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary
            )
            Text(
                tamanoTexto,
                fontSize = 10.sp,
                color    = TextSecondary
            )
        }

        IconButton(onClick = onEliminar, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.DeleteOutline, null, tint = ChileRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }

        Icon(
            Icons.Default.OpenInNew, null,
            tint     = TextHint,
            modifier = Modifier.size(16.dp)
        )
    }
}
