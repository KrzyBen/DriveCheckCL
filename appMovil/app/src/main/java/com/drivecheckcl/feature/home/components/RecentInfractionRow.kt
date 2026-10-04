package com.drivecheckcl.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.BorderGray
import com.drivecheckcl.theme.TextPrimary
import com.drivecheckcl.theme.TextSecondary

// NOTA: no se encontraron llamadas a este componente en ninguna pantalla actual.
// Se mantiene movido tal cual por ahora; candidato a eliminar si se confirma que
// es código muerto (revisar antes de la defensa).
@Composable
fun RecentInfractionRow(color: Color, nombre: String, ley: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, fontSize = 12.sp, color = TextPrimary)
            Text(ley,    fontSize = 10.sp, color = TextSecondary)
        }
    }
    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
}
