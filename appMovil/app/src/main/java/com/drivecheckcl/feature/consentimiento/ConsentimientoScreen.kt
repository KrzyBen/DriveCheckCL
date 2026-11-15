package com.drivecheckcl.feature.consentimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.feedback.ErrorText
import com.drivecheckcl.ui.components.layout.FlagAccentBar
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.ConsentimientoTexto

/**
 * Puerta entre el login (o una sesión ya iniciada) y Home. Consulta al servidor
 * y, si el usuario no ha respondido a la versión vigente del texto, muestra el
 * aviso + la casilla opcional. Si no hace falta decidir, o no hay conexión,
 * pasa directo a Home sin bloquear.
 */
@Composable
fun ConsentimientoScreen(
    onContinuar: () -> Unit,
    viewModel: ConsentimientoViewModel = viewModel()
) {
    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Estado local: se reinicia cada vez que la pantalla entra a la composición,
    // así que nunca arrastra el resultado de una sesión anterior.
    var mostrarFormulario by remember { mutableStateOf(false) }
    var acepta by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.requiereDecision()) mostrarFormulario = true else onContinuar()
    }

    if (!mostrarFormulario) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ChileBlue)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChileBlue)
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
        ) {
            Text(ConsentimientoTexto.TITULO, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = White)
        }
        FlagAccentBar()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionLabel("AVISO")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .padding(14.dp)
            ) {
                Text(ConsentimientoTexto.AVISO, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)
            }

            SectionLabel("OPCIONAL")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .clickable { acepta = !acepta }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = acepta,
                    onCheckedChange = { acepta = it },
                    colors = CheckboxDefaults.colors(checkedColor = ChileBlue)
                )
                Text(
                    ConsentimientoTexto.CASILLA,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    lineHeight = 19.sp,
                    modifier   = Modifier.weight(1f).padding(top = 12.dp, end = 8.dp)
                )
            }

            error?.let { ErrorText(it) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick  = { viewModel.guardar(acepta) { onContinuar() } },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ChileRed),
                enabled  = !guardando
            ) {
                if (guardando) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Continuar", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = White)
                }
            }
            // Solo si falló el guardado: no se deja al conductor sin poder grabar
            // por un problema de conexión. Sin decisión guardada, se vuelve a preguntar.
            if (error != null) {
                TextButton(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
                    Text("Decidir más tarde", fontSize = 13.sp, color = ChileBlue)
                }
            }
        }
    }
}
