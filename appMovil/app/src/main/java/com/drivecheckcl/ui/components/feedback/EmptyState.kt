package com.drivecheckcl.ui.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.BorderGray
import com.drivecheckcl.theme.TextHint
import com.drivecheckcl.theme.TextSecondary

/** Estado vacío genérico (ícono + texto + subtexto opcional), usado en listas de Mis
 *  informes, Mis archivos y Crear informe. */
@Composable
fun EmptyState(
    icon:       ImageVector,
    text:       String,
    subtext:    String = ""
) {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = BorderGray, modifier = Modifier.size(44.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            if (subtext.isNotEmpty()) {
                Text(subtext, fontSize = 11.sp, color = TextHint)
            }
        }
    }
}
