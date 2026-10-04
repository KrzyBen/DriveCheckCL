package com.drivecheckcl.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.SurfaceWhite
import com.drivecheckcl.theme.TextHint

@Composable
fun BottomNavBar(
    selected:          Int,
    onSelect:          (Int) -> Unit,
    modifier:          Modifier,
    onDashcam:         () -> Unit,
    onGoToConfig:      () -> Unit,
    onGoToMisArchivos: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomNavItem(icon = Icons.Default.Home,     label = "Inicio",   selected = selected == 0, onClick = { onSelect(0) })
        BottomNavItem(icon = Icons.Default.Videocam, label = "Dashcam",  selected = selected == 1, onClick = { onSelect(1); onDashcam() })
        BottomNavItem(icon = Icons.Default.Folder,   label = "Archivos", selected = selected == 2, onClick = { onSelect(2); onGoToMisArchivos() })
        BottomNavItem(icon = Icons.Default.Person,   label = "Perfil",   selected = selected == 3, onClick = { onSelect(3); onGoToConfig() })
    }
}

@Composable
private fun BottomNavItem(
    icon:     ImageVector,
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 12.dp)
    ) {
        Icon(icon, null, tint = if (selected) ChileBlue else TextHint, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 9.sp, color = if (selected) ChileBlue else TextHint)
    }
}
