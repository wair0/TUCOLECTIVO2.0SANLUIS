package com.tucolectivo.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.ColumnScope

private val Cyan = Color(0xFF19D9FF)
private val Core = Color(0xFFE8FCFF)
private val Pink = Color(0xFFFF2E9A)
private val Green = Color(0xFF25FFB7)
private val Ink = Color(0xFF020308)
private val Panel = Color(0xFF03050D)

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val typeface = remember(context) {
        runCatching {
            Typeface.createFromAsset(context.assets, "fonts/cyberpunk.ttf")
        }.getOrElse { Typeface.MONOSPACE }
    }

    val anim = rememberInfiniteTransition(label = "header")
    val scan by anim.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "scan"
    )
    val pulse by anim.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val flicker by anim.animateFloat(
        0.72f,
        1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flicker"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(92.dp)
            .zIndex(40f)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val inset = 4.dp.toPx()
            val frame = Rect(inset, inset, w - inset, h - inset)

            drawRect(Ink)
            drawHudGrid(w, h)
            drawNeonFrame(frame, 0.78f + pulse * 0.5f)

            val scanX = inset + (w - inset * 2f) * scan
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Cyan.copy(alpha = 0.9f), Color.Transparent)
                ),
                topLeft = Offset(scanX - 24.dp.toPx(), inset),
                size = Size(48.dp.toPx(), 2.dp.toPx())
            )

            repeat(12) { i ->
                val active = ((scan * 12f).toInt() + i) % 6 < 3
                val accent = if (i % 2 == 0) Cyan else Pink
                drawRect(
                    color = accent.copy(alpha = if (active) 0.95f else 0.2f),
                    topLeft = Offset(w * 0.30f + i * 6.5.dp.toPx(), h - 7.dp.toPx()),
                    size = Size(4.dp.toPx(), 2.dp.toPx())
                )
            }

            drawStatusPlate(w, h, pulse)
            drawHeaderText(typeface, title, statusText, w, h, flicker, pulse)
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderControlButton(
                active = menuOpen,
                onClick = onMenuClick
            ) { accent, press, open ->
                drawMenuIcon(accent, press, open)
            }

            Spacer(Modifier.weight(1f))

            HeaderControlButton(
                active = searchOpen,
                onClick = onSearchClick
            ) { accent, press, _ ->
                drawSearchIcon(accent, press)
            }

            Spacer(Modifier.width(4.dp))

            HeaderControlButton(
                active = notificationsOpen,
                badge = hasUnread,
                onClick = onNotificationsClick
            ) { accent, press, _ ->
                drawBellIcon(accent, press)
            }
        }
    }
}

@Composable
private fun HeaderControlButton(
    active: Boolean,
    badge: Boolean = false,
    onClick: () -> Unit,
    icon: DrawScope.(Color, Float, Float) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(90),
        label = "press"
    )
    val open by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(220),
        label = "open"
    )
    val anim = rememberInfiniteTransition(label = "control")
    val pulse by anim.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "controlPulse"
    )

    Box(
        Modifier
            .size(42.dp)
            .graphicsLayer {
                val scale = 1f - press * 0.08f
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val accent = when {
                active -> Pink
                else -> Cyan
            }

            drawRect(
                color = Panel,
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx())
            )

            drawNeonFrame(
                Rect(
                    2.dp.toPx(),
                    2.dp.toPx(),
                    size.width - 2.dp.toPx(),
                    size.height - 2.dp.toPx()
                ),
                0.75f + pulse * 0.4f + press
            )

            icon(accent, press, open)

            if (badge) {
                drawCircle(
                    color = Pink.copy(alpha = 0.3f + 0.45f * pulse),
                    radius = 5.dp.toPx(),
                    center = Offset(size.width - 3.dp.toPx(), 4.dp.toPx())
                )
                drawCircle(
                    color = Pink,
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width - 3.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawHudGrid(w: Float, h: Float) {
    repeat(13) { i ->
        val x = w * i / 12f
        drawLine(
            color = Cyan.copy(alpha = 0.025f),
            start = Offset(x, 5.dp.toPx()),
            end = Offset(x, h - 5.dp.toPx()),
            strokeWidth = 1f
        )
    }

    repeat(4) { i ->
        val y = h * i / 3f
        drawLine(
            color = Pink.copy(alpha = 0.022f),
            start = Offset(5.dp.toPx(), y),
            end = Offset(w - 5.dp.toPx(), y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawNeonFrame(rect: Rect, glow: Float) {
    for (layer in 4 downTo 1) {
        drawRect(
            color = Cyan.copy(alpha = (0.055f * glow).coerceIn(0f, 1f)),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(
                width = 1.4.dp.toPx() * (1f + layer * 1.15f),
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
        )
    }

    drawRect(
        color = Cyan,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )

    drawRect(
        color = Core.copy(alpha = 0.7f),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = 0.55.dp.toPx())
    )
}

private fun DrawScope.drawStatusPlate(w: Float, h: Float, pulse: Float) {
    val plate = Rect(
        w * 0.35f,
        h * 0.64f,
        w * 0.65f,
        h * 0.92f
    )

    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Cyan.copy(alpha = 0.08f), Panel, Pink.copy(alpha = 0.08f))
        ),
        topLeft = Offset(plate.left, plate.top),
        size = Size(plate.width, plate.height)
    )

    drawRect(
        color = Cyan.copy(alpha = 0.55f + 0.2f * pulse),
        topLeft = Offset(plate.left, plate.top),
        size = Size(plate.width, plate.height),
        style = Stroke(width = 1.dp.toPx())
    )

    drawCircle(
        color = Green.copy(alpha = 0.22f + 0.4f * pulse),
        radius = 7.dp.toPx(),
        center = Offset(w * 0.382f, h * 0.78f)
    )

    drawCircle(
        color = Green,
        radius = 2.6.dp.toPx(),
        center = Offset(w * 0.382f, h * 0.78f)
    )
}

private fun DrawScope.drawHeaderText(
    typeface: Typeface,
    title: String,
    status: String,
    w: Float,
    h: Float,
    flicker: Float,
    pulse: Float
) {
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textAlign = Paint.Align.CENTER
        textSize = 16.sp.toPx()
        letterSpacing = 0.08f
    }

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas

        titlePaint.color = Pink.copy(alpha = 0.5f).toArgb()
        native.drawText(
            title,
            w / 2f - 1.dp.toPx(),
            29.dp.toPx() + 1.dp.toPx(),
            titlePaint
        )

        titlePaint.color = Core.copy(alpha = flicker).toArgb()
        native.drawText(title, w / 2f, 29.dp.toPx(), titlePaint)

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = 8.sp.toPx()
            letterSpacing = 0.06f
            color = Green.copy(alpha = 0.72f + pulse * 0.28f).toArgb()
        }

        native.drawText(
            status,
            w / 2f,
            h * 0.78f + 3.dp.toPx(),
            statusPaint
        )
    }
}

private fun DrawScope.drawMenuIcon(color: Color, press: Float, open: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val spread = 8.dp.toPx() * (1f - open * 0.65f)

    for (i in -1..1) {
        val y = cy + i * spread
        drawLine(
            color = color,
            start = Offset(cx - 11.dp.toPx(), y),
            end = Offset(cx + 11.dp.toPx(), y),
            strokeWidth = 2.5.dp.toPx() * (1f + press * 0.15f),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSearchIcon(color: Color, press: Float) {
    val center = Offset(size.width * 0.43f, size.height * 0.43f)
    val radius = 8.5.dp.toPx()

    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2.4.dp.toPx() * (1f + press * 0.15f))
    )

    drawLine(
        color = Pink,
        start = center + Offset(radius * 0.68f, radius * 0.68f),
        end = center + Offset(13.dp.toPx(), 13.dp.toPx()),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawBellIcon(color: Color, press: Float) {
    val cx = size.width / 2f
    val top = 9.dp.toPx()
    val bottom = 29.dp.toPx()

    val path = Path().apply {
        moveTo(cx - 9.dp.toPx(), bottom)
        cubicTo(
            cx - 7.dp.toPx(), bottom - 4.dp.toPx(),
            cx - 7.dp.toPx(), top + 3.dp.toPx(),
            cx, top
        )
        cubicTo(
            cx + 7.dp.toPx(), top + 3.dp.toPx(),
            cx + 7.dp.toPx(), bottom - 4.dp.toPx(),
            cx + 9.dp.toPx(), bottom
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 2.3.dp.toPx() * (1f + press * 0.15f),
            cap = StrokeCap.Round
        )
    )

    drawLine(
        color = color,
        start = Offset(cx - 11.dp.toPx(), bottom),
        end = Offset(cx + 11.dp.toPx(), bottom),
        strokeWidth = 2.3.dp.toPx(),
        cap = StrokeCap.Round
    )

    drawCircle(
        color = Pink,
        radius = 2.dp.toPx(),
        center = Offset(cx, bottom + 4.dp.toPx())
    )
}
