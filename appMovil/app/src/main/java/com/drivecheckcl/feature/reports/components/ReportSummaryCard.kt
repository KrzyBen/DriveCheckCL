package com.drivecheckcl.feature.reports.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.SurfaceWhite
import com.drivecheckcl.theme.TextSecondary

/** Tarjeta de resumen con contador (ENVIADOS, ANALIZANDO, APROBADOS, RECHAZADOS). */
@Composable
fun ReportSummaryCard(modifier: Modifier, valor: String, label: String, color: Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = color)
            Text(label, fontSize = 9.sp,  color = TextSecondary, letterSpacing = 0.3.sp)
        }
    }
}
