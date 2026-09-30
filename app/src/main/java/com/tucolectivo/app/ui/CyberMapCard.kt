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
fun CyberMapCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = CyberAnimation.pulse(min = 0.35f, max = 1f)
    val scan = CyberAnimation.scan()
    val glitch = CyberAnimation.glitch(amplitude = 1.4f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF06161A), CyberColors.Surface, Color(0xFF100619))))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = 0.28f + pulse.value * 0.35f),
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
            )
            val y = size.height * scan.value
            drawLine(
                color = CyberColors.Primary.copy(alpha = 0.10f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 8.dp.toPx()
            )
            drawLine(
                color = CyberColors.Secondary.copy(alpha = 0.7f),
                start = androidx.compose.ui.geometry.Offset(0f, 2.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.20f, 2.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("02 // NAVEGACIÓN ESPACIAL", color = CyberColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("MAPA", color = CyberColors.OnSurface, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Text("⌖", color = CyberColors.Secondary.copy(alpha = 0.65f + pulse.value * 0.35f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("POSICIÓN • RECORRIDOS • RED URBANA", color = CyberColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(5) { i ->
                    Box(
                        Modifier.weight(1f).height(if (i == 1) 7.dp else 3.dp)
                            .background(if (i == 1) CyberColors.Secondary.copy(alpha = 0.78f) else CyberColors.Primary.copy(alpha = 0.35f + pulse.value * 0.25f))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "ABRIR SISTEMA DE MAPA  //  " + if (glitch.value > 0.5f) "LINK" else "ONLINE",
                color = CyberColors.Primary.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }
    }
}
