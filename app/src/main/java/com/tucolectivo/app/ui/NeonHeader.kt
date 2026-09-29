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
private val NeonPurple = Color(0xFF9D45FF)
private val NeonGreen = Color(0xFF25FFB7)
private val Ink = Color(0xFF030712)
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
    val t by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "scan"
    )
    val pulse by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1050, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val breath by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

    Box(modifier.fillMaxWidth().height(92.dp).zIndex(40f)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val inset = 5.dp.toPx()
            val glow = 0.72f + 0.42f * pulse + 0.22f * breath
            val flick = when {
                t in 0.23f..0.242f -> 0.38f
                t in 0.51f..0.522f -> 0.55f
                t in 0.78f..0.789f -> 0.42f
                else -> 1f
            }

            drawRect(Ink)

            // HUD grid interior.
            for (x in 0..12) {
                val xx = w * x / 12f
                drawLine(
                    NeonCeleste.copy(alpha = 0.035f),
                    Offset(xx, inset + 1.dp.toPx()),
                    Offset(xx, h - inset - 1.dp.toPx()),
                    1f
                )
            }
            for (y in 0..4) {
                val yy = h * y / 4f
                drawLine(
                    NeonPurple.copy(alpha = 0.035f),
                    Offset(inset, yy),
                    Offset(w - inset, yy),
                    1f
                )
            }

            // Rectangular frame: no cut/diagonal corners.
            val frame = Rect(inset, inset, w - inset, h - inset)
            neonStroke(NeonCeleste, 2.2.dp.toPx(), glow, flick, layers = 5) { c, st ->
                drawRect(c, topLeft = Offset(frame.left, frame.top), size = Size(frame.width, frame.height), style = st)
            }
            drawRect(
                NeonPink.copy(alpha = 0.2f + 0.25f * pulse),
                Offset(inset + 2.dp.toPx(), inset + 2.dp.toPx()),
                Size(w - 2 * inset - 4.dp.toPx(), 1.5.dp.toPx())
            )

            // Moving energy scan around the perimeter.
            val scanX = inset + (w - 2 * inset) * t
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, NeonCeleste.copy(alpha = 0.9f), Color.Transparent)
                ),
                Offset(scanX - 28.dp.toPx(), inset),
                Size(56.dp.toPx(), 2.5.dp.toPx())
            )

            // Segmented cyber bars around the center HUD.
            val barY = h - 14.dp.toPx()
            for (i in 0..7) {
                val active = ((t * 8f).toInt() + i) % 8 < 3
                val c = if (i % 2 == 0) NeonCeleste else NeonPink
                drawRect(
                    c.copy(alpha = if (active) 0.95f else 0.22f),
                    Offset(w * 0.30f + i * 9.dp.toPx(), barY),
                    Size(6.dp.toPx(), 3.dp.toPx())
                )
                drawRect(
                    c.copy(alpha = if (active) 0.95f else 0.22f),
                    Offset(w * 0.70f - i * 9.dp.toPx(), barY),
                    Size(6.dp.toPx(), 3.dp.toPx())
                )
            }

            // Dynamic status HUD plate.
            val plate = Rect(
                w * 0.365f,
                h * 0.61f,
                w * 0.635f,
                h * 0.91f
            )
            drawRoundRect(
                Brush.horizontalGradient(
                    listOf(NeonCeleste.copy(alpha = 0.08f), Ink.copy(alpha = 0.96f), NeonPink.copy(alpha = 0.08f))
                ),
                plate,
                cornerRadius = CornerRadius(5.dp.toPx())
            )
            neonStroke(NeonCeleste, 1.2.dp.toPx(), glow, 0.9f, layers = 3) { c, st ->
                drawRoundRect(c, topLeft = Offset(plate.left, plate.top), size = Size(plate.width, plate.height), cornerRadius = CornerRadius(5.dp.toPx()), style = st)
            }
            drawCircle(
                NeonGreen.copy(alpha = 0.25f + 0.45f * pulse),
                8.dp.toPx(),
                Offset(w * 0.395f, h * 0.765f)
            )
            drawCircle(NeonGreen, 3.2.dp.toPx(), Offset(w * 0.395f, h * 0.765f))

            // Tiny circuit traces.
            val traceY = h * 0.48f
            drawLine(NeonCeleste.copy(alpha = 0.6f), Offset(w * 0.23f, traceY), Offset(w * 0.34f, traceY), 1.5.dp.toPx())
            drawLine(NeonPink.copy(alpha = 0.6f), Offset(w * 0.66f, traceY), Offset(w * 0.77f, traceY), 1.5.dp.toPx())
            drawCircle(NeonCeleste, 2.dp.toPx(), Offset(w * 0.23f, traceY))
            drawCircle(NeonPink, 2.dp.toPx(), Offset(w * 0.77f, traceY))
        }

        // Interactive surfaces remain Compose controls so the three contextual menus keep working.
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(active = menuOpen, onClick = onMenuClick) { _, _, _, _ -> }
            Spacer(Modifier.weight(1f))
            HeaderIconButton(active = searchOpen, phaseMs = 500, onClick = onSearchClick) { _, _, _, _ -> }
            Spacer(Modifier.width(6.dp))
            HeaderIconButton(
                active = notificationsOpen,
                phaseMs = 1000,
                badge = hasUnread,
                onClick = onNotificationsClick
            ) { _, _, _, _ -> }
        }

        // Bus glyph and title are rendered independently from the clickable buttons.
        Canvas(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val busCenter = Offset(size.width * 0.285f, size.height * 0.52f)
            drawBusGlyph(busCenter, 0.88f + 0.12f * pulse, glow = 0.7f + 0.4f * pulse)
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .offset(x = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Text(
                    title,
                    color = NeonPink.copy(alpha = 0.35f),
                    fontFamily = cyberFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.45.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = (-1.5).dp, y = 1.dp)
                )
                Text(
                    title,
                    color = NeonCore,
                    fontFamily = cyberFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.45.sp,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 20f)),
                    modifier = Modifier.graphicsLayer {
                        alpha = if (t in 0.51f..0.522f) 0.62f else 1f
                    }
                )
            }
            Spacer(Modifier.height(13.dp))
            Text(
                statusText,
                color = NeonGreen,
                fontFamily = cyberFont,
                fontWeight = FontWeight.Black,
                fontSize = 8.5.sp,
                letterSpacing = 0.9.sp,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(shadow = Shadow(NeonGreen, Offset.Zero, 10f)),
                modifier = Modifier.graphicsLayer { alpha = 0.72f + 0.28f * pulse }
            )
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
    val infinite = rememberInfiniteTransition(label = "header_btn")
    val offset = StartOffset(phaseMs, StartOffsetType.FastForward)
    val t by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), initialStartOffset = offset),
        label = "t"
    )
    val pulse by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset),
        label = "pulse"
    )
    val open by animateFloatAsState(if (active) 1f else 0f, tween(220), label = "open")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")

    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer {
                val k = 1f - 0.08f * press
                scaleX = k
                scaleY = k
            }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flick = if (t in 0.58f..0.595f) 0.4f else 1f
            val glow = (0.72f + 0.5f * pulse + 0.7f * press + 0.45f * open) * flick
            val accent = lerp(NeonCeleste, NeonPink, open)
            val pad = 3.dp.toPx()
            val frame = Rect(pad, pad, size.width - pad, size.height - pad)

            // Transparent functional hit surface with active-state neon, while the Canvas header provides the icons.
            neonStroke(accent, 1.5.dp.toPx(), glow, if (active) 0.95f else 0.28f, layers = 3) { c, st ->
                drawRect(frame, c, style = st)
            }
            if (pressed) {
                drawCircle(accent.copy(alpha = 0.16f), 22.dp.toPx(), center = center)
            }
            if (badge) {
                drawCircle(NeonPink.copy(alpha = 0.3f + 0.4f * pulse), 6.dp.toPx(), Offset(size.width - 4.dp.toPx(), 5.dp.toPx()))
                drawCircle(NeonPink, 3.dp.toPx(), Offset(size.width - 4.dp.toPx(), 5.dp.toPx()))
            }
        }
    }
}

private inline fun neonStroke(
    color: Color,
    width: Float,
    glow: Float = 1f,
    alpha: Float = 1f,
    layers: Int = 4,
    draw: (Color, Stroke) -> Unit
) {
    for (i in layers downTo 1) {
        draw(
            color.copy(alpha = (0.08f * glow * alpha).coerceIn(0f, 1f)),
            Stroke(width * (1f + i * 1.25f), cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
    }
    draw(color.copy(alpha = alpha), Stroke(width, cap = StrokeCap.Square, join = StrokeJoin.Miter))
    draw(NeonCore.copy(alpha = 0.8f * alpha), Stroke(width * 0.32f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
}

private fun DrawScope.drawBusGlyph(center: Offset, scale: Float, glow: Float) {
    val col = NeonCeleste
    val w = 54f * scale
    val h = 64f * scale
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    val body = Rect(left, top + 8f * scale, left + w, top + h)
    neonStroke(col, 4f * scale, glow, 0.95f, layers = 4) { c, st ->
        drawRoundRect(c, topLeft = Offset(body.left, body.top), size = Size(body.width, body.height), cornerRadius = CornerRadius(7f * scale), style = st)
    }
    drawRoundRect(
        Ink.copy(alpha = 0.88f),
        Rect(left + 7f * scale, top + 18f * scale, left + w - 7f * scale, top + 39f * scale),
        CornerRadius(3f * scale)
    )
    drawLine(col, Offset(center.x, top + 19f * scale), Offset(center.x, top + 38f * scale), 2f * scale)
    drawLine(col, Offset(left - 5f * scale, top + 24f * scale), Offset(left, top + 24f * scale), 3f * scale, StrokeCap.Round)
    drawLine(col, Offset(left + w, top + 24f * scale), Offset(left + w + 5f * scale, top + 24f * scale), 3f * scale, StrokeCap.Round)
    drawCircle(col, 4f * scale, Offset(left + 11f * scale, top + h - 2f * scale))
    drawCircle(col, 4f * scale, Offset(left + w - 11f * scale, top + h - 2f * scale))
    drawLine(NeonPink, Offset(left + 11f * scale, top + 49f * scale), Offset(left + w - 11f * scale, top + 49f * scale), 2f * scale)
}

private fun DrawScope.menuIcon(pulse: Float, glow: Float, open: Float) {
    val col = lerp(NeonCeleste, NeonPink, open)
    for (i in 0..2) {
        val y = 50f + (i - 1) * 24f * (1f - open)
        val len = if (i == 1) 44f else 64f
        val alpha = if (i == 1) 1f - open else 1f
        rotate((1 - i) * 45f * open, Offset(50f, y)) {
            neonStroke(col, 8f, glow, alpha, 3) { c, st ->
                drawLine(c, Offset(50f - len / 2f, y), Offset(50f + len / 2f, y), st.width, StrokeCap.Round)
            }
        }
    }
    drawRect(NeonPink.copy(alpha = (0.4f + 0.6f * pulse) * (1f - open)), Offset(78f, 47f), Size(8f, 6f))
}

private fun DrawScope.searchIcon(t: Float, pulse: Float, glow: Float, open: Float) {
    val col = lerp(NeonCeleste, NeonPink, open)
    val c = Offset(42f, 42f)
    val r = 24f
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
            moveTo(24f, 70f)
            cubicTo(32f, 62f, 32f, 50f, 32f, 42f)
            cubicTo(32f, 28f, 40f, 20f, 50f, 20f)
            cubicTo(60f, 20f, 68f, 28f, 68f, 42f)
            cubicTo(68f, 50f, 68f, 62f, 76f, 70f)
            close()
        }
        neonStroke(col, 6f, glow, layers = 3) { c, st -> drawPath(body, c, style = st) }
        drawLine(col, Offset(44f, 20f), Offset(56f, 20f), 5f, StrokeCap.Round)
        drawArc(col, 20f, 140f, false, Offset(38f, 66f), Size(24f, 16f), style = Stroke(5f))
    }
}
