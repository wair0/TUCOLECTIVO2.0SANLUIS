package com.tucolectivo.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
data class NeonNotification(
    val title: String,
    val detail: String,
    val time: String,
    val unread: Boolean = true
)

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
private fun NeonCyberText(
    text: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    modifier: Modifier = Modifier
) {
    // Animación visible y aislada: el barrido se aplica solo a los píxeles del texto.
    // InfiniteTransition actualiza el valor continuamente mientras el texto está en composición.
    val transition = rememberInfiniteTransition(label = "neon_text")
    val sweep by transition.animateFloat(
        initialValue = -0.45f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon_sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_text_pulse"
    )

    BasicText(
        text = text,
        modifier = modifier
            .graphicsLayer {
                // SrcIn necesita una capa aislada para no afectar el marco.
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = 0.92f + 0.08f * pulse
            }
            .drawWithContent {
                drawContent()

                // Banda estrecha de luz que cruza realmente las letras.
                val bandCenter = size.width * sweep
                val bandWidth = (size.width * 0.42f).coerceAtLeast(24f)
                val band = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF19D9FF),
                        Color(0xFF19D9FF),
                        Color(0xFFE8FCFF),
                        Color(0xFFB66CFF),
                        Color(0xFFFF2E9A)
                    ),
                    start = Offset(bandCenter - bandWidth, 0f),
                    end = Offset(bandCenter + bandWidth, size.height)
                )

                drawRect(
                    brush = band,
                    blendMode = BlendMode.SrcIn
                )
            },
        style = TextStyle(
            color = Color.White,
            fontFamily = fontFamily,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            shadow = Shadow(
                color = Color(0xFF19D9FF).copy(alpha = 0.78f),
                blurRadius = 9f
            )
        ),
        maxLines = 1
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberHeaderControl(
                modifier = Modifier.size(48.dp),
                active = menuOpen,
                accent = CyberColors.Cyan,
                onClick = onMenuClick
            ) { color, press, open -> drawMenuIcon(color, press, open) }

            Spacer(Modifier.width(10.dp))

            // Centro del header: título arriba y estado debajo.
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CyberFrame(
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 230.dp)
                        .height(34.dp),
                    accent = CyberColors.Cyan,
                    accent2 = CyberColors.Magenta,
                    visor = true,
                    padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeonCyberText(
                        text = title,
                        fontFamily = fontFamily,
                        fontSize = 12.sp,
                        letterSpacing = 0.08.em
                    )
                }

                Spacer(Modifier.height(3.dp))

                CyberFrame(
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 170.dp)
                        .height(30.dp)
                        .animateContentSize(),
                    vents = true,
                    padding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeonCyberText(
                        text = statusText,
                        fontFamily = fontFamily,
                        fontSize = 8.sp,
                        letterSpacing = 0.06.em
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            CyberHeaderControl(
                modifier = Modifier.size(44.dp),
                active = searchOpen,
                accent = CyberColors.Cyan,
                onClick = onSearchClick
            ) { color, press, _ -> drawSearchIcon(color, press) }

            Spacer(Modifier.width(8.dp))

            CyberHeaderControl(
                modifier = Modifier.size(44.dp),
                active = notificationsOpen,
                accent = CyberColors.Cyan,
                badge = hasUnread,
                onClick = onNotificationsClick
            ) { color, press, _ -> drawBellIcon(color, press) }
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
            // El icono dibuja su propio marco Transformer: no se superpone un marco antiguo.
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
    val u = size.minDimension / 44f
    val white = Color(0xFFE8FCFF)
    val steel = CyberColors.Steel
    val frame = Rect(2f * u, 2f * u, size.width - 2f * u, size.height - 2f * u)

    // Diseño Canva: módulo mecánico Transformer, exclusivamente cyan/blanco.
    drawRect(steel, frame.topLeft, Size(frame.width, frame.height))
    drawRect(color.copy(alpha = 0.34f), frame.topLeft, Size(frame.width, frame.height), style = Stroke(4f * u))
    drawRect(color, frame.topLeft, Size(frame.width, frame.height), style = Stroke(1.4f * u))
    drawRect(white.copy(alpha = 0.65f), Offset(frame.left + 4f * u, frame.top + 4f * u),
        Size(frame.width - 8f * u, frame.height - 8f * u), style = Stroke(0.6f * u))

    val cy = size.height / 2f
    val gap = 8f * u
    for (i in -1..1) {
        val y = cy + i * gap
        val p = Path().apply {
            moveTo(9f * u, y - 2.8f * u)
            lineTo(13f * u, y - 4.2f * u)
            lineTo(size.width - 13f * u, y - 4.2f * u)
            lineTo(size.width - 9f * u, y)
            lineTo(size.width - 13f * u, y + 4.2f * u)
            lineTo(13f * u, y + 4.2f * u)
            close()
        }
        drawPath(p, color.copy(alpha = 0.22f), style = Stroke(2.8f * u))
        drawPath(p, color, style = Stroke(1.5f * u))
        drawLine(white.copy(alpha = 0.75f), Offset(14f * u, y - 0.8f * u),
            Offset(size.width - 14f * u, y - 0.8f * u), 0.55f * u)
    }
    drawCircle(color.copy(alpha = 0.35f), 3f * u, Offset(7f * u, 7f * u))
    drawCircle(white.copy(alpha = 0.55f), 1.1f * u, Offset(size.width - 7f * u, size.height - 7f * u))
}

private fun DrawScope.drawSearchIcon(color: Color, press: Float) {
    val u = size.minDimension / 44f
    val cx = size.width * 0.46f
    val cy = size.height * 0.45f
    val r = 8.4f * u
    val core = Color(0xFFE8FCFF)
    val glow = color.copy(alpha = 0.20f + press * 0.12f)

    // Icono nativo HUD: visor de reconocimiento cyberpunk.
    drawCircle(glow, r + 5f * u, Offset(cx, cy), style = Stroke(2.5f * u))
    drawCircle(color.copy(alpha = 0.55f), r + 2.2f * u, Offset(cx, cy), style = Stroke(1f * u))
    drawCircle(color, r, Offset(cx, cy), style = Stroke(2f * u))

    // Retícula interna.
    drawLine(color.copy(alpha = 0.55f), Offset(cx - r + 2f*u, cy), Offset(cx + r - 2f*u, cy), 0.7f*u)
    drawLine(color.copy(alpha = 0.55f), Offset(cx, cy - r + 2f*u), Offset(cx, cy + r - 2f*u), 0.7f*u)
    drawCircle(core.copy(alpha = 0.9f), 2.1f*u, Offset(cx, cy))
    drawCircle(color, (3.6f + press*1.5f)*u, Offset(cx, cy), style = Stroke(0.8f*u))

    // Mango mecánico angular.
    val p0 = Offset(cx + r*0.62f, cy + r*0.62f)
    val p1 = Offset(cx + 13f*u, cy + 13f*u)
    drawLine(color.copy(alpha = 0.30f), p0, p1, 4f*u, StrokeCap.Square)
    drawLine(color, p0, p1, 1.8f*u, StrokeCap.Square)
    drawLine(core.copy(alpha = 0.8f), Offset(p1.x-2.5f*u,p1.y-2.5f*u), p1, 0.8f*u)

    // Tres microsegmentos de estado.
    drawLine(color, Offset(5f*u, 8f*u), Offset(10f*u, 8f*u), 1f*u)
    drawLine(color.copy(alpha=0.7f), Offset(5f*u, 11f*u), Offset(8f*u, 11f*u), 0.8f*u)
}

private fun DrawScope.drawBellIcon(color: Color, press: Float) {
    val u = size.minDimension / 44f
    val cx = size.width / 2f
    val core = Color(0xFFE8FCFF)
    val top = 9f*u
    val bottom = 28f*u

    // Icono nativo HUD: módulo de alerta cyberpunk.
    val shell = Path().apply {
        moveTo(cx-9f*u,bottom)
        lineTo(cx-7f*u,bottom-3f*u)
        lineTo(cx-6.5f*u,top+5f*u)
        quadraticBezierTo(cx,top,cx+6.5f*u,top+5f*u)
        lineTo(cx+7f*u,bottom-3f*u)
        lineTo(cx+9f*u,bottom)
        close()
    }
    drawPath(shell, color.copy(alpha=0.18f + press*0.08f), style=Stroke(3.8f*u))
    drawPath(shell, color, style=Stroke(1.8f*u))

    // Barras HUD laterales.
    drawLine(color, Offset(cx-13f*u,top+6f*u), Offset(cx-10f*u,top+9f*u), 1.5f*u, StrokeCap.Square)
    drawLine(color.copy(alpha=.7f), Offset(cx-14f*u,top+11f*u), Offset(cx-11f*u,top+11f*u), 1f*u)
    drawLine(color, Offset(cx+13f*u,top+6f*u), Offset(cx+10f*u,top+9f*u), 1.5f*u, StrokeCap.Square)
    drawLine(color.copy(alpha=.7f), Offset(cx+14f*u,top+11f*u), Offset(cx+11f*u,top+11f*u), 1f*u)

    // Sensor energético central.
    drawCircle(color.copy(alpha=.18f + press*.10f), 5f*u, Offset(cx,top+9f*u))
    drawCircle(color, 2.2f*u, Offset(cx,top+9f*u))
    drawCircle(core, .7f*u, Offset(cx-.7f*u,top+8.3f*u))

    // Placa inferior.
    drawLine(color, Offset(cx-11f*u,bottom), Offset(cx+11f*u,bottom), 2f*u, StrokeCap.Square)
    drawLine(core.copy(alpha=.75f), Offset(cx-5f*u,bottom-4.5f*u), Offset(cx+5f*u,bottom-4.5f*u), .8f*u)

    // Pulso de alerta.
    drawCircle(color.copy(alpha=.45f), (11f+press*3f)*u, Offset(cx,top+9f*u), style=Stroke(.8f*u))
}



@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberCyberpunkFontFamily(): androidx.compose.ui.text.font.FontFamily {
    val assets = LocalContext.current.assets
    return androidx.compose.ui.text.font.FontFamily(
        androidx.compose.ui.text.font.Font(
            path = "fonts/cyberpunk.ttf",
            assetManager = assets,
            weight = androidx.compose.ui.text.font.FontWeight.Normal
        )
    )
}
