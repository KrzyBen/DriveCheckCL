package com.drivecheckcl.feature.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.VideoLocal
import com.drivecheckcl.theme.BorderGray
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.TextHint
import com.drivecheckcl.theme.TextPrimary
import com.drivecheckcl.theme.TextSecondary
import com.drivecheckcl.theme.White
import com.drivecheckcl.util.formatFileSize
import com.drivecheckcl.util.formatTimestampId
import com.drivecheckcl.util.relativeTime
import java.io.File

/** Fila de video seleccionable dentro de Crear informe (checkbox + info + tamaño). */
@Composable
fun VideoSelectableRow(
    video:        VideoLocal,
    seleccionado: Boolean,
    habilitado:   Boolean,
    onToggle:     () -> Unit
) {
    val nombreLegible = remember(video.nombre) {
        formatTimestampId(video.nombre.removePrefix("dashcam_").removeSuffix(".mp4"))
    }

    val tiempoRelativo = remember(video.fechaGrabacion) {
        relativeTime(video.fechaGrabacion)
    }

    val tamano = remember(video.rutaArchivo) {
        formatFileSize(File(video.rutaArchivo).length())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado) { onToggle() }
            .background(
                if (seleccionado) ChileBlue.copy(alpha = 0.04f)
                else Color.Transparent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (seleccionado) ChileBlue else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (seleccionado) ChileBlue
                    else if (!habilitado) BorderGray
                    else TextSecondary,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) {
                Icon(
                    Icons.Default.Check, null,
                    tint     = White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Ícono video
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (habilitado) ChileBlue.copy(alpha = 0.1f)
                    else BorderGray.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Videocam, null,
                tint     = if (habilitado) ChileBlue else TextHint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                nombreLegible,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = if (habilitado) TextPrimary else TextHint
            )
            Text(
                tiempoRelativo,
                fontSize = 10.sp,
                color    = TextSecondary
            )
        }

        Text(
            tamano,
            fontSize = 10.sp,
            color    = TextHint
        )
    }
}
