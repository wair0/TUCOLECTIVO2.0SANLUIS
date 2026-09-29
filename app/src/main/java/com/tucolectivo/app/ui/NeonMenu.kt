package com.tucolectivo.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Panel = Color(0xFF050714)

@Composable
fun NeonMenuScreen(
    onLineas: () -> Unit = {},
    onMapa: () -> Unit = {},
    onParadas: () -> Unit = {},
    onFavoritos: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonCard("LÍNEAS", Modifier.weight(1f).height(112.dp), 0, onLineas, DrawScope::busIcon)
            NeonCard("MAPA", Modifier.weight(1f).height(112.dp), 750, onMapa, DrawScope::mapIcon)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonCard("PARADAS\nCERCANAS", Modifier.weight(1f).height(112.dp), 1500, onParadas, DrawScope::stopIcon)
            NeonCard("FAVORITOS", Modifier.weight(1f).height(112.dp), 2250, onFavoritos, DrawScope::starIcon)
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
    val infinite = rememberInfiniteTransition(label = "neon")
    val offset = StartOffset(phaseMs, StartOffsetType.FastForward)
    val breath by infinite.animateFloat(
        0f, 1f, label = "breath",
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset)
    )
    val pulse by infinite.animateFloat(
        0f, 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset)
    )
    val t by infinite.animateFloat(
        0f, 1f, label = "t",
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), initialStartOffset = offset)
    )
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")
    val font = rememberCyberpunkFontFamily()

    Box(
        modifier
            .graphicsLayer { val k = 1f - 0.04f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = when {
                t in 0.58f..0.595f -> 0.35f
                t in 0.62f..0.628f -> 0.55f
                t in 0.71f..0.718f -> 0.4f
                else -> 1f
            }
            val slow = 0.55f + 0.45f * breath
            val fast = 0.4f + 0.6f * pulse
            val glow = (0.35f * slow + 0.65f * fast + 0.55f * press) * flick

            val pad = 6.dp.toPx()
            val left = pad; val top = pad
            val right = size.width - pad; val bottom = size.height - pad
            val cut = 9.dp.toPx()

            drawRect(Panel.copy(alpha = 0.88f), Offset(left, top), Size(right - left, bottom - top))

            val frame = Path().apply {
                moveTo(left + cut, top); lineTo(right - cut, top); lineTo(right, top + cut)
                lineTo(right, bottom - cut); lineTo(right - cut, bottom); lineTo(left + cut, bottom)
                lineTo(left, bottom - cut); lineTo(left, top + cut); close()
            }
            drawPath(frame, NeonCeleste.copy(alpha = 0.08f * glow), style = Stroke(width = 10.dp.toPx()))
            drawPath(frame, NeonCeleste.copy(alpha = 0.18f * glow), style = Stroke(width = 5.dp.toPx()))
            drawPath(frame, NeonCeleste.copy(alpha = 0.55f + 0.35f * pulse * flick), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

            val bLen = 12.dp.toPx() * (0.85f + 0.15f * pulse)
            val bW = 2.2.dp.toPx()
            listOf(
                Offset(left, top) to listOf(Offset(bLen, 0f), Offset(0f, bLen)),
                Offset(right, top) to listOf(Offset(-bLen, 0f), Offset(0f, bLen)),
                Offset(left, bottom) to listOf(Offset(bLen, 0f), Offset(0f, -bLen)),
                Offset(right, bottom) to listOf(Offset(-bLen, 0f), Offset(0f, -bLen))
            ).forEach { (origin, dirs) ->
                dirs.forEach { d ->
                    drawLine(NeonPink.copy(alpha = 0.55f + 0.4f * pulse), origin, origin + d, strokeWidth = bW, cap = StrokeCap.Round)
                }
            }

            val scanY = top + (bottom - top) * ((t + 0.3f * sin(t * 6.28f).toFloat()) % 1f)
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, NeonCeleste.copy(alpha = 0.25f * glow), Color.Transparent)),
                Offset(left, scanY), Offset(right, scanY), strokeWidth = 1.2.dp.toPx()
            )

            val s = size.minDimension * 0.42f
            translate((size.width - s) / 2f, size.height * 0.10f) {
                scale(s / 100f, Offset.Zero) { icon(t, pulse, glow) }
            }
        }

        Text(
            text = title, color = NeonCore, fontFamily = font, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.1.sp, textAlign = TextAlign.Center, lineHeight = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp, start = 6.dp, end = 6.dp)
        )
    }
}

private fun DrawScope.busIcon(t: Float, pulse: Float, glow: Float) {
    val c = NeonCeleste.copy(alpha = 0.9f * glow)
    val body = Path().apply {
        moveTo(18f, 38f); lineTo(82f, 38f); lineTo(88f, 48f); lineTo(88f, 70f); lineTo(78f, 70f); lineTo(78f, 78f)
        lineTo(68f, 78f); lineTo(68f, 70f); lineTo(32f, 70f); lineTo(32f, 78f); lineTo(22f, 78f); lineTo(22f, 70f)
        lineTo(12f, 70f); lineTo(12f, 48f); close()
    }
    drawPath(body, c, style = Stroke(2.2f, cap = StrokeCap.Round))
    drawLine(c, Offset(18f, 52f), Offset(82f, 52f), 1.6f)
    drawCircle(NeonPink.copy(alpha = 0.7f + 0.3f * pulse), 3.5f, Offset(28f, 62f))
    drawCircle(NeonPink.copy(alpha = 0.7f + 0.3f * pulse), 3.5f, Offset(72f, 62f))
    drawLine(NeonCeleste.copy(alpha = 0.55f), Offset(0f, 83f), Offset(100f, 83f), 1.6f)
}

private fun DrawScope.mapIcon(t: Float, pulse: Float, glow: Float) {
    val c = NeonCeleste.copy(alpha = 0.9f * glow)
    val map = Path().apply {
        moveTo(12f, 22f); lineTo(38f, 16f); lineTo(62f, 24f); lineTo(88f, 16f)
        lineTo(88f, 78f); lineTo(62f, 86f); lineTo(38f, 78f); lineTo(12f, 86f); close()
    }
    drawPath(map, c, style = Stroke(2f, cap = StrokeCap.Round))
    drawLine(c, Offset(38f, 16f), Offset(38f, 78f), 1.4f)
    drawLine(c, Offset(62f, 24f), Offset(62f, 86f), 1.4f)
    drawCircle(NeonPink.copy(alpha = 0.85f + 0.15f * pulse), 6f + 2f * pulse, Offset(55f, 48f))
    drawCircle(NeonCore, 2.5f, Offset(55f, 48f))
}

private fun DrawScope.stopIcon(t: Float, pulse: Float, glow: Float) {
    val c = NeonCeleste.copy(alpha = 0.9f * glow)
    drawCircle(c, 22f, Offset(50f, 42f), style = Stroke(2.2f))
    drawCircle(NeonPink.copy(alpha = 0.5f + 0.4f * pulse), 6f + 3f * pulse, Offset(50f, 42f))
    drawLine(c, Offset(50f, 64f), Offset(50f, 86f), 2.2f, StrokeCap.Round)
    drawLine(c, Offset(38f, 86f), Offset(62f, 86f), 2f, StrokeCap.Round)
}

private fun DrawScope.starIcon(t: Float, pulse: Float, glow: Float) {
    val c = NeonPink.copy(alpha = 0.85f * glow + 0.15f * pulse)
    val star = Path().apply {
        val cx = 50f; val cy = 48f
        val outer = 28f * (0.92f + 0.08f * pulse); val inner = 12f
        for (i in 0 until 10) {
            val ang = Math.toRadians((-90 + i * 36).toDouble())
            val r = if (i % 2 == 0) outer else inner
            val x = cx + (r * kotlin.math.cos(ang)).toFloat()
            val y = cy + (r * kotlin.math.sin(ang)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, c, style = Stroke(2f, cap = StrokeCap.Round))
    drawPath(star, c.copy(alpha = 0.15f * glow))
}
