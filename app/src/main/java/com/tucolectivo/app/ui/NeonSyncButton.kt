package com.tucolectivo.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)

@Composable
fun NeonSyncButton(
    modifier: Modifier = Modifier,
    text: String = "SINCRONIZAR LINEAS",
    syncingText: String = "SINCRONIZANDO...",
    syncing: Boolean = false,
    onClick: () -> Unit = {},
    animationsEnabled: Boolean = true
) {
    val cyberFont = rememberCyberpunkFontFamily()
    val time = if (animationsEnabled) rememberFrameTimeSec() else rememberUpdatedState(0f)
    val timeSec = time.value
    val t = if (animationsEnabled) cycle01(timeSec, 3.0f) else 0f
    val pulse = if (animationsEnabled) pulse01(timeSec, 1.5f) else 0f
    val spin = if (animationsEnabled) cycle01(timeSec, 0.9f) * 360f else 0f
    val mix by animateFloatAsState(if (syncing) 1f else 0f, if (animationsEnabled) tween(300) else snap(), label = "mix")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, if (animationsEnabled) tween(120) else snap(), label = "press")

    val angle = when {
        syncing -> spin
        pressed -> spin
        else -> 0f
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                val k = 1f - 0.03f * press
                scaleX = k
                scaleY = k
            }
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = !syncing,
                onClick = onClick
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = if (t in 0.62f..0.635f || t in 0.67f..0.68f) 0.45f else 1f
            val glow = (0.7f + 0.45f * pulse + 0.6f * press) * flick
            val accent = lerp(NeonCeleste, NeonPink, maxOf(mix, press * 0.6f))
            val pad = 8.dp.toPx()
            val fw = size.width - 2 * pad
            val fh = size.height - 2 * pad
            val frame = Path().apply {
                moveTo(pad, pad); lineTo(pad + fw, pad); lineTo(pad + fw, pad + fh); lineTo(pad, pad + fh); close()
            }
            clipPath(frame) {
                drawRect(Ink.copy(alpha = 0.82f), topLeft = Offset(pad, pad), size = Size(fw, fh))
                val band = 40.dp.toPx()
                val bx = pad + fw * t
                drawRect(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.25f), accent.copy(alpha = 0f)),
                        startX = bx - band,
                        endX = bx + band
                    ),
                    topLeft = Offset(bx - band, pad),
                    size = Size(band * 2, fh)
                )
            }
            neon(accent, 2.4.dp.toPx(), glow, flick) { c, st -> drawPath(frame, c, style = st) }
            val s = fh * 0.72f
            translate(pad + 4.dp.toPx(), (size.height - s) / 2f) {
                scale(s / 100f, Offset.Zero) { syncIcon(angle, pulse, glow, accent) }
            }
        }
        Text(
            text = if (syncing) syncingText else text,
            color = NeonCore,
            fontFamily = cyberFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(if (syncing || pressed) NeonPink else NeonCeleste, Offset.Zero, 20f)),
            modifier = Modifier.align(Alignment.Center).padding(start = 44.dp, end = 12.dp)
        )
    }
}

private inline fun neon(color: Color, width: Float, glow: Float = 1f, alpha: Float = 1f, layers: Int = 4, draw: (Color, Stroke) -> Unit) {
    for (i in layers downTo 1) {
        draw(color.copy(alpha = (0.09f * glow * alpha).coerceIn(0f, 1f)), Stroke(width * (1f + i * 1.2f), cap = StrokeCap.Square, join = StrokeJoin.Miter))
    }
    draw(color.copy(alpha = alpha), Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    draw(NeonCore.copy(alpha = 0.85f * alpha), Stroke(width * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.syncIcon(angle: Float, pulse: Float, glow: Float, accent: Color) {
    val c = Offset(50f, 50f)
    val r = 30f
    drawCircle(NeonPink.copy(alpha = 0.10f + 0.20f * pulse), 42f, c)
    rotate(angle, c) {
        for (k in 0..1) {
            val start = 25f + k * 180f
            val sweep = 130f
            neon(accent, 5f, glow, layers = 3) { col, st ->
                drawArc(col, start, sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = st)
            }
            val a = (start + sweep) * PI.toFloat() / 180f
            val nrm = Offset(cos(a), sin(a))
            val dir = Offset(-sin(a), cos(a))
            val tip = c + nrm * r
            val head = Path().apply {
                val p1 = tip + dir * 13f; val p2 = tip + nrm * 10f; val p3 = tip - nrm * 10f
                moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
            }
            drawPath(head, Ink)
            neon(accent, 3.4f, glow, layers = 2) { col, st -> drawPath(head, col, style = st) }
        }
    }
    drawCircle(NeonPink.copy(alpha = 0.3f + 0.4f * pulse), 8f, c)
    drawCircle(NeonCore, 3.4f, c)
}
