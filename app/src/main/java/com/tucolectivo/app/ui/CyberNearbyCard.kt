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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberNearbyCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = CyberAnimation.pulse(min = 0.35f, max = 1f)
    val sweep = CyberAnimation.sweep(durationMillis = 2200)
    val glow = CyberAnimation.glow(min = 0.45f, max = 1f)
    val glitch = CyberAnimation.glitch(amplitude = 1.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF07170F),
                        CyberColors.Surface,
                        Color(0xFF100619)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = CyberColors.Tertiary.copy(alpha = 0.28f + pulse.value * 0.32f),
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
            )

            val center = androidx.compose.ui.geometry.Offset(
                x = size.width - 42.dp.toPx(),
                y = 48.dp.toPx()
            )
            val radius = 25.dp.toPx()
            val angle = sweep.value * 360f
            val end = androidx.compose.ui.geometry.Offset(
                x = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * radius,
                y = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * radius
            )

            drawCircle(
                color = CyberColors.Tertiary.copy(alpha = 0.08f + glow.value * 0.10f),
                radius = radius + 7.dp.toPx(),
                center = center
            )
            drawCircle(
                color = CyberColors.Tertiary.copy(alpha = 0.45f + glow.value * 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.45f + pulse.value * 0.35f),
                start = center,
                end = end,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = CyberColors.Primary.copy(alpha = 0.7f + pulse.value * 0.3f),
                radius = 3.dp.toPx(),
                center = end
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
                        "03 // SENSOR DE PROXIMIDAD",
                        color = CyberColors.Tertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.35.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "PARADAS CERCANAS",
                        color = CyberColors.OnSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                }
                Text(
                    "◎",
                    color = CyberColors.Tertiary.copy(alpha = 0.55f + glow.value * 0.45f),
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "GEOLOCALIZACIÓN • DISTANCIA • ARRIBOS",
                color = CyberColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.75.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(6) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(if (i == 2 || i == 5) 7.dp else 3.dp)
                            .background(
                                if (i == 2 || i == 5) {
                                    CyberColors.Tertiary.copy(alpha = 0.78f)
                                } else {
                                    CyberColors.Primary.copy(alpha = 0.28f + pulse.value * 0.22f)
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "ESCANEAR ENTORNO  //  " + if (glitch.value > 0.45f) "LOCK" else "RADAR",
                color = CyberColors.Primary.copy(alpha = 0.72f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.05.sp
            )
        }
    }
}
