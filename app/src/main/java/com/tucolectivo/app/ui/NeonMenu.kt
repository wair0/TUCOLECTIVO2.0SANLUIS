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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val TAU = (2.0 * PI).toFloat()

@Composable
fun NeonMenuScreen(
    onLineas: () -> Unit = {},
    onMapa: () -> Unit = {},
    onParadas: () -> Unit = {},
    onFavoritos: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            NeonCard("LÍNEAS", Modifier.weight(1f), 0, onLineas, DrawScope::busIcon)
            NeonCard("MAPA", Modifier.weight(1f), 750, onMapa, DrawScope::mapIcon)
        }
        Row(Modifier.fillMaxWidth()) {
            NeonCard("PARADAS\nCERCANAS", Modifier.weight(1f), 1500, onParadas, DrawScope::stopIcon)
            NeonCard("FAVORITOS", Modifier.weight(1f), 2250, onFavoritos, DrawScope::starIcon)
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
    val t by infinite.animateFloat(
        0f, 1f, label = "t",
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), initialStartOffset = offset)
    )
    val pulse by infinite.animateFloat(
        0f, 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset)
    )
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")

    Box(
        modifier
            .aspectRatio(1.25f)
            .graphicsLayer { val k = 1f - 0.05f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = if (t in 0.62f..0.635f || t in 0.67f..0.68f) 0.45f else 1f
            val glow = (0.7f + 0.45f * pulse + 0.6f * press) * flick
            val pad = 10.dp.toPx()
            val fw = size.width - 2 * pad
            val fh = size.height - 2 * pad
            val frame = Path().apply { moveTo(pad, pad); lineTo(pad + fw, pad); lineTo(pad + fw, pad + fh); lineTo(pad, pad + fh); close() }
            clipPath(frame) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0B2236), Ink)))
                var y = pad
                while (y < pad + fh) {
                    drawLine(NeonCeleste.copy(alpha = 0.05f), Offset(pad, y), Offset(pad + fw, y), 1f)
                    y += 5.dp.toPx()
                }
                val band = 26.dp.toPx()
                val by = pad + fh * t
                drawRect(
                    Brush.verticalGradient(
                        listOf(NeonCeleste.copy(alpha = 0f), NeonCeleste.copy(alpha = 0.22f), NeonCeleste.copy(alpha = 0f))
                    ),
                    topLeft = Offset(pad, by - band), size = Size(fw, band * 2)
                )
            }
            neon(NeonCeleste, 2.2.dp.toPx(), glow, flick) { c, st -> drawPath(frame, c, style = st) }
            val measure = PathMeasure().apply { setPath(frame, true) }
            val head = measure.length * t
            val tail = measure.length * 0.14f
            val seg = Path()
            for (k in 0 until 5) {
                val from = head - tail * (k + 1) / 5f
                val to = head - tail * k / 5f
                seg.reset()
                when {
                    to <= 0f -> measure.getSegment(measure.length + from, measure.length + to, seg, true)
                    from >= 0f -> measure.getSegment(from, to, seg, true)
                    else -> {
                        measure.getSegment(measure.length + from, measure.length, seg, true)
                        measure.getSegment(0f, to, seg, true)
                    }
                }
                neon(NeonCeleste, 3.2.dp.toPx(), 1.8f, 1f - k / 5f, layers = 2) { c, st -> drawPath(seg, c, style = st) }
            }
            val s = fh * 0.42f
            translate((size.width - s) / 2f, pad + fh * 0.12f) {
                scale(s / 100f, Offset.Zero) { icon(t, pulse, glow) }
            }
        }
        Text(
            text = title, color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Bold,
            fontSize = 12.sp, letterSpacing = 1.2.sp, textAlign = TextAlign.Center, lineHeight = 14.sp,
            style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 14f)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, start = 8.dp, end = 8.dp)
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

private fun DrawScope.busIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.9f * glow
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawRoundRect(c, Offset(8f, 22f), Size(84f, 46f), CornerRadius(0f), style = st) }
    neon(NeonCeleste, 3.4f, glow, a, 2) { c, st ->
        for (x in listOf(16f, 39f, 62f)) drawRoundRect(c, Offset(x, 32f), Size(17f, 15f), CornerRadius(0f), style = st)
    }
    drawLine(NeonPink.copy(alpha = a), Offset(16f, 58f), Offset(58f, 58f), 3.4f, StrokeCap.Round)
    for (cx in listOf(28f, 72f)) {
        val ctr = Offset(cx, 72f)
        drawCircle(Ink, 9f, ctr)
        neon(NeonCeleste, 5f, glow, a, 2) { c, st -> drawCircle(c, 9f, ctr, style = st) }
    }
    drawCircle(NeonPink.copy(alpha = 0.4f + 0.6f * pulse), 3.4f, Offset(84f, 56f))
}

private fun DrawScope.mapIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.9f * glow
    val map = Path().apply {
        moveTo(8f, 32f); lineTo(36f, 24f); lineTo(64f, 32f); lineTo(92f, 24f)
        lineTo(92f, 74f); lineTo(64f, 82f); lineTo(36f, 74f); lineTo(8f, 82f); close()
    }
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawPath(map, c, style = st) }
    neon(NeonCeleste, 3f, glow, a * 0.8f, 2) { c, st ->
        drawLine(c, Offset(36f, 24f), Offset(36f, 74f), st.width, StrokeCap.Round)
        drawLine(c, Offset(64f, 32f), Offset(64f, 82f), st.width, StrokeCap.Round)
    }
    drawCircle(NeonPink.copy(alpha = 0.85f + 0.15f * pulse), 6f + 2f * pulse, Offset(50f, 54f))
    drawCircle(NeonCore, 2.5f, Offset(50f, 54f))
}

private fun DrawScope.stopIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.9f * glow
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawCircle(c, 22f, Offset(50f, 42f), style = st) }
    drawCircle(NeonPink.copy(alpha = 0.5f + 0.4f * pulse), 6f + 3f * pulse, Offset(50f, 42f))
    neon(NeonCeleste, 4f, glow, a, 2) { c, st ->
        drawLine(c, Offset(50f, 64f), Offset(50f, 86f), st.width, StrokeCap.Round)
        drawLine(c, Offset(38f, 86f), Offset(62f, 86f), st.width, StrokeCap.Round)
    }
}

private fun DrawScope.starIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.85f * glow + 0.15f * pulse
    val mid = Offset(50f, 48f)
    val star = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 28f * (0.92f + 0.08f * pulse) else 12f
            val ang = -TAU / 4f + i * TAU / 10f
            val x = mid.x + r * cos(ang); val y = mid.y + r * sin(ang)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, NeonPink.copy(alpha = 0.15f * glow))
    neon(NeonPink, 4f, glow, a, 3) { c, st -> drawPath(star, c, style = st) }
}

@Preview(showBackground = true, backgroundColor = 0xFF060912, widthDp = 380, heightDp = 700)
@Composable
private fun NeonMenuPreview() {
    NeonMenuScreen()
}
