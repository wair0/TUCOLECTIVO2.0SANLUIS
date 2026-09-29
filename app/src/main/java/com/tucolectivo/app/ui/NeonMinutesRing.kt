package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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

/**
 * Círculo animado de minutos.
 *
 * Ángulo en mutableFloatStateOf + bucle withFrameNanos (runtime).
 * Lectura en composición → cada frame redibuja.
 * Velocidad: pocos minutos → más rápido; más minutos → más lento.
 */
@Composable
fun NeonMinutesRing(
    minutes: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 82.dp,
    isArriving: Boolean = minutes != null && minutes <= 1
) {
    val cyberFont = rememberCyberpunkFontFamily()
    val arriving = isArriving || (minutes != null && minutes <= 0)
    val display = when {
        arriving -> "ARRIBANDO"
        minutes == null -> "--"
        else -> minutes.toString()
    }

    val durationSec = when {
        arriving -> 0.22f
        minutes == null -> 1.2f
        else -> (0.25f + minutes * 0.08f).coerceIn(0.33f, 3.0f)
    }
    val degPerSec = 360f / durationSec

    var angle by remember { mutableFloatStateOf(0f) }
    var pulse by remember { mutableFloatStateOf(0.8f) }

    LaunchedEffect(durationSec) {
        var lastNanos = 0L
        var pulseT = 0f
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos).coerceAtMost(50_000_000L)) / 1_000_000_000f
                    angle = (angle + degPerSec * dt) % 360f
                    pulseT += dt
                    pulse = 0.7f + 0.3f * (0.5f + 0.5f * kotlin.math.sin(pulseT * 8.8f))
                }
                lastNanos = now
            }
        }
    }

    val drawAngle = angle
    val drawPulse = pulse

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.06f
            val r = (this.size.minDimension / 2f) - stroke * 2f
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val topLeft = Offset(cx - r, cy - r)
            val arcSize = Size(r * 2f, r * 2f)

            drawCircle(
                NeonCeleste.copy(alpha = 0.45f + 0.35f * drawPulse),
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
                val start = drawAngle - 90f
                drawArc(
                    NeonPink.copy(alpha = 0.28f * drawPulse),
                    startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke * 3.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    NeonPink.copy(alpha = 0.95f),
                    startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round)
                )
                val tipRad = Math.toRadians((start + sweep).toDouble())
                val tx = cx + r * cos(tipRad).toFloat()
                val ty = cy + r * sin(tipRad).toFloat()
                drawCircle(NeonPink.copy(alpha = 0.4f), stroke * 2.4f, Offset(tx, ty))
                drawCircle(NeonPink, stroke * 1.1f, Offset(tx, ty))
                drawCircle(NeonCore, stroke * 0.4f, Offset(tx, ty))
            } else {
                drawCircle(
                    NeonPink.copy(alpha = 0.25f + 0.35f * drawPulse),
                    radius = r, center = Offset(cx, cy),
                    style = Stroke(width = stroke * 1.8f, cap = StrokeCap.Round)
                )
                drawArc(
                    NeonPink,
                    startAngle = drawAngle - 90f, sweepAngle = 60f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke * 1.3f, cap = StrokeCap.Round)
                )
            }
        }

        if (arriving) {
            Text(
                text = "ARRIBANDO", color = NeonPink, fontFamily = cyberFont,
                fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(NeonPink, Offset.Zero, 12f))
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = display, color = NeonCeleste, fontFamily = cyberFont,
                    fontWeight = FontWeight.Bold, fontSize = (size.value * 0.24f).sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 16f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "MIN", color = NeonCeleste.copy(alpha = 0.9f), fontFamily = cyberFont,
                    fontWeight = FontWeight.Bold, fontSize = (size.value * 0.1f).sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = size * 0.16f)
                )
            }
        }
    }
}
