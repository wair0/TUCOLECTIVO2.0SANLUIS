package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
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
    Box(Modifier.fillMaxSize()) {
        // El fondo general ya es estático; las tarjetas tampoco tienen escáner animado.
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                NeonCard("LÍNEAS", Modifier.weight(1f), onClick = onLineas, icon = DrawScope::busIcon)
                NeonCard("MAPA", Modifier.weight(1f), onClick = onMapa, icon = DrawScope::mapIcon)
            }
            Row(Modifier.fillMaxWidth()) {
                NeonCard("PARADAS\nCERCANAS", Modifier.weight(1f), onClick = onParadas, icon = DrawScope::stopIcon)
                NeonCard("FAVORITOS", Modifier.weight(1f), onClick = onFavoritos, icon = DrawScope::starIcon)
            }
        }
    }
}

@Composable
fun NeonCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: DrawScope.(t: Float, pulse: Float, glow: Float) -> Unit
) {
    val cyberFont = rememberMenuCyberpunkFontFamily()
    val source = remember { MutableInteractionSource() }

    Box(
        modifier
            .aspectRatio(1.18f)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = 0f
            val pulse = 0.5f
            val glow = 1f
            val pad = 7.dp.toPx()
            val fw = size.width - 2 * pad
            val fh = size.height - 2 * pad
            val frame = Path().apply {
                moveTo(pad, pad)
                lineTo(pad + fw, pad)
                lineTo(pad + fw, pad + fh)
                lineTo(pad, pad + fh)
                close()
            }

            // Tarjeta fija: sin escáner, pulso, entrada, rebote ni ripple.
            clipPath(frame) {
                drawRect(CardFill)
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF123048), CardFill))
                )
                var y = pad
                while (y < pad + fh) {
                    drawLine(
                        NeonCeleste.copy(alpha = 0.12f),
                        Offset(pad, y),
                        Offset(pad + fw, y),
                        1.2f
                    )
                    y += 4.dp.toPx()
                }
            }

            neon(NeonCeleste, 3.2.dp.toPx(), glow, 1f, 4) { c, st ->
                drawPath(frame, c, style = st)
            }

            drawLine(
                NeonPink.copy(alpha = 0.90f),
                Offset(pad + 6.dp.toPx(), pad + 5.dp.toPx()),
                Offset(pad + 42.dp.toPx(), pad + 5.dp.toPx()),
                3.dp.toPx()
            )
            drawLine(
                NeonCeleste.copy(alpha = 0.90f),
                Offset(pad + fw - 42.dp.toPx(), pad + fh - 5.dp.toPx()),
                Offset(pad + fw - 6.dp.toPx(), pad + fh - 5.dp.toPx()),
                3.dp.toPx()
            )

            val iconSize = fh * 0.40f
            translate((size.width - iconSize) / 2f, pad + fh * 0.10f) {
                scale(iconSize / 100f, Offset.Zero) {
                    icon(t, pulse, glow)
                }
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
        )
    }
}

private fun DrawScope.busIcon(t: Float, pulse: Float, glow: Float) {
    val a = 0.95f * glow
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawRoundRect(c, Offset(8f, 22f), Size(84f, 46f), CornerRadius(0f), style = st) }
    neon(NeonCeleste, 3.4f, glow, a, 2) { c, st -> for (x in listOf(16f, 39f, 62f)) drawRoundRect(c, Offset(x, 32f), Size(17f, 15f), CornerRadius(0f), style = st) }
    drawLine(NeonPink.copy(alpha = a), Offset(16f, 58f), Offset(58f, 58f), 3.4f, StrokeCap.Round)
    for (cx in listOf(28f, 72f)) {
        val ctr = Offset(cx, 72f); drawCircle(Ink, 9f, ctr)
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
            val x = mid.x + r * cos(ang); val y = mid.y + r * sin(ang)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, NeonPink.copy(alpha = 0.25f * glow))
    neon(NeonPink, 4f, glow, a, 3) { c, st -> drawPath(star, c, style = st) }
}


private fun DrawScope.neon(
    color: Color,
    width: Float,
    glow: Float,
    alpha: Float,
    layers: Int,
    draw: DrawScope.(Color, Stroke) -> Unit
) {
    val safeGlow = glow.coerceIn(0f, 1f)
    for (layer in layers downTo 1) {
        val spread = width * (1f + layer * 0.9f)
        val a = (alpha * safeGlow * (0.035f + layer * 0.025f)).coerceIn(0f, 1f)
        draw(color.copy(alpha = a), Stroke(width = spread, cap = StrokeCap.Square, join = StrokeJoin.Miter))
    }
    draw(
        color.copy(alpha = alpha.coerceIn(0f, 1f)),
        Stroke(width = width, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )
}


@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberMenuCyberpunkFontFamily(): androidx.compose.ui.text.font.FontFamily {
    val assets = LocalContext.current.assets
    return androidx.compose.ui.text.font.FontFamily(
        androidx.compose.ui.text.font.Font(
            path = "fonts/cyberpunk.ttf",
            assetManager = assets,
            weight = androidx.compose.ui.text.font.FontWeight.Normal
        )
    )
}
