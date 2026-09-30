package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transpuntano.app.model.TransitArrival

@Composable
fun CyberArrivalsPanel(
    stopName: String,
    activeLineCode: Int?,
    arrivals: List<TransitArrival>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onSaveFavorite: () -> Unit,
    onMapRoute: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        CyberArrivalsHeader(stopName, activeLineCode, loading, arrivals.size, error)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberArrivalAction(
                Modifier.weight(1f),
                if (loading) "SYNC..." else "ACTUALIZAR",
                CyberColors.Primary,
                !loading,
                onRefresh
            )
            CyberArrivalAction(
                Modifier.weight(1f),
                "FAVORITO",
                CyberColors.Tertiary,
                true,
                onSaveFavorite
            )
            CyberArrivalAction(
                Modifier.weight(1.35f),
                "VER RECORRIDO EN MAPA",
                CyberColors.Secondary,
                activeLineCode != null,
                onMapRoute
            )
        }
        if (arrivals.isEmpty() && !loading) CyberArrivalsEmptyState(error)
        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp)
        ) {
            items(arrivals) { arrival ->
                CyberArrivalCard(
                    lineLabel = if (arrival.line.isBlank()) "LÍNEA ${activeLineCode ?: "--"}" else arrival.line,
                    destination = arrival.destination.ifBlank { "DESTINO" },
                    minutes = arrival.minutes,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CyberArrivalsHeader(
    stopName: String,
    activeLineCode: Int?,
    loading: Boolean,
    arrivalsCount: Int,
    error: String?
) {
    val pulse by CyberAnimation.pulse(min = .35f, max = 1f)
    val sweep by CyberAnimation.sweep(durationMillis = 2100)
    val glitch by CyberAnimation.glitch(amplitude = 1.2f)
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(112.dp)) {
        Canvas(Modifier.fillMaxWidth().height(112.dp)) {
            drawRoundRect(
                CyberColors.Surface,
                Offset(1.dp.toPx(), 1.dp.toPx()),
                Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                CornerRadius(18.dp.toPx(), 18.dp.toPx())
            )
            drawRoundRect(
                CyberColors.Primary.copy(alpha = .12f + .08f * pulse),
                Offset(1.dp.toPx(), 1.dp.toPx()),
                Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(8.dp.toPx())
            )
            drawRoundRect(
                CyberColors.Primary.copy(alpha = .65f + .25f * pulse),
                Offset(1.dp.toPx(), 1.dp.toPx()),
                Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(1.6.dp.toPx())
            )
            val x = 12.dp.toPx() + (size.width - 24.dp.toPx()) * sweep
            drawLine(
                CyberColors.Secondary.copy(alpha = .75f),
                Offset(x, 6.dp.toPx()),
                Offset(x, size.height - 6.dp.toPx()),
                2.dp.toPx(),
                StrokeCap.Round
            )
            val gx = 16.dp.toPx() + glitch.dp.toPx()
            drawLine(
                CyberColors.Tertiary.copy(alpha = .85f),
                Offset(gx, 22.dp.toPx()),
                Offset(gx + 26.dp.toPx(), 22.dp.toPx()),
                2.dp.toPx()
            )
            drawLine(
                CyberColors.Secondary.copy(alpha = .8f),
                Offset(18.dp.toPx(), size.height - 18.dp.toPx()),
                Offset(74.dp.toPx(), size.height - 18.dp.toPx()),
                3.dp.toPx()
            )
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 13.dp)) {
            Text("10 // ARRIBOS EN TIEMPO REAL", color = CyberColors.Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                stopName.ifBlank { "PARADA ACTIVA" }.uppercase(),
                color = CyberColors.OnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "LÍNEA ${activeLineCode ?: "--"} // ${arrivalsCount.toString().padStart(2, '0')} ARRIBOS // ${if (loading) "SINCRONIZANDO" else "LINK ONLINE"}",
                color = if (loading) CyberColors.Secondary else CyberColors.Tertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            error?.let {
                Text("ERROR // ${it.uppercase()}", color = CyberColors.Error, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CyberArrivalAction(
    modifier: Modifier,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val pulse by CyberAnimation.pulse(min = .4f, max = 1f)
    Box(modifier.height(54.dp).clickable(enabled = enabled, onClick = onClick)) {
        Canvas(Modifier.fillMaxWidth().height(54.dp)) {
            drawRoundRect(CyberColors.SurfaceVariant, cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()))
            drawRoundRect(
                accent.copy(alpha = if (enabled) .58f + .22f * pulse else .2f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(1.3.dp.toPx())
            )
        }
        Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (enabled) accent else CyberColors.Muted,
                fontSize = if (label.length > 12) 7.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CyberArrivalsEmptyState(error: String?) {
    val pulse by CyberAnimation.pulse(min = .25f, max = .7f)
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp).height(74.dp)) {
        Canvas(Modifier.fillMaxWidth().height(74.dp)) {
            drawRoundRect(CyberColors.SurfaceVariant, cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()))
            drawRoundRect(
                CyberColors.Border.copy(alpha = .45f + .25f * pulse),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                if (error == null) "SIN ARRIBOS DETECTADOS" else "LINK DE ARRIBOS INTERRUMPIDO",
                color = if (error == null) CyberColors.Muted else CyberColors.Error,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (error == null) "ACTUALIZA EL NODO PARA VOLVER A SINCRONIZAR"
                else "REVISA LA CONEXIÓN Y REINTENTA LA SINCRONIZACIÓN",
                color = CyberColors.Muted,
                fontSize = 8.sp
            )
        }
    }
}
