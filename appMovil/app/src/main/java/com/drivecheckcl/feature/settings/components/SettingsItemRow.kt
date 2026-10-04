package com.drivecheckcl.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.BorderGray
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.TextPrimary
import com.drivecheckcl.theme.TextSecondary
import com.drivecheckcl.theme.White

/** Fila de configuración que navega a otra pantalla al tocarla. */
@Composable
fun SettingsRowNav(
    icon:     ImageVector,
    iconBg:   Color,
    title:    String,
    subtitle: String?,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = White, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = BorderGray, modifier = Modifier.size(20.dp))
    }
}

/** Fila de configuración con un switch (toggle) a la derecha. */
@Composable
fun SettingsRowToggle(
    icon:            ImageVector,
    iconBg:          Color,
    title:           String,
    subtitle:        String?,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = White, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = White,
                checkedTrackColor   = ChileBlue,
                uncheckedThumbColor = White,
                uncheckedTrackColor = BorderGray
            )
        )
    }
}
