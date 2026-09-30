package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun CyberLineCard(
    lineCode: Int,
    lineName: String,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse by CyberAnimation.pulse(
        durationMillis = 1500 + (index % 4) * 120,
        min = 0.34f,
        max = 1f
    )
    val sweep by CyberAnimation.sweep(
        durationMillis = 2300 + (index % 3) * 180
    )
    val glitch by CyberAnimation.glitch(
        amplitude = 1.1f,
        durationMs = 1800 + (index % 3) * 90
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxWidth().height(118.dp)) {
            val inset = 2.dp.toPx()
            val w = size.width - inset * 2f
            val h = size.height - inset * 2f
            val radius = 16.dp.toPx()

            drawRoundRect(
                color = CyberColors.Surface,
                topLeft = Offset(inset, inset),
                size = Size(w, h),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = 0.08f + pulse * 0.08f),
                topLeft = Offset(inset, inset),
                size = Size(w, h),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(8.dp.toPx())
            )
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = 0.72f + pulse * 0.22f),
                topLeft = Offset(inset, inset),
                size = Size(w, h),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(1.6.dp.toPx())
            )

            val scanX = inset + w * sweep
            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.85f),
                start = Offset(scanX, inset + 6.dp.toPx()),
                end = Offset(scanX, inset + h - 6.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            val segmentY = size.height - 17.dp.toPx()
            val gap = 8.dp.toPx()
            val segment = (size.width - 36.dp.toPx() - gap * 5f) / 6f
            for (i in 0 until 6) {
                val startX = 18.dp.toPx() + i * (segment + gap)
                drawLine(
                    color = if ((i + index) % 3 == 0) {
                        CyberColors.Secondary.copy(alpha = 0.9f)
                    } else {
                        CyberColors.Primary.copy(alpha = 0.55f)
                    },
                    start = Offset(startX, segmentY),
                    end = Offset(startX + segment, segmentY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Square
                )
            }

            val cut = 18.dp.toPx()
            drawLine(CyberColors.Secondary, Offset(inset, inset + cut), Offset(inset, inset), 2.dp.toPx())
            drawLine(CyberColors.Secondary, Offset(inset, inset), Offset(inset + cut, inset), 2.dp.toPx())
            drawLine(CyberColors.Tertiary, Offset(size.width - cut, size.height - inset), Offset(size.width - inset, size.height - inset), 2.dp.toPx())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(58.dp)) {
                    drawRoundRect(
                        color = CyberColors.Primary.copy(alpha = 0.08f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                    drawRoundRect(
                        color = CyberColors.Secondary.copy(alpha = 0.72f + pulse * 0.2f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(1.4.dp.toPx())
                    )
                    drawCircle(
                        color = CyberColors.Tertiary.copy(alpha = 0.8f),
                        radius = 3.dp.toPx(),
                        center = Offset(29.dp.toPx(), 29.dp.toPx())
                    )
                }
                Text(
                    lineCode.toString(),
                    color = CyberColors.OnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = glitch.dp)
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    "LINE // %02d".format(index + 1),
                    color = CyberColors.Secondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    lineName.uppercase(),
                    color = CyberColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "RECORRIDO // CALLES // PARADAS",
                    color = CyberColors.Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
            }

            Text(
                "LINK",
                color = CyberColors.Primary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
