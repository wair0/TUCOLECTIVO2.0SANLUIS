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
private val CardFill = Color(0xFF0B1628)
private val TAU = (2.0 * PI).toFloat()

@Composable
fun NeonMenuScreen(
    onLineas: () -> Unit = {},
    onMapa: () -> Unit = {},
    onParadas: () -> Unit = {},
    onFavoritos: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "homeHud")
    val sweep by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "homeSweep")
    val pulse by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "homePulse")
    val gridShift by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "gridShift")

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Ink.copy(alpha = 0.50f))
            val step = 20.dp.toPx()
            val shift = gridShift * step
            val gridA = 0.18f + 0.10f * pulse
            var x = -step + shift
            while (x < size.width + step) {
                drawLine(NeonCeleste.copy(alpha = gridA), Offset(x, 0f), Offset(x, size.height), 1.6f)
                x += step
            }
            var y = -step + shift * 0.6f
            while (y < size.height + step) {
                drawLine(NeonCeleste.copy(alpha = gridA * 0.65f), Offset(0f, y), Offset(size.width, y), 1.4f)
                y += step
            }
            val bandH = 56.dp.toPx()
            val sy = size.height * sweep
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, NeonPink.copy(alpha = 0.35f + 0.20f * pulse), NeonCeleste.copy(alpha = 0.28f), Color.Transparent)),
                Offset(0f, sy - bandH),
                Size(size.width, bandH * 2)
            )
            drawLine(NeonPink.copy(alpha = 0.90f), Offset(0f, sy), Offset(size.width, sy), 2.5.dp.toPx())
            for (i in 0..12) {
                val px = size.width * ((sweep * 0.55f + i * 0.09f) % 1f)
                val py = 10.dp.toPx() + i * 24.dp.toPx()
                drawCircle(NeonCeleste.copy(alpha = 0.45f + 0.30f * pulse), 3.dp.toPx() + pulse * 2.5.dp.toPx(), Offset(px, py))
            }
            val br = 24.dp.toPx()
            val thick = 3.dp.toPx()
            val col = NeonPink.copy(alpha = 0.80f + 0.20f * pulse)
            drawLine(col, Offset(8f, 8f), Offset(8f + br, 8f), thick)
            drawLine(col, Offset(8f, 8f), Offset(8f, 8f + br), thick)
            drawLine(col, Offset(size.width - 8f - br, 8f), Offset(size.width - 8f, 8f), thick)
            drawLine(col, Offset(size.width - 8f, 8f), Offset(size.width - 8f, 8f + br), thick)
            drawLine(col, Offset(8f, size.height - 8f), Offset(8f + br, size.height - 8f), thick)
            drawLine(col, Offset(8f, size.height - 8f - br), Offset(8f, size.height - 8f), thick)
            drawLine(col, Offset(size.width - 8f - br, size.height - 8f), Offset(size.width - 8f, size.height - 8f), thick)
            drawLine(col, Offset(size.width - 8f, size.height - 8f - br), Offset(size.width - 8f, size.height - 8f), thick)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                NeonCard("LÍNEAS", Modifier.weight(1f), 0, onLineas, DrawScope::busIcon)
                NeonCard("MAPA", Modifier.weight(1f), 500, onMapa, DrawScope::mapIcon)
            }
            Row(Modifier.fillMaxWidth()) {
                NeonCard("PARADAS\nCERCANAS", Modifier.weight(1f), 1000, onParadas, DrawScope::stopIcon)
                NeonCard("FAVORITOS", Modifier.weight(1f), 1500, onFavoritos, DrawScope::starIcon)
            }
        }
    }
}

@Composable
fun NeonCard(
    title: String,
    modifier: Modifier = Modifier,
    phaseMs: Int = 0,
    onClick: () -> Unit = {},
    icon: DrawScope.(t: Float, pulse: Float, glow: Float) -> Unit
) {
    val cyberFont = rememberCyberpunkFontFamily()
    val infinite = rememberInfiniteTransition(label = "neon")
    val offset = StartOffset(phaseMs, StartOffsetType.FastForward)
    val t by infinite.animateFloat(0f, 1f, label = "t", animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), initialStartOffset = offset))
    val pulse by infinite.animateFloat(0f, 1f, label = "pulse", animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset))
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
    val scaleIn = 0.82f + 0.18f * appear.value

    Box(
        modifier.aspectRatio(1.18f).graphicsLayer {
            val k = (1f - 0.07f * press) * scaleIn
            scaleX = k; scaleY = k; alpha = appear.value
        }.clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = if (t in 0.62f..0.635f || t in 0.67f..0.68f) 0.5f else 1f
            val glow = (1.0f + 0.6f * pulse + 0.8f * press) * flick
            val pad = 7.dp.toPx()
            val fw = size.width - 2 * pad
            val fh = size.height - 2 * pad
            val frame = Path().apply {
                moveTo(pad, pad); lineTo(pad + fw, pad); lineTo(pad + fw, pad + fh); lineTo(pad, pad + fh); close()
            }
            clipPath(frame) {
                drawRect(CardFill)
                drawRect(Brush.verticalGradient(listOf(Color(0xFF123048), CardFill)))
                var y = pad
                while (y < pad + fh) {
                    drawLine(NeonCeleste.copy(alpha = 0.14f + 0.08f * pulse), Offset(pad, y), Offset(pad + fw, y), 1.2f)
                    y += 4.dp.toPx()
                }
                val band = 36.dp.toPx()
                val by = pad + fh * t
                drawRect(
                    Brush.verticalGradient(listOf(NeonCeleste.copy(alpha = 0f), NeonCeleste.copy(alpha = 0.45f), NeonPink.copy(alpha = 0.28f), NeonCeleste.copy(alpha = 0f))),
                    topLeft = Offset(pad, by - band),
                    size = Size(fw, band * 2)
                )
            }
            neon(NeonCeleste, 3.2.dp.toPx(), glow, flick) { c, st -> drawPath(frame, c, style = st) }
            val scanX = pad + fw * t
            drawLine(NeonPink.copy(alpha = 1f), Offset(scanX, pad), Offset(scanX, pad + fh), 3.dp.toPx())
            drawLine(NeonPink.copy(alpha = 0.40f), Offset(scanX, pad), Offset(scanX, pad + fh), 10.dp.toPx())
            if (press > 0.01f) {
                val ripple = (1f - press) * 52.dp.toPx()
                drawCircle(NeonPink.copy(alpha = 0.5f * press), ripple, Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 3.5.dp.toPx()))
            }
            drawLine(NeonPink.copy(alpha = 0.90f + 0.10f * pulse), Offset(pad + 6.dp.toPx(), pad + 5.dp.toPx()), Offset(pad + 42.dp.toPx(), pad + 5.dp.toPx()), 3.dp.toPx())
            drawLine(NeonCeleste.copy(alpha = 0.90f), Offset(pad + fw - 42.dp.toPx(), pad + fh - 5.dp.toPx()), Offset(pad + fw - 6.dp.toPx(), pad + fh - 5.dp.toPx()), 3.dp.toPx())
            val measure = PathMeasure().apply { setPath(frame, true) }
            val head = measure.length * t
            val tail = measure.length * 0.22f
            val seg = Path()
            for (k in 0 until 6) {
                val from = head - tail * (k + 1) / 6f
                val to = head - tail * k / 6f
                seg.reset()
                when {
                    to <= 0f -> measure.getSegment(measure.length + from, measure.length + to, seg, true)
                    from >= 0f -> measure.getSegment(from, to, seg, true)
                    else -> {
                        measure.getSegment(measure.length + from, measure.length, seg, true)
                        measure.getSegment(0f, to, seg, true)
                    }
                }
                neon(NeonCeleste, 4.dp.toPx(), 2.2f, 1f - k / 6f, layers = 2) { c, st -> drawPath(seg, c, style = st) }
            }
            val s = fh * 0.40f
            translate((size.width - s) / 2f, pad + fh * 0.10f) {
                scale(s / 100f, Offset.Zero) { icon(t, pulse, glow) }
            }
        }
        Text(
            text = title,
            color = NeonCore,
            fontFamily = cyberFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 18f)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
        )
    }
}

private inline fun neon(color: Color, width: Float, glow: Float = 1f, alpha: Float = 1f, layers: Int = 4, draw: (Color, Stroke) -> Unit) {
    for (i in layers downTo 1) {
        draw(color.copy(alpha = (0.12f * glow * alpha).coerceIn(0f, 1f)), Stroke(width * (1f + i * 1.3f), cap = StrokeCap.Square, join = StrokeJoin.Miter))
    }
    draw(color.copy(alpha = alpha), Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    draw(NeonCore.copy(alpha = 0.9f * alpha), Stroke(width * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.busIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.95f * glow
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawRoundRect(c, Offset(8f, 22f), Size(84f, 46f), CornerRadius(0f), style = st) }
    neon(NeonCeleste, 3.4f, glow, a, 2) { c, st -> for (x in listOf(16f, 39f, 62f)) drawRoundRect(c, Offset(x, 32f), Size(17f, 15f), CornerRadius(0f), style = st) }
    drawLine(NeonPink.copy(alpha = a), Offset(16f, 58f), Offset(58f, 58f), 3.4f, StrokeCap.Round)
    for (cx in listOf(28f, 72f)) {
        val ctr = Offset(cx, 72f)
        drawCircle(Ink, 9f, ctr)
        neon(NeonCeleste, 5f, glow, a, 2) { c, st -> drawCircle(c, 9f, ctr, style = st) }
    }
    drawCircle(NeonPink.copy(alpha = 0.55f + 0.45f * pulse), 4f, Offset(84f, 56f))
}

private fun DrawScope.mapIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.95f * glow
    val map = Path().apply {
        moveTo(8f, 32f); lineTo(36f, 24f); lineTo(64f, 32f); lineTo(92f, 24f)
        lineTo(92f, 74f); lineTo(64f, 82f); lineTo(36f, 74f); lineTo(8f, 82f); close()
    }
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawPath(map, c, style = st) }
    neon(NeonCeleste, 3f, glow, a * 0.8f, 2) { c, st ->
        drawLine(c, Offset(36f, 24f), Offset(36f, 74f), st.width, StrokeCap.Round)
        drawLine(c, Offset(64f, 32f), Offset(64f, 82f), st.width, StrokeCap.Round)
    }
    drawCircle(NeonPink.copy(alpha = 0.95f), 8f + 3f * pulse, Offset(50f, 54f))
    drawCircle(NeonCore, 3f, Offset(50f, 54f))
}

private fun DrawScope.stopIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.95f * glow
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawCircle(c, 22f, Offset(50f, 42f), style = st) }
    drawCircle(NeonPink.copy(alpha = 0.6f + 0.4f * pulse), 8f + 4f * pulse, Offset(50f, 42f))
    neon(NeonCeleste, 4f, glow, a, 2) { c, st ->
        drawLine(c, Offset(50f, 64f), Offset(50f, 86f), st.width, StrokeCap.Round)
        drawLine(c, Offset(38f, 86f), Offset(62f, 86f), st.width, StrokeCap.Round)
    }
}

private fun DrawScope.starIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.9f * glow + 0.1f * pulse
    val mid = Offset(50f, 48f)
    val star = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 28f * (0.92f + 0.08f * pulse) else 12f
            val ang = -TAU / 4f + i * TAU / 10f
            val x = mid.x + r * cos(ang)
            val y = mid.y + r * sin(ang)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, NeonPink.copy(alpha = 0.25f * glow))
    neon(NeonPink, 4f, glow, a, 3) { c, st -> drawPath(star, c, style = st) }
}
