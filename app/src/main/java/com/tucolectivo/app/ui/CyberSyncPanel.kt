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
fun CyberSyncPanel(
    syncing: Boolean,
    statusText: String,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = CyberAnimation.pulse(min = 0.45f, max = 1f)
    val sweep = CyberAnimation.sweep()
    val glitch = CyberAnimation.glitch(amplitude = 1.2f)

    val cyan = CyberColors.Primary
    val pink = CyberColors.Secondary
    val green = CyberColors.Tertiary
    val surface = CyberColors.Surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(surface, Color(0xFF07131C), CyberColors.Background)
                )
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF070B12))
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val glowAlpha = 0.28f + pulse.value * 0.22f
                drawRoundRect(
                    color = cyan.copy(alpha = glowAlpha),
                    style = Stroke(width = 2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                )

                val sweepX = size.width * sweep.value
                drawLine(
                    color = cyan.copy(alpha = 0.08f),
                    start = androidx.compose.ui.geometry.Offset(sweepX, 0f),
                    end = androidx.compose.ui.geometry.Offset(sweepX, size.height),
                    strokeWidth = 7.dp.toPx()
                )

                drawLine(
                    color = pink.copy(alpha = 0.42f),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - 2.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NÚCLEO DE SINCRONIZACIÓN",
                            color = cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (syncing) "ENLACE DE DATOS ACTIVO" else "SISTEMA EN LÍNEA",
                            color = CyberColors.OnSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(green.copy(alpha = 0.08f))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (syncing) "SYNC" else "READY",
                            color = green.copy(alpha = 0.65f + pulse.value * 0.35f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (syncing) "▰▰▰ TRANSMITIENDO DATOS..." else statusText.removePrefix("● ").uppercase(),
                    color = if (syncing) pink else CyberColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cyan.copy(alpha = 0.06f))
                        .clickable(enabled = !syncing, onClick = onSync)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (syncing) "SINCRONIZANDO..." else "INICIAR SINCRONIZACIÓN",
                        color = cyan.copy(alpha = 0.72f + pulse.value * 0.28f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.15.sp
                    )
                    Text(
                        text = if (syncing) "◉" else "▶",
                        color = pink.copy(alpha = 0.7f + kotlin.math.abs(glitch.value) * 0.2f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
