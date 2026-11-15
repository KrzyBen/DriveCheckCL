package com.drivecheckcl.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.White

/**
 * Header azul con flecha de volver + título, usado en Mis informes, Mis archivos,
 * Crear informe y Configuración. Antes cada pantalla tenía su propia copia casi
 * idéntica de este bloque; [extraContent] permite agregar lo que cada pantalla
 * necesitaba debajo del título (chips de filtro, badges, tarjeta de perfil, etc.).
 */
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChileBlue)
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(24.dp).clickable(enabled = backEnabled) { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
        }
        if (extraContent != null) {
            Spacer(modifier = Modifier.height(14.dp))
            extraContent()
        }
    }
}
