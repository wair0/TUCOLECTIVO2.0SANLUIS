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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

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

private enum class HeaderControl { MENU, SEARCH, NOTIFICATIONS }

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
    val context = LocalContext.current
    val typeface = remember(context) {
        runCatching { Typeface.createFromAsset(context.assets, "fonts/cyberpunk.ttf") }
            .getOrElse { Typeface.MONOSPACE }
    }
    val anim = rememberInfiniteTransition(label = "header")
    val scan by anim.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "scan"
    )
    val pulse by anim.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val flicker by anim.animateFloat(
        0.72f, 1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flicker"
    )

    Box(modifier.fillMaxWidth().height(92.dp).zIndex(40f)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val inset = 4.dp.toPx()
            val frame = androidx.compose.ui.geometry.Rect(inset, inset, w - inset, h - inset)

            drawRect(Ink)
            drawHudGrid(w, h)
            drawNeonFrame(frame, 0.78f + pulse * 0.5f)

            val scanX = inset + (w - inset * 2f) * scan
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.9f), Color.Transparent)),
                androidx.compose.ui.geometry.Offset(scanX - 24.dp.toPx(), inset),
                androidx.compose.ui.geometry.Size(48.dp.toPx(), 2.dp.toPx())
            )

            repeat(12) { i ->
                val active = ((scan * 12f).toInt() + i) % 6 < 3
                val c = if (i % 2 == 0) Cyan else Pink
                drawRect(
                    c.copy(alpha = if (active) 0.95f else 0.2f),
                    androidx.compose.ui.geometry.Offset(w * 0.30f + i * 6.5.dp.toPx(), h - 7.dp.toPx()),
                    androidx.compose.ui.geometry.Size(4.dp.toPx(), 2.dp.toPx())
                )
            }

            drawStatusPlate(w, h, pulse)
            drawHeaderText(typeface, title, statusText, w, h, flicker, pulse)
        }

        Row(
            Modifier.fillMaxSize().padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderControlButton(HeaderControl.MENU, menuOpen, onMenuClick)
            Spacer(Modifier.weight(1f))
            HeaderControlButton(HeaderControl.SEARCH, searchOpen, onSearchClick)
            Spacer(Modifier.width(4.dp))
            HeaderControlButton(
                HeaderControl.NOTIFICATIONS,
                notificationsOpen,
                onNotificationsClick,
                badge = hasUnread
            )
        }
    }
}

@Composable
private fun HeaderControlButton(
    control: HeaderControl,
    active: Boolean,
    onClick: () -> Unit,
    badge: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")
    val open by animateFloatAsState(if (active) 1f else 0f, tween(220), label = "open")
    val anim = rememberInfiniteTransition(label = "control")
    val pulse by anim.animateFloat(
        0f, 1f,
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
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val accent = if (open > 0.5f) Pink else Cyan
            drawRect(
                Panel,
                androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
                androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx())
            )
            drawNeonFrame(
                androidx.compose.ui.geometry.Rect(
                    2.dp.toPx(), 2.dp.toPx(), size.width - 2.dp.toPx(), size.height - 2.dp.toPx()
                ),
                0.75f + pulse * 0.4f + press
            )
            when (control) {
                HeaderControl.MENU -> drawMenuIcon(accent, press, open)
                HeaderControl.SEARCH -> drawSearchIcon(accent, press)
                HeaderControl.NOTIFICATIONS -> drawBellIcon(accent, press)
            }
            if (badge) {
                drawCircle(Pink.copy(alpha = 0.3f + 0.45f * pulse), 5.dp.toPx(),
                    androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx(), 4.dp.toPx()))
                drawCircle(Pink, 2.5.dp.toPx(),
                    androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx(), 4.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawHudGrid(w: Float, h: Float) {
    repeat(13) { i ->
        val x = w * i / 12f
        drawLine(Cyan.copy(alpha = 0.025f),
            androidx.compose.ui.geometry.Offset(x, 5.dp.toPx()),
            androidx.compose.ui.geometry.Offset(x, h - 5.dp.toPx()), 1f)
    }
    repeat(4) { i ->
        val y = h * i / 3f
        drawLine(Pink.copy(alpha = 0.022f),
            androidx.compose.ui.geometry.Offset(5.dp.toPx(), y),
            androidx.compose.ui.geometry.Offset(w - 5.dp.toPx(), y), 1f)
    }
}

private fun DrawScope.drawNeonFrame(
    rect: androidx.compose.ui.geometry.Rect,
    glow: Float
) {
    for (layer in 4 downTo 1) {
        drawRect(
            Cyan.copy(alpha = (0.055f * glow).coerceIn(0f, 1f)),
            androidx.compose.ui.geometry.Offset(rect.left, rect.top),
            androidx.compose.ui.geometry.Size(rect.width, rect.height),
            Stroke(
                width = 1.4.dp.toPx() * (1f + layer * 1.15f),
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
        )
    }
    drawRect(
        Cyan,
        androidx.compose.ui.geometry.Offset(rect.left, rect.top),
        androidx.compose.ui.geometry.Size(rect.width, rect.height),
        Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )
    drawRect(
        Core.copy(alpha = 0.7f),
        androidx.compose.ui.geometry.Offset(rect.left, rect.top),
        androidx.compose.ui.geometry.Size(rect.width, rect.height),
        Stroke(width = 0.55.dp.toPx())
    )
}

private fun DrawScope.drawStatusPlate(w: Float, h: Float, pulse: Float) {
    val plate = androidx.compose.ui.geometry.Rect(
        w * 0.35f, h * 0.64f, w * 0.65f, h * 0.92f
    )
    drawRect(
        Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.08f), Panel, Pink.copy(alpha = 0.08f))),
        androidx.compose.ui.geometry.Offset(plate.left, plate.top),
        androidx.compose.ui.geometry.Size(plate.width, plate.height)
    )
    drawRect(
        Cyan.copy(alpha = 0.55f + 0.2f * pulse),
        androidx.compose.ui.geometry.Offset(plate.left, plate.top),
        androidx.compose.ui.geometry.Size(plate.width, plate.height),
        Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        Green.copy(alpha = 0.22f + 0.4f * pulse),
        7.dp.toPx(),
        androidx.compose.ui.geometry.Offset(w * 0.382f, h * 0.78f)
    )
    drawCircle(Green, 2.6.dp.toPx(),
        androidx.compose.ui.geometry.Offset(w * 0.382f, h * 0.78f))
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
    titlePaint.color = Pink.copy(alpha = 0.5f).toArgb()
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.drawText(title, w / 2f - 1.dp.toPx(), 29.dp.toPx() + 1.dp.toPx(), titlePaint)
        titlePaint.color = Core.copy(alpha = flicker).toArgb()
        native.drawText(title, w / 2f, 29.dp.toPx(), titlePaint)

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = 8.sp.toPx()
            letterSpacing = 0.06f
            color = Green.copy(alpha = 0.72f + pulse * 0.28f).toArgb()
        }
        native.drawText(status, w / 2f, h * 0.78f + 3.dp.toPx(), statusPaint)
    }
}

private fun DrawScope.drawMenuIcon(color: Color, press: Float, open: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val spread = 8.dp.toPx() * (1f - open * 0.65f)
    for (i in -1..1) {
        val y = cy + i * spread
        drawLine(color,
            androidx.compose.ui.geometry.Offset(cx - 11.dp.toPx(), y),
            androidx.compose.ui.geometry.Offset(cx + 11.dp.toPx(), y),
            2.5.dp.toPx() * (1f + press * 0.15f), StrokeCap.Round)
    }
}

private fun DrawScope.drawSearchIcon(color: Color, press: Float) {
    val center = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.43f)
    val radius = 8.5.dp.toPx()
    drawCircle(color, radius, center, Stroke(width = 2.4.dp.toPx() * (1f + press * 0.15f)))
    drawLine(Pink,
        center + androidx.compose.ui.geometry.Offset(radius * 0.68f, radius * 0.68f),
        center + androidx.compose.ui.geometry.Offset(13.dp.toPx(), 13.dp.toPx()),
        2.4.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.drawBellIcon(color: Color, press: Float) {
    val cx = size.width / 2f
    val top = 9.dp.toPx()
    val bottom = 29.dp.toPx()
    val path = Path().apply {
        moveTo(cx - 9.dp.toPx(), bottom)
        cubicTo(cx - 7.dp.toPx(), bottom - 4.dp.toPx(), cx - 7.dp.toPx(), top + 3.dp.toPx(), cx, top)
        cubicTo(cx + 7.dp.toPx(), top + 3.dp.toPx(), cx + 7.dp.toPx(), bottom - 4.dp.toPx(), cx + 9.dp.toPx(), bottom)
    }
    drawPath(path, color, style = Stroke(width = 2.3.dp.toPx() * (1f + press * 0.15f), cap = StrokeCap.Round))
    drawLine(color,
        androidx.compose.ui.geometry.Offset(cx - 11.dp.toPx(), bottom),
        androidx.compose.ui.geometry.Offset(cx + 11.dp.toPx(), bottom),
        2.3.dp.toPx(), StrokeCap.Round)
    drawCircle(Pink, 2.dp.toPx(),
        androidx.compose.ui.geometry.Offset(cx, bottom + 4.dp.toPx()))
}
