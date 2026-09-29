package com.tucolectivo.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)

/** Círculo animado de minutos. Pocos min → más rápido; más min → más lento. */
@Composable
fun NeonMinutesRing(
    minutes: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    isArriving: Boolean = minutes != null && minutes <= 1
) {
    val cyberFont = rememberCyberpunkFontFamily()
    val arriving = isArriving || (minutes != null && minutes <= 0)
    val display = when {
        arriving -> "ARRIBANDO"
        minutes == null -> "--"
        else -> minutes.toString()
    }

    val durationMs = when {
        arriving -> 220
        minutes == null -> 1200
        else -> ((0.25f + minutes * 0.08f).coerceIn(0.33f, 3.0f) * 1000f).toInt()
    }.coerceAtLeast(120)

    val phase = remember(durationMs) { Animatable(0f) }
    LaunchedEffect(durationMs) {
        while (true) {
            phase.snapTo(0f)
            phase.animateTo(360f, tween(durationMs, easing = LinearEasing))
        }
    }

    val infinite = rememberInfiniteTransition(label = "ringPulse")
    val pulse by infinite.animateFloat(
        0.7f, 1f,
        infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val angle = phase.value
            val stroke = this.size.minDimension * 0.06f
            val r = (this.size.minDimension / 2f) - stroke * 2f
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val topLeft = Offset(cx - r, cy - r)
            val arcSize = Size(r * 2f, r * 2f)

            drawCircle(
                NeonCeleste.copy(alpha = 0.15f * pulse),
                radius = r + stroke * 2.5f,
                center = Offset(cx, cy),
                style = Stroke(width = stroke * 2.8f)
            )
            drawCircle(
                NeonCeleste.copy(alpha = 0.5f + 0.3f * pulse),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawCircle(
                NeonCore.copy(alpha = 0.4f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = stroke * 0.3f, cap = StrokeCap.Round)
            )

            if (!arriving) {
                val sweep = 100f
                drawArc(
                    NeonPink.copy(alpha = 0.3f * pulse),
                    startAngle = angle - 90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 3.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    NeonPink.copy(alpha = 0.95f),
                    startAngle = angle - 90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round)
                )
                val tipRad = Math.toRadians((angle - 90f + sweep).toDouble())
                val tx = cx + r * cos(tipRad).toFloat()
                val ty = cy + r * sin(tipRad).toFloat()
                drawCircle(NeonPink.copy(alpha = 0.4f), stroke * 2.4f, Offset(tx, ty))
                drawCircle(NeonPink, stroke * 1.1f, Offset(tx, ty))
                drawCircle(NeonCore, stroke * 0.4f, Offset(tx, ty))
            } else {
                drawCircle(
                    NeonPink.copy(alpha = 0.25f + 0.35f * pulse),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke * 1.8f, cap = StrokeCap.Round)
                )
                drawArc(
                    NeonPink,
                    startAngle = angle - 90f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 1.3f, cap = StrokeCap.Round)
                )
            }
        }

        if (arriving) {
            Text(
                text = "ARRIBANDO",
                color = NeonPink,
                fontFamily = cyberFont,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(NeonPink, Offset.Zero, 12f))
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = display,
                    color = NeonCeleste,
                    fontFamily = cyberFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.24f).sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 16f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "MIN",
                    color = NeonCeleste.copy(alpha = 0.9f),
                    fontFamily = cyberFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.1f).sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = size * 0.16f)
                )
            }
        }
    }
}
