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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CyberLinesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = CyberAnimation.pulse(min = 0.35f, max = 1f)
    val sweep = CyberAnimation.sweep()
    val glow = CyberAnimation.glow(min = 0.45f, max = 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF07131C),
                        CyberColors.Surface,
                        Color(0xFF100718)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = 0.30f + pulse.value * 0.32f),
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
            )

            val x = size.width * sweep.value
            drawLine(
                color = CyberColors.Primary.copy(alpha = 0.12f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 9.dp.toPx()
            )

            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.65f),
                start = androidx.compose.ui.geometry.Offset(0f, size.height - 2.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.30f, size.height - 2.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "01 // RED DE TRANSPORTE",
                        color = CyberColors.Secondary.copy(alpha = 0.72f + glow.value * 0.28f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "LÍNEAS",
                        color = CyberColors.OnSurface,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberColors.Primary.copy(alpha = 0.09f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "▶",
                        color = CyberColors.Primary.copy(alpha = 0.65f + pulse.value * 0.35f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "EXPLORAR RECORRIDOS • CALLES • PARADAS • ARRIBOS",
                color = CyberColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(7) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(if (index == 2 || index == 5) 5.dp else 3.dp)
                            .background(
                                if (index == 2 || index == 5)
                                    CyberColors.Secondary.copy(alpha = 0.75f)
                                else
                                    CyberColors.Primary.copy(alpha = 0.42f + pulse.value * 0.25f)
                            )
                    )
                }
            }
        }
    }
}
