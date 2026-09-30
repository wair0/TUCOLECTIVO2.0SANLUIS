package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CyberFavoritesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = CyberAnimation.pulse(min = 0.3f, max = 1f)
    val glow = CyberAnimation.glow(min = 0.4f, max = 1f)
    val sweep = CyberAnimation.sweep(durationMillis = 2400)
    val glitch = CyberAnimation.glitch(amplitude = 1.3f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF120719),
                        CyberColors.Surface,
                        Color(0xFF07151B)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = CyberColors.Secondary.copy(alpha = 0.28f + pulse.value * 0.34f),
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
            )

            val x = size.width * sweep.value
            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.10f + glow.value * 0.08f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 9.dp.toPx()
            )

            val center = androidx.compose.ui.geometry.Offset(
                size.width - 43.dp.toPx(),
                48.dp.toPx()
            )
            drawCircle(
                color = CyberColors.Secondary.copy(alpha = 0.08f + glow.value * 0.10f),
                radius = 30.dp.toPx(),
                center = center
            )
            drawCircle(
                color = CyberColors.Secondary.copy(alpha = 0.48f + pulse.value * 0.35f),
                radius = 21.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = CyberColors.Primary.copy(alpha = 0.72f + glow.value * 0.28f),
                radius = 5.dp.toPx(),
                center = center
            )
            drawLine(
                color = CyberColors.Primary.copy(alpha = 0.7f),
                start = center.copy(x = center.x - 12.dp.toPx()),
                end = center.copy(x = center.x + 12.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "04 // NÚCLEO DE FAVORITOS",
                        color = CyberColors.Secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.35.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "FAVORITOS",
                        color = CyberColors.OnSurface,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    "★",
                    color = CyberColors.Secondary.copy(alpha = 0.55f + glow.value * 0.45f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "PARADAS GUARDADAS • ACCESO RÁPIDO • ARRIBOS",
                color = CyberColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.65.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(5) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(if (i == 0 || i == 4) 7.dp else 3.dp)
                            .background(
                                if (i == 0 || i == 4) {
                                    CyberColors.Secondary.copy(alpha = 0.78f)
                                } else {
                                    CyberColors.Primary.copy(alpha = 0.30f + pulse.value * 0.22f)
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "ABRIR NÚCLEO  //  " + if (glitch.value > 0.5f) "SYNC" else "READY",
                color = CyberColors.Primary.copy(alpha = 0.74f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.05.sp
            )
        }
    }
}
