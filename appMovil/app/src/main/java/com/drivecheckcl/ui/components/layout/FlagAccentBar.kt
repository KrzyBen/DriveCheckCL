package com.drivecheckcl.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.ChileRed
import com.drivecheckcl.theme.White

/**
 * Franja de 3 colores (azul / blanco / rojo) usada como acento bajo el header en casi
 * todas las pantallas. Antes estaba copiada y pegada en Login, Register, Home,
 * MisInformes, CrearInforme, MisArchivos y Configuración.
 */
@Composable
fun FlagAccentBar(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(5.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileBlue))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(White))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(ChileRed))
    }
}
