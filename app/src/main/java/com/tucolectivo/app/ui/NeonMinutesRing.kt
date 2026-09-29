package com.tucolectivo.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)

/**
 * Círculo de minutos cyberpunk (recreación mejorada del SVG/Canvas legacy).
 *
 * Velocidad del arco rosa:
 * - Pocos minutos / ARRIBANDO → gira más rápido
 * - Más minutos → gira más lento
 *
 * Fórmula heredada del View original:
 *   durationSec = 0.22 si arribando
 *   durationSec = (0.25 + minutos * 0.08).coerceIn(0.33, 3.0) si no
 *   → tiempo de una vuelta completa en segundos.
 */
@Composable
fun NeonMinutesRing(
    minutes: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    isArriving: Boolean = minutes != null && minutes <= 1
) {
    val arriving = isArriving || (minutes != null && minutes <= 0)
    val display = when {
        arriving -> "ARRIBANDO"
        minutes == null -> "--"
        else -> minutes.toString()
    }

    // Segundos por vuelta completa (más bajo = más rápido).
    val durationSec = when {
        arriving -> 0.22f
        minutes == null -> 1.2f
        else -> (0.25f + minutes * 0.08f).coerceIn(0.33f, 3.0f)
    }
    val durationMs = (durationSec * 1000f).toInt().coerceAtLeast(120)

    // key: al cambiar minutos se reinicia la transición con la nueva velocidad.
    key(durationMs, arriving) {
        val infinite = rememberInfiniteTransition(label = "minutesRing")
        val phase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        val pulse by infinite.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = this.size.minDimension * 0.055f
                val r = (this.size.minDimension / 2f) - stroke * 1.8f
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val topLeft = Offset(cx - r, cy - r)
                val arcSize = Size(r * 2f, r * 2f)

                // Halo exterior suave
                drawCircle(
                    color = NeonCeleste.copy(alpha = 0.12f * pulse),
                    radius = r + stroke * 2.2f,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke * 2.4f)
                )

                // Aro base celeste
                drawCircle(
                    color = NeonCeleste.copy(alpha = 0.55f + 0.25f * pulse),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // Núcleo del tubo
                drawCircle(
                    color = NeonCore.copy(alpha = 0.35f),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke * 0.35f, cap = StrokeCap.Round)
                )

                if (!arriving) {
                    // Arco rosa móvil (~92° como el legacy)
                    val sweep = 92f
                    drawArc(
                        color = NeonPink.copy(alpha = 0.25f * pulse),
                        startAngle = phase,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke * 2.8f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = NeonPink.copy(alpha = 0.95f),
                        startAngle = phase,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // Punta del arco
                    val tipRad = Math.toRadians((phase + sweep).toDouble())
                    val tx = cx + r * cos(tipRad).toFloat()
                    val ty = cy + r * sin(tipRad).toFloat()
                    drawCircle(
                        color = NeonPink.copy(alpha = 0.35f),
                        radius = stroke * 2.2f,
                        center = Offset(tx, ty)
                    )
                    drawCircle(
                        color = NeonPink,
                        radius = stroke * 0.95f,
                        center = Offset(tx, ty)
                    )
                    drawCircle(
                        color = NeonCore,
                        radius = stroke * 0.35f,
                        center = Offset(tx, ty)
                    )
                } else {
                    // ARRIBANDO: anillo rosa pulsante completo
                    drawCircle(
                        color = NeonPink.copy(alpha = 0.2f + 0.35f * pulse),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = stroke * 1.6f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = NeonPink,
                        startAngle = phase,
                        sweepAngle = 48f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round)
                    )
                }
            }

            if (arriving) {
                Text(
                    text = "ARRIBANDO",
                    color = NeonCeleste,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 12f)),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                ColumnCenterMinutes(display = display, size = size)
            }
        }
    }
}

@Composable
private fun ColumnCenterMinutes(display: String, size: Dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = display,
            color = NeonCeleste,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.22f).sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 14f)),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "M I N",
            color = NeonCeleste.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.09f).sp,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = size * 0.18f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060912)
@Composable
private fun PreviewRingFast() {
    NeonMinutesRing(minutes = 2, size = 88.dp)
}

@Preview(showBackground = true, backgroundColor = 0xFF060912)
@Composable
private fun PreviewRingSlow() {
    NeonMinutesRing(minutes = 18, size = 88.dp)
}

@Preview(showBackground = true, backgroundColor = 0xFF060912)
@Composable
private fun PreviewArriving() {
    NeonMinutesRing(minutes = 0, isArriving = true, size = 88.dp)
}
