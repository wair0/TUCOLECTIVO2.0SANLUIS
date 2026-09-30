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
import com.transpuntano.app.model.TransitIntersection
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.model.TransitStreet

@Composable
fun CyberLinesHeader(
    level: LinesLevel,
    activeLine: TransitLine?,
    activeStreet: TransitStreet?,
    activeIntersection: TransitIntersection?,
    activeStop: TransitStop?,
    onBack: () -> Unit
) {
    val pulse by CyberAnimation.pulse(min = 0.38f, max = 1f)
    val sweep by CyberAnimation.sweep(durationMillis = 2400)
    val glitch by CyberAnimation.glitch(amplitude = 1.4f)

    val routeTitle = when (level) {
        LinesLevel.CATALOG -> "RED DE TRANSPORTE"
        LinesLevel.STREETS -> "RECORRIDO // CALLES"
        LinesLevel.INTERSECTIONS -> "NODO // INTERSECCIONES"
        LinesLevel.STOPS -> "NODO // PARADAS"
        LinesLevel.ARRIVALS -> "ARRIBOS // TIEMPO REAL"
    }

    val context = when (level) {
        LinesLevel.CATALOG -> "CATÁLOGO PRINCIPAL"
        LinesLevel.STREETS -> activeLine?.let { "LÍNEA " + it.code + " // " + it.name } ?: "LÍNEA ACTIVA"
        LinesLevel.INTERSECTIONS -> activeStreet?.name ?: "CALLE ACTIVA"
        LinesLevel.STOPS -> activeIntersection?.name ?: "INTERSECCIÓN ACTIVA"
        LinesLevel.ARRIVALS -> activeStop?.description ?: "PARADA ACTIVA"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberColors.Background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(146.dp)) {
            Canvas(Modifier.fillMaxWidth().height(146.dp)) {
                val l = 2.dp.toPx()
                val t = 2.dp.toPx()
                val r = size.width - 2.dp.toPx()
                val b = size.height - 2.dp.toPx()
                val radius = 18.dp.toPx()

                drawRoundRect(
                    color = CyberColors.Surface,
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = CyberColors.Primary.copy(alpha = 0.10f + 0.07f * pulse),
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(8.dp.toPx())
                )
                drawRoundRect(
                    color = CyberColors.Primary.copy(alpha = 0.70f + 0.22f * pulse),
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(1.5.dp.toPx())
                )

                val scanX = l + (r - l) * sweep
                drawLine(
                    color = CyberColors.Secondary.copy(alpha = 0.78f),
                    start = Offset(scanX, t + 3.dp.toPx()),
                    end = Offset(scanX, b - 3.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val cut = 22.dp.toPx()
                drawLine(CyberColors.Secondary, Offset(l, t + cut), Offset(l, t), 2.2.dp.toPx())
                drawLine(CyberColors.Secondary, Offset(l, t), Offset(l + cut, t), 2.2.dp.toPx())
                drawLine(CyberColors.Secondary, Offset(r - cut, b), Offset(r, b), 2.2.dp.toPx())
                drawLine(CyberColors.Secondary, Offset(r, b - cut), Offset(r, b), 2.2.dp.toPx())

                val y = b - 28.dp.toPx()
                drawLine(
                    CyberColors.Border,
                    Offset(18.dp.toPx(), y),
                    Offset(r - 18.dp.toPx(), y),
                    1.dp.toPx()
                )
                drawLine(
                    CyberColors.Tertiary.copy(alpha = 0.75f),
                    Offset(18.dp.toPx(), y),
                    Offset(78.dp.toPx(), y),
                    3.dp.toPx()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(42.dp)) {
                        drawRoundRect(
                            color = CyberColors.Primary.copy(alpha = 0.08f),
                            cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
                        )
                        drawRoundRect(
                            color = CyberColors.Primary.copy(alpha = 0.85f),
                            cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                            style = Stroke(1.3.dp.toPx())
                        )
                        drawLine(
                            CyberColors.Secondary,
                            Offset(25.dp.toPx(), 21.dp.toPx()),
                            Offset(13.dp.toPx(), 21.dp.toPx()),
                            2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            CyberColors.Secondary,
                            Offset(13.dp.toPx(), 21.dp.toPx()),
                            Offset(19.dp.toPx(), 15.dp.toPx()),
                            2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            CyberColors.Secondary,
                            Offset(13.dp.toPx(), 21.dp.toPx()),
                            Offset(19.dp.toPx(), 27.dp.toPx()),
                            2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "06 // SISTEMA DE LÍNEAS",
                        color = CyberColors.Secondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.7.sp
                    )
                    Text(
                        "LÍNEAS",
                        color = CyberColors.OnSurface,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier.padding(start = glitch.dp)
                    )
                    Text(
                        routeTitle,
                        color = CyberColors.Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("LINK", color = CyberColors.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("ONLINE", color = CyberColors.Tertiary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("SYNC 100%", color = CyberColors.Primary.copy(alpha = 0.85f), fontSize = 8.sp)
                }
            }

            Text(
                context.uppercase(),
                color = CyberColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 10.dp)
            )

            Text(
                "NODE_" + (level.ordinal + 1),
                color = CyberColors.Tertiary.copy(alpha = 0.75f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 10.dp)
            )
        }
    }
}
