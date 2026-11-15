package com.drivecheckcl.feature.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.NotificacionData
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.EmptyState
import com.drivecheckcl.util.parseIsoFecha
import com.drivecheckcl.util.relativeTime

/**
 * Lista de notificaciones dentro de la app (no hay push real, ver decisión de
 * alcance: solo se actualiza mientras el usuario tiene la app abierta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesSheet(
    notificaciones: List<NotificacionData>,
    onDismiss:      () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Notificaciones", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            if (notificaciones.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    text = "No tienes notificaciones"
                )
            } else {
                notificaciones.forEach { noti ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(noti.mensaje, fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            relativeTime(parseIsoFecha(noti.createdAt)),
                            fontSize = 11.sp,
                            color    = TextSecondary
                        )
                    }
                    if (noti != notificaciones.last()) {
                        HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
