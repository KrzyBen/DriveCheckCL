package com.drivecheckcl.feature.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.White

/** Chip de filtro ("Todos", "Esta semana", "Este mes") en el header de Mis informes. */
@Composable
fun ReportFilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) White else White.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = if (active) ChileBlue else White.copy(alpha = 0.8f),
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}
