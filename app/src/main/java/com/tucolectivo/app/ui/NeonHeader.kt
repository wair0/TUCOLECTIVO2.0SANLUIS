package com.tucolectivo.app.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val TAU = (2.0 * PI).toFloat()

data class NeonNotification(
    val title: String,
    val detail: String,
    val time: String,
    val unread: Boolean = true
)

@Composable
fun NeonHeader(
    modifier: Modifier = Modifier,
    title: String = "TU COLECTIVO 2.0",
    statusText: String = "● SISTEMA LISTO",
    menuOpen: Boolean = false,
    searchOpen: Boolean = false,
    notificationsOpen: Boolean = false,
    hasUnread: Boolean = false,
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val cyberFont = rememberCyberpunkFontFamily()
    val infinite = rememberInfiniteTransition(label = "header")
    val t by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "t")
    val pulse by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val breath by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")

    Box(modifier.fillMaxWidth().height(92.dp).zIndex(40f)) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = when {
                t in 0.58f..0.595f -> 0.35f
                t in 0.62f..0.628f -> 0.5f
                t in 0.71f..0.718f -> 0.4f
                else -> 1f
            }
            val glow = (0.45f + 0.3f * breath + 0.4f * pulse) * flick
            val w = size.width; val h = size.height; val y = h - 2.dp.toPx()
            drawRect(Ink.copy(alpha = 0.92f))
            val line = Path().apply {
                moveTo(0f, 1.dp.toPx()); lineTo(w, 1.dp.toPx()); lineTo(w, y); lineTo(0f, y); close()
            }
            neonStroke(NeonCeleste, 2.dp.toPx(), glow, flick) { c, st -> drawPath(line, c, style = st) }
            val m = PathMeasure().apply { setPath(line, false) }
            val head = m.length * t; val tail = m.length * 0.16f; val seg = Path()
            for (k in 0 until 5) {
                val from = (head - tail * (k + 1) / 5).coerceAtLeast(0f)
                val to = head - tail * k / 5
                if (to > from) {
                    seg.reset(); m.getSegment(from, to, seg, true)
                    neonStroke(NeonCeleste, 3.dp.toPx(), 1.8f, 1f - k / 5f, layers = 2) { c, st -> drawPath(seg, c, style = st) }
                }
            }
            for (i in 0..2) {
                val on = (t * 9f).toInt() % 3 == i
                drawRect(NeonPink.copy(alpha = if (on) 1f else 0.3f), Offset(w / 2f - 15.dp.toPx() + i * 11.dp.toPx(), y - 1.5.dp.toPx()), Size(6.dp.toPx(), 3.dp.toPx()))
            }
        }

        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
            HeaderIconButton(active = menuOpen, onClick = onMenuClick) { _, p, g, o -> menuIcon(p, g, o) }
            Spacer(Modifier.weight(1f))
            HeaderIconButton(active = searchOpen, phaseMs = 500, onClick = onSearchClick) { tt, p, g, o -> searchIcon(tt, p, g, o) }
            Spacer(Modifier.width(8.dp))
            HeaderIconButton(active = notificationsOpen, phaseMs = 1000, badge = hasUnread, onClick = onNotificationsClick) { tt, p, g, o -> bellIcon(tt, p, g, o, hasUnread) }
        }

        Column(Modifier.align(Alignment.TopCenter).padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Text(text = title, color = NeonPink.copy(alpha = 0.45f), fontFamily = cyberFont, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.5.sp, maxLines = 1, softWrap = false, modifier = Modifier.offset(x = (-1).dp, y = 0.5.dp).graphicsLayer { alpha = if (t in 0.58f..0.595f || t in 0.67f..0.68f) 0.7f else 0.12f })
                Text(text = title, color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.5.sp, maxLines = 1, softWrap = false, style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 24f)), modifier = Modifier.graphicsLayer { alpha = if (t in 0.62f..0.635f || t in 0.71f..0.718f) 0.62f else 1f })
            }
            Spacer(Modifier.height(3.dp))
            Text(text = statusText, color = NeonCeleste, fontFamily = cyberFont, fontWeight = FontWeight.Black, fontSize = 9.5.sp, letterSpacing = 1.1.sp, maxLines = 1, softWrap = false, style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 12f)), modifier = Modifier.graphicsLayer { alpha = 0.72f + 0.28f * pulse })
        }
    }
}

@Composable
private fun HeaderIconButton(
    active: Boolean,
    phaseMs: Int = 0,
    badge: Boolean = false,
    onClick: () -> Unit,
    icon: DrawScope.(t: Float, pulse: Float, glow: Float, open: Float) -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "btn")
    val offset = StartOffset(phaseMs, StartOffsetType.FastForward)
    val t by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), initialStartOffset = offset), label = "t")
    val pulse by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset), label = "pulse")
    val open by animateFloatAsState(if (active) 1f else 0f, tween(220), label = "open")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")

    Box(
        Modifier.size(48.dp).graphicsLayer { val k = 1f - 0.08f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = when { t in 0.58f..0.595f -> 0.35f; t in 0.62f..0.628f -> 0.5f; else -> 1f }
            val glow = (0.7f + 0.45f * pulse + 0.6f * press + 0.4f * open) * flick
            val accent = lerp(NeonCeleste, NeonPink, open)
            val pad = 4.dp.toPx(); val w = size.width - 2 * pad; val h = size.height - 2 * pad
            val frame = Path().apply { moveTo(pad, pad); lineTo(pad + w, pad); lineTo(pad + w, pad + h); lineTo(pad, pad + h); close() }
            clipPath(frame) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0B2236), Ink)))
                drawRect(accent.copy(alpha = 0.22f * open))
            }
            neonStroke(accent, 1.8.dp.toPx(), glow, flick) { c, st -> drawPath(frame, c, style = st) }
            val s = size.width * 0.52f
            translate((size.width - s) / 2f, (size.height - s) / 2f) { scale(s / 100f, Offset.Zero) { icon(t, pulse, glow, open) } }
            if (badge) {
                val bc = Offset(pad + w - 2.dp.toPx(), pad + 2.dp.toPx())
                drawCircle(NeonPink.copy(alpha = 0.35f + 0.35f * pulse), 7.dp.toPx(), bc)
                drawCircle(NeonPink, 3.5.dp.toPx(), bc)
            }
        }
    }
}

private inline fun neonStroke(color: Color, width: Float, glow: Float = 1f, alpha: Float = 1f, layers: Int = 4, draw: (Color, Stroke) -> Unit) {
    for (i in layers downTo 1) draw(color.copy(alpha = (0.09f * glow * alpha).coerceIn(0f, 1f)), Stroke(width * (1f + i * 1.2f), cap = StrokeCap.Square, join = StrokeJoin.Miter))
    draw(color.copy(alpha = alpha), Stroke(width, cap = StrokeCap.Square, join = StrokeJoin.Miter))
    draw(NeonCore.copy(alpha = 0.85f * alpha), Stroke(width * 0.35f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
}

private fun DrawScope.menuIcon(pulse: Float, glow: Float, open: Float) {
    val col = lerp(NeonCeleste, NeonPink, open)
    for (i in 0..2) {
        val y = 50f + (i - 1) * 24f * (1f - open)
        val len = if (i == 1) 44f else 64f
        val alpha = if (i == 1) 1f - open else 1f
        rotate((1 - i) * 45f * open, Offset(50f, y)) {
            neonStroke(col, 8f, glow, alpha, 3) { c, st -> drawLine(c, Offset(50f - len / 2f, y), Offset(50f + len / 2f, y), st.width, StrokeCap.Round) }
        }
    }
    drawRect(NeonPink.copy(alpha = (0.4f + 0.6f * pulse) * (1f - open)), Offset(78f, 47f), Size(8f, 6f))
}

private fun DrawScope.searchIcon(t: Float, pulse: Float, glow: Float, open: Float) {
    val col = lerp(NeonCeleste, NeonPink, open)
    val c = Offset(42f, 42f); val r = 24f
    drawCircle(Ink, r, c)
    neonStroke(col, 8f, glow, layers = 3) { color, st ->
        drawCircle(color, r, c, style = st)
        drawLine(color, Offset(59f, 59f), Offset(86f, 86f), st.width, StrokeCap.Round)
    }
    val a = t * TAU
    drawLine(NeonPink, c, c + Offset(cos(a), sin(a)) * (r - 7f), 4f, StrokeCap.Round)
    drawCircle(NeonPink.copy(alpha = 0.3f + 0.5f * pulse), 5f, c)
}

private fun DrawScope.bellIcon(t: Float, pulse: Float, glow: Float, open: Float, ringing: Boolean) {
    val col = lerp(NeonCeleste, NeonPink, open)
    val k = if (ringing && t < 0.35f) 1f - t / 0.35f else 0f
    rotate(sin(t * TAU * 6f) * 9f * k, Offset(50f, 14f)) {
        val body = Path().apply {
            moveTo(24f, 70f); cubicTo(32f, 62f, 32f, 50f, 32f, 42f)
            cubicTo(32f, 28f, 40f, 20f, 50f, 20f); cubicTo(60f, 20f, 68f, 28f, 68f, 42f)
            cubicTo(68f, 50f, 68f, 62f, 76f, 70f); close()
        }
        neonStroke(col, 6f, glow, layers = 3) { c, st -> drawPath(body, c, style = st) }
        drawLine(col, Offset(44f, 20f), Offset(56f, 20f), 5f, StrokeCap.Round)
        drawArc(col, 20f, 140f, false, Offset(38f, 66f), Size(24f, 16f), style = Stroke(5f))
    }
}
