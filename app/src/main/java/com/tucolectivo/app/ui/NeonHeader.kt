package com.tucolectivo.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*

/**
 * Header cyberpunk v3 "Transformers": blindaje metálico, todo recto, solo celeste y magenta.
 * Tus botones / iconos / textos nativos van dentro de cada slot:
 *
 * CyberHeader(
 *     menuSlot   = { TuBotonHamburguesa() },     // marco 48dp
 *     statusSlot = { TuTextoDeEstado(estado) },  // el marco crece o encoge con el texto
 *     titleSlot  = { TuTitulo() },
 *     searchSlot = { TuBotonLupa() },            // marco 44dp
 *     bellSlot   = { TuBotonCampanita() }        // marco 44dp
 * )
 */
object CyberColors {
    val Cyan = Color(0xFF19E3FF)
    val Magenta = Color(0xFFFF2BD6)
    val Steel = Color(0xFF1A2130)
    val SteelDark = Color(0xFF0B0F19)
    val Deep = Color(0xFF05070E)
}

/** Corchetes/abrazaderas en las cuatro esquinas (solo líneas rectas). */
private fun corners(w: Float, h: Float, a: Float) = Path().apply {
    moveTo(0f, a); lineTo(0f, 0f); lineTo(a, 0f)
    moveTo(w - a, 0f); lineTo(w, 0f); lineTo(w, a)
    moveTo(w, h - a); lineTo(w, h); lineTo(w - a, h)
    moveTo(a, h); lineTo(0f, h); lineTo(0f, h - a)
}

/** Bisel metálico: luz arriba/izquierda, sombra abajo/derecha. */
private fun DrawScope.bevel(w: Float, h: Float, t: Float) {
    drawRect(Color.White.copy(alpha = 0.16f), Offset.Zero, Size(w, t))
    drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(t, h))
    drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, h - t), Size(w, t))
    drawRect(Color.Black.copy(alpha = 0.40f), Offset(w - t, 0f), Size(t, h))
}

/** Remaches cuadrados en las cuatro esquinas. */
private fun DrawScope.rivets(w: Float, h: Float, o: Float, s: Float) {
    for (x in listOf(o, w - o - s)) for (y in listOf(o, h - o - s)) {
        drawRect(Color.White.copy(alpha = 0.35f), Offset(x, y), Size(s, s))
    }
}

/**
 * Placa de blindaje rectangular. Se redibuja sola al cambiar de tamaño.
 * vents = rejilla de ventilación a la derecha (reservá padding al final); visor = banda de energon + alas.
 */
@Composable
fun CyberFrame(
    modifier: Modifier = Modifier,
    accent: Color = CyberColors.Cyan,
    accent2: Color = accent,
    vents: Boolean = false,
    visor: Boolean = false,
    padding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .then(modifier)
            .drawWithCache {
                val u = 1.dp.toPx()
                val w = size.width
                val h = size.height
                val plate = Brush.verticalGradient(listOf(CyberColors.Steel, CyberColors.SteelDark))
                val edge = Brush.horizontalGradient(listOf(accent, accent2))
                val band = Brush.horizontalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.3f), accent2.copy(alpha = 0.3f), Color.Transparent)
                )
                onDrawBehind {
                    drawRect(plate)
                    bevel(w, h, 2 * u)
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(5 * u, 5 * u), Size(w - 10 * u, h - 10 * u), style = Stroke(u)) // línea de panel
                    rivets(w, h, 8 * u, 3 * u)
                    drawRect(edge, alpha = 0.22f, style = Stroke(5 * u))                                                        // glow energon
                    drawRect(edge, style = Stroke(1.5f * u))
                    if (vents) repeat(4) { i ->
                        val x = w - 34 * u + i * 6 * u
                        drawRect(Color.Black.copy(alpha = 0.75f), Offset(x, 10 * u), Size(3 * u, h - 20 * u))
                        drawRect(accent2, Offset(x, h - 12 * u), Size(3 * u, 2 * u))
                    }
                    if (visor) {
                        drawRect(band, Offset(6 * u, h / 2 - 9 * u), Size(w - 12 * u, 18 * u))
                        drawRect(accent, Offset(-34 * u, h / 2 - u), Size(28 * u, 2 * u))
                        drawRect(accent, Offset(-42 * u, h / 2 - 8 * u), Size(8 * u, 16 * u))
                        drawRect(accent2, Offset(w + 6 * u, h / 2 - u), Size(28 * u, 2 * u))
                        drawRect(accent2, Offset(w + 34 * u, h / 2 - 8 * u), Size(8 * u, 16 * u))
                    }
                }
            }
            .padding(padding),
        contentAlignment = contentAlignment,
        content = content
    )
}


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
    val fontFamily = rememberCyberpunkFontFamily()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberColors.Deep)
            .statusBarsPadding()
            .padding(6.dp)
            .drawWithCache {
                val u = 1.dp.toPx()
                val w = size.width
                val h = size.height
                val plate = Brush.verticalGradient(listOf(CyberColors.Steel, CyberColors.Deep))
                val rim = Brush.horizontalGradient(listOf(CyberColors.Cyan, CyberColors.Magenta))
                val seams = Path().apply {
                    var x = 48 * u
                    while (x < w) { moveTo(x, 0f); lineTo(x, h); x += 48 * u }
                    var y = 24 * u
                    while (y < h) { moveTo(0f, y); lineTo(w, y); y += 24 * u }
                }
                val clamps = corners(w, h, 22 * u)
                onDrawBehind {
                    drawRect(plate)
                    drawPath(seams, Color.Black.copy(alpha = 0.5f), style = Stroke(u))
                    translate(u, u) { drawPath(seams, Color.White.copy(alpha = 0.06f), style = Stroke(u)) }
                    bevel(w, h, 3 * u)
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(7 * u, 7 * u), Size(w - 14 * u, h - 14 * u), style = Stroke(u))
                    drawRect(rim, alpha = 0.22f, style = Stroke(7 * u))
                    drawRect(rim, style = Stroke(2 * u))
                    drawPath(clamps, Color.White.copy(alpha = 0.32f), style = Stroke(4 * u, cap = StrokeCap.Square))
                    rivets(w, h, 11 * u, 3 * u)
                }
            }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CyberHeaderControl(
                modifier = Modifier.size(48.dp),
                active = menuOpen,
                accent = CyberColors.Cyan,
                onClick = onMenuClick
            ) { color, press, open -> drawMenuIcon(color, press, open) }

            Spacer(Modifier.width(10.dp))

            // El marco se dimensiona por el texto y anima su expansión/contracción.
            CyberFrame(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 170.dp)
                    .height(40.dp),
                vents = true,
                padding = PaddingValues(start = 14.dp, top = 6.dp, end = 40.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = statusText,
                    style = TextStyle(
                        color = CyberColors.Cyan,
                        fontFamily = fontFamily,
                        fontSize = 8.sp,
                        letterSpacing = 0.06.em
                    ),
                    maxLines = 1
                )
            }

            Spacer(Modifier.weight(1f))

            CyberHeaderControl(
                modifier = Modifier.size(44.dp),
                active = searchOpen,
                accent = CyberColors.Magenta,
                onClick = onSearchClick
            ) { color, press, _ -> drawSearchIcon(color, press) }

            Spacer(Modifier.width(8.dp))

            CyberHeaderControl(
                modifier = Modifier.size(44.dp),
                active = notificationsOpen,
                accent = CyberColors.Magenta,
                badge = hasUnread,
                onClick = onNotificationsClick
            ) { color, press, _ -> drawBellIcon(color, press) }
        }

        CyberFrame(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
                .widthIn(min = 180.dp, max = 230.dp)
                .height(34.dp),
            accent = CyberColors.Cyan,
            accent2 = CyberColors.Magenta,
            visor = true,
            padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = Color(0xFFE8FCFF),
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 0.08.em
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CyberHeaderControl(
    modifier: Modifier,
    active: Boolean,
    accent: Color,
    badge: Boolean = false,
    onClick: () -> Unit,
    icon: DrawScope.(Color, Float, Float) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(90),
        label = "headerPress"
    )
    val open by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(220),
        label = "headerOpen"
    )
    val pulseTransition = rememberInfiniteTransition(label = "headerControlPulse")
    val pulse by pulseTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "headerControlPulseValue"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = 1f - press * 0.08f
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val frameAccent = if (active) CyberColors.Magenta else accent
            drawRect(
                color = CyberColors.SteelDark,
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx())
            )
            drawNeonFrame(
                Rect(2.dp.toPx(), 2.dp.toPx(), size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                0.72f + pulse * 0.42f + press
            )
            icon(frameAccent, press, open)

            if (badge) {
                drawCircle(
                    color = CyberColors.Magenta.copy(alpha = 0.28f + 0.5f * pulse),
                    radius = 5.dp.toPx(),
                    center = Offset(size.width - 3.dp.toPx(), 4.dp.toPx())
                )
                drawCircle(
                    color = CyberColors.Magenta,
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width - 3.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawNeonFrame(rect: Rect, glow: Float) {
    for (layer in 4 downTo 1) {
        drawRect(
            color = CyberColors.Cyan.copy(alpha = (0.055f * glow).coerceIn(0f, 1f)),
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
        color = CyberColors.Cyan,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )
    drawRect(
        color = Color.White.copy(alpha = 0.7f),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = 0.55.dp.toPx())
    )
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
            cap = StrokeCap.Square
        )
    }
}

private fun DrawScope.drawSearchIcon(color: Color, press: Float) {
    val center = Offset(size.width * 0.43f, size.height * 0.43f)
    val radius = 8.5.dp.toPx()
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 2.4.dp.toPx() * (1f + press * 0.15f)))
    drawLine(
        color = CyberColors.Magenta,
        start = center + Offset(radius * 0.68f, radius * 0.68f),
        end = center + Offset(13.dp.toPx(), 13.dp.toPx()),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Square
    )
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
    drawPath(path = path, color = color, style = Stroke(width = 2.3.dp.toPx() * (1f + press * 0.15f), cap = StrokeCap.Square))
    drawLine(
        color = color,
        start = Offset(cx - 11.dp.toPx(), bottom),
        end = Offset(cx + 11.dp.toPx(), bottom),
        strokeWidth = 2.3.dp.toPx(),
        cap = StrokeCap.Square
    )
    drawCircle(color = CyberColors.Magenta, radius = 2.dp.toPx(), center = Offset(cx, bottom + 4.dp.toPx()))
}
