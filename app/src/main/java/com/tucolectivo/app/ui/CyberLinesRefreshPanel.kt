package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun CyberLinesRefreshPanel(
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    val pulse by CyberAnimation.pulse(durationMillis = 1300, min = 0.4f, max = 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 12.dp)
            .clickable(enabled = !loading, onClick = onRefresh),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxWidth().height(58.dp)) {
            drawRoundRect(
                color = CyberColors.SurfaceVariant.copy(alpha = 0.7f),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = if (error == null) CyberColors.Primary.copy(alpha = 0.65f + pulse * 0.25f) else CyberColors.Error,
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(1.4.dp.toPx())
            )
            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.75f),
                start = Offset(14.dp.toPx(), size.height - 8.dp.toPx()),
                end = Offset(76.dp.toPx(), size.height - 8.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Square
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (loading) "DESCARGANDO RED..." else if (error != null) "ERROR DE ENLACE" else "CATÁLOGO SINCRONIZADO",
                color = if (error != null) CyberColors.Error else CyberColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Text(
                if (loading) "BUSCANDO" else "ACTUALIZAR // LINK",
                color = CyberColors.Primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        }
    }
}
