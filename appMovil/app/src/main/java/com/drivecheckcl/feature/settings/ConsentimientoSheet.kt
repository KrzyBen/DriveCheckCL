package com.drivecheckcl.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.data.model.ConsentimientoData
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.ErrorText
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.ConsentimientoTexto
import com.drivecheckcl.util.formatEpochMillis
import com.drivecheckcl.util.parseIsoFecha

/** Hoja de Ajustes para revisar el aviso y activar o revocar el uso en el estudio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentimientoSheet(
    estado:    ConsentimientoData?,
    guardando: Boolean,
    error:     String?,
    onCambiar: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Privacidad y datos", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

            Text(ConsentimientoTexto.AVISO, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)

            SectionLabel("OPCIONAL")

            if (estado == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChileBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        ConsentimientoTexto.CASILLA,
                        fontSize   = 13.sp,
                        color      = TextPrimary,
                        lineHeight = 19.sp,
                        modifier   = Modifier.weight(1f)
                    )
                    // El valor sale siempre del servidor: si guardar falla, el interruptor no cambia.
                    Switch(
                        checked         = estado.aceptoEstudio,
                        onCheckedChange = onCambiar,
                        enabled         = !guardando,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = White,
                            checkedTrackColor   = ChileBlue,
                            uncheckedThumbColor = White,
                            uncheckedTrackColor = BorderGray
                        )
                    )
                }

                estado.fecha?.let {
                    val cuando = formatEpochMillis(parseIsoFecha(it.replace('T', ' ')))
                    Text("Última decisión: $cuando", fontSize = 11.sp, color = TextSecondary)
                }
            }

            error?.let { ErrorText(it) }
        }
    }
}
