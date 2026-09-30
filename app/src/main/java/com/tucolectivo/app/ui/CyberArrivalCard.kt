package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CyberArrivalCard(
    lineLabel: String,
    destination: String,
    minutes: Int?,
    modifier: Modifier = Modifier,
    isArriving: Boolean = minutes != null && minutes <= 1
) {
    val pulse by CyberAnimation.pulse(min = 0.3f, max = 1f)
    val sweep by CyberAnimation.sweep(durationMillis = 1800)
    val glitch by CyberAnimation.glitch(amplitude = 1.5f)

    Box(modifier.fillMaxWidth().height(116.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 2.dp.toPx()
            drawRoundRect(
                color = CyberColors.Surface,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = .10f + .10f * pulse),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(8.dp.toPx())
            )
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = .55f + .3f * pulse),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(1.5.dp.toPx())
            )
            val scanX = 10.dp.toPx() + (size.width - 20.dp.toPx()) * sweep
            drawLine(
                color = CyberColors.Secondary.copy(alpha = .7f),
                start = Offset(scanX, 7.dp.toPx()),
                end = Offset(scanX, size.height - 7.dp.toPx()),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            val gx = 12.dp.toPx() + glitch.dp.toPx()
            drawLine(CyberColors.Tertiary, Offset(gx, 10.dp.toPx()), Offset(gx + 24.dp.toPx(), 10.dp.toPx()), 2.dp.toPx())
            drawLine(CyberColors.Secondary.copy(alpha = .8f), Offset(size.width - 54.dp.toPx(), size.height - 10.dp.toPx()), Offset(size.width - 14.dp.toPx(), size.height - 10.dp.toPx()), 2.dp.toPx())
        }

        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ARRIBO // ${lineLabel.uppercase()}", color = CyberColors.Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(destination.uppercase(), color = CyberColors.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(
                    if (isArriving) "STATUS // ARRIBANDO" else "STATUS // EN RUTA",
                    color = if (isArriving) CyberColors.Secondary else CyberColors.Tertiary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            NeonMinutesRing(minutes = minutes, isArriving = isArriving, size = 84.dp)
        }
    }
}
