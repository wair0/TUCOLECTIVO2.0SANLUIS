package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Panel = Color(0xFF050714)

/**
 * Tarjeta de arribo cyberpunk con [NeonMinutesRing].
 * Sustituye CyberArrivalCardView (View/Canvas) por Compose + Canvas.
 */
@Composable
fun NeonArrivalCard(
    lineLabel: String,
    destination: String,
    minutes: Int?,
    modifier: Modifier = Modifier,
    isArriving: Boolean = minutes != null && minutes <= 1
) {
    Box(modifier.fillMaxWidth().height(104.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val pad = 2.dp.toPx()
            val left = pad
            val top = pad
            val right = size.width - pad
            val bottom = size.height - pad
            drawRect(Panel, Offset(left, top), Size(right - left, bottom - top))
            drawRect(NeonCeleste.copy(alpha = 0.10f), Offset(left, top), Size(right - left, bottom - top), style = Stroke(width = 6.dp.toPx()))
            drawRect(NeonCeleste.copy(alpha = 0.75f), Offset(left, top), Size(right - left, bottom - top), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Square))
            drawRect(NeonPink.copy(alpha = 0.7f), Offset(left + 2.dp.toPx(), top + 2.dp.toPx()), Size(34.dp.toPx(), 2.dp.toPx()))
            drawRect(NeonPink.copy(alpha = 0.7f), Offset(right - 36.dp.toPx(), bottom - 4.dp.toPx()), Size(34.dp.toPx(), 2.dp.toPx()))
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = lineLabel.uppercase(),
                    color = NeonCore,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.horizontalGradient(listOf(NeonCeleste, NeonPink))
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = destination.uppercase(),
                    color = NeonPink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            NeonMinutesRing(
                minutes = minutes,
                isArriving = isArriving,
                size = 82.dp
            )
        }
    }
}
