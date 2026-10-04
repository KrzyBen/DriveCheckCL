package com.drivecheckcl.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.InputBackground
import com.drivecheckcl.theme.TextHint

/** Chip que indica si un requisito de contraseña ("8+ caracteres", "Mayúscula", "Número") se cumple. */
@Composable
fun PasswordHintChip(text: String, met: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (met) ChileBlue.copy(alpha = 0.1f) else InputBackground)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text     = text,
            fontSize = 10.sp,
            color    = if (met) ChileBlue else TextHint
        )
    }
}
