package com.drivecheckcl.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drivecheckcl.theme.White

/** Punto individual del indicador de pasos del registro. */
@Composable
fun StepDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) White else White.copy(alpha = 0.3f))
    )
}

/** Línea conectora entre pasos del indicador de registro. */
@Composable
fun StepLine(active: Boolean) {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(2.dp)
            .background(if (active) White else White.copy(alpha = 0.3f))
    )
}
