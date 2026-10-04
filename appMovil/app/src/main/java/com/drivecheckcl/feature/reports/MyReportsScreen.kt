package com.drivecheckcl.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.data.model.EstadoInforme
import com.drivecheckcl.data.model.InformeLocal
import com.drivecheckcl.feature.reports.components.ReportCard
import com.drivecheckcl.feature.reports.components.ReportFilterChip
import com.drivecheckcl.feature.reports.components.ReportSummaryCard
import com.drivecheckcl.theme.*
import com.drivecheckcl.ui.components.layout.AppTopBar
import com.drivecheckcl.ui.components.layout.FlagAccentBar
import com.drivecheckcl.ui.components.text.SectionLabel
import com.drivecheckcl.util.openPdfFile
import java.util.concurrent.TimeUnit

private const val FILTRO_TODOS        = 0
private const val FILTRO_ESTA_SEMANA  = 1
private const val FILTRO_ESTE_MES     = 2

@Composable
fun MyReportsScreen(
    onBack:          () -> Unit,
    onCrearInforme:  () -> Unit,
    viewModel:       ReportViewModel = viewModel()
) {
    val informesTodos by viewModel.informes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filtroActivo by remember { mutableIntStateOf(FILTRO_TODOS) }
    var informeAEliminar by remember { mutableStateOf<InformeLocal?>(null) }

    LaunchedEffect(Unit) {
        viewModel.cargarInformes()
    }

    // ── Filtro por fecha, ahora conectado de verdad (antes "Esta semana"/"Este mes"
    //    cambiaban de estado pero nunca filtraban la lista) ─────────────────────
    val informes = remember(informesTodos, filtroActivo) {
        if (filtroActivo == FILTRO_TODOS) {
            informesTodos
        } else {
            val limiteDias = if (filtroActivo == FILTRO_ESTA_SEMANA) 7 else 30
            val limiteMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(limiteDias.toLong())
            informesTodos.filter { it.fechaCreacion >= limiteMillis }
        }
    }

    // Contadores para resumen (sobre el resultado ya filtrado)
    val totalEnviados   = informes.count { it.estado == EstadoInforme.ENVIADO || it.estado == EstadoInforme.RECIBIDO }
    val totalAnalizando = informes.count { it.estado == EstadoInforme.ANALIZANDO }
    val totalAprobados  = informes.count { it.estado == EstadoInforme.APROBADO }
    val totalRechazados = informes.count { it.estado == EstadoInforme.RECHAZADO }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        AppTopBar(
            title  = "Mis informes",
            onBack = onBack,
            extraContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Esta semana", "Este mes").forEachIndexed { i, label ->
                        ReportFilterChip(
                            label   = label,
                            active  = filtroActivo == i,
                            onClick = { filtroActivo = i }
                        )
                    }
                }
            }
        )

        FlagAccentBar()

        Box(modifier = Modifier.fillMaxSize()) {
            if (informes.isEmpty()) {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Assignment, null,
                        tint     = BorderGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (informesTodos.isEmpty()) "No hay informes aún" else "Sin informes en este período",
                        fontSize   = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color      = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (informesTodos.isEmpty()) "Crea tu primer informe con un video grabado" else "Prueba con otro filtro de fecha",
                        fontSize = 12.sp,
                        color    = TextHint
                    )
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Resumen
                    item {
                        Row(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportSummaryCard(modifier = Modifier.weight(1f), valor = "$totalEnviados", label = "ENVIADOS", color = ChileBlue)
                            ReportSummaryCard(modifier = Modifier.weight(1f), valor = "$totalAnalizando", label = "ANALIZANDO", color = WarningAmber)
                            ReportSummaryCard(modifier = Modifier.weight(1f), valor = "$totalAprobados", label = "APROBADOS", color = SuccessGreen)
                            ReportSummaryCard(modifier = Modifier.weight(1f), valor = "$totalRechazados", label = "RECHAZADOS", color = Color(0xFFD52B1E))
                        }
                    }

                    item { SectionLabel("HISTORIAL") }

                    items(informes, key = { it.id }) { informe ->
                        ReportCard(
                            informe        = informe,
                            onActualizar   = { viewModel.cargarInformes() },
                            onDescargarPdf = {
                                viewModel.descargarPdf(context, informe) { archivo ->
                                    openPdfFile(context, archivo.absolutePath)
                                }
                            },
                            onEliminar     = { informeAEliminar = informe }
                        )
                    }
                }
            }

            // ── FAB crear informe ─────────────────────────────────────────────
            ExtendedFloatingActionButton(
                onClick          = onCrearInforme,
                modifier         = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor   = ChileRed,
                contentColor     = White,
                icon             = { Icon(Icons.Default.Add, null) },
                text             = { Text("Crear informe", fontSize = 13.sp) }
            )
        }
    }

    // ── Diálogo de confirmación de eliminación ─────────────────────────────────
    informeAEliminar?.let { informe ->
        AlertDialog(
            onDismissRequest = { informeAEliminar = null },
            title            = { Text("Eliminar informe") },
            text             = { Text("¿Seguro que quieres eliminar el informe \"${informe.titulo}\"? Esta acción no se puede deshacer.") },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.eliminarInforme(informe.id)
                    informeAEliminar = null
                }) {
                    Text("Eliminar", color = Color(0xFFD52B1E))
                }
            },
            dismissButton = {
                TextButton(onClick = { informeAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
