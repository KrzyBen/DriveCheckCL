package com.drivecheckcl.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.*
import com.drivecheckcl.util.CalidadGrabacion

/** Bottom sheet simple de selección única para la calidad de grabación del dashcam. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingQualitySheet(
    seleccionActual: CalidadGrabacion,
    onSeleccionar:   (CalidadGrabacion) -> Unit,
    onDismiss:       () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Calidad de grabación", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Mayor calidad ocupa más espacio de almacenamiento por video grabado.",
                fontSize = 12.sp,
                color    = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            CalidadGrabacion.values().forEach { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeleccionar(opcion); onDismiss() }
                        .padding(vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = opcion == seleccionActual,
                        onClick  = { onSeleccionar(opcion); onDismiss() },
                        colors   = RadioButtonDefaults.colors(selectedColor = ChileBlue)
                    )
                    Text(opcion.etiqueta, fontSize = 14.sp, color = TextPrimary)
                }
                if (opcion != CalidadGrabacion.values().last()) {
                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                }
            }
        }
    }
}
