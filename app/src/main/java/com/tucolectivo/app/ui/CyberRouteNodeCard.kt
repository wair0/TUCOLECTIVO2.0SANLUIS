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
fun CyberRouteListHeader(
    eyebrow: String,
    title: String,
    context: String,
    error: String?
) {
    val pulse by CyberAnimation.pulse(min = 0.35f, max = 1f)
    val sweep by CyberAnimation.sweep(durationMillis = 2200)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(82.dp)) {
            Canvas(Modifier.fillMaxWidth().height(82.dp)) {
                drawRoundRect(
                    color = CyberColors.Surface,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawRoundRect(
                    color = CyberColors.Primary.copy(alpha = 0.55f + 0.22f * pulse),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(1.4.dp.toPx())
                )
                val x = 10.dp.toPx() + (size.width - 20.dp.toPx()) * sweep
                drawLine(
                    color = CyberColors.Secondary.copy(alpha = 0.55f),
                    start = Offset(x, 5.dp.toPx()),
                    end = Offset(x, size.height - 5.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(eyebrow, color = CyberColors.Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(title.uppercase(), color = CyberColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(context.uppercase(), color = CyberColors.Muted, fontSize = 9.sp, maxLines = 1)
                error?.let {
                    Text("ERROR // ${it.uppercase()}", color = CyberColors.Error, fontSize = 8.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun CyberRouteNodeCard(
    index: Int,
    eyebrow: String,
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val pulse by CyberAnimation.pulse(min = 0.35f, max = 1f)
    val sweep by CyberAnimation.sweep(durationMillis = 2000)
    val glitch by CyberAnimation.glitch(amplitude = 1.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val inset = 1.dp.toPx()
            drawRoundRect(
                color = CyberColors.Surface,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.13f + 0.07f * pulse),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(7.dp.toPx())
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.68f + 0.22f * pulse),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(1.5.dp.toPx())
            )
            val x = 8.dp.toPx() + (size.width - 16.dp.toPx()) * sweep
            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.65f),
                start = Offset(x, 4.dp.toPx()),
                end = Offset(x, size.height - 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
            val glitchX = 12.dp.toPx() + glitch.dp.toPx()
            drawLine(accent.copy(alpha = 0.85f), Offset(glitchX, 16.dp.toPx()), Offset(glitchX + 22.dp.toPx(), 16.dp.toPx()), 2.dp.toPx())
            drawLine(CyberColors.Border, Offset(18.dp.toPx(), size.height - 18.dp.toPx()), Offset(size.width - 18.dp.toPx(), size.height - 18.dp.toPx()), 1.dp.toPx())
            drawLine(accent, Offset(18.dp.toPx(), size.height - 18.dp.toPx()), Offset(58.dp.toPx(), size.height - 18.dp.toPx()), 3.dp.toPx())
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(48.dp)) {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.12f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = 0.75f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(1.3.dp.toPx())
                    )
                }
                Text(
                    "%02d".format(index + 1),
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(Modifier.weight(1f)) {
                Text(eyebrow, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(title.uppercase(), color = CyberColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Text(subtitle.uppercase(), color = CyberColors.Muted, fontSize = 8.sp, maxLines = 2)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("LINK", color = CyberColors.Tertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(">", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CyberRouteActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val pulse by CyberAnimation.pulse(min = 0.4f, max = 1f)

    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            drawRoundRect(
                color = CyberColors.SurfaceVariant,
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawRoundRect(
                color = CyberColors.Secondary.copy(alpha = 0.58f + 0.22f * pulse),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(1.4.dp.toPx())
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = CyberColors.Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = CyberColors.Muted, fontSize = 8.sp)
            }
            Text("MAP // >", color = CyberColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
