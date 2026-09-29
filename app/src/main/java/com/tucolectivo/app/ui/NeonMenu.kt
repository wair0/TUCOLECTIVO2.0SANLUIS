package com.tucolectivo.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Menú cyberpunk con 4 tarjetas dibujadas 100 % con Canvas (Jetpack Compose + Material3).
 *
 * Uso: setContent { NeonMenuScreen(onLineas = { }, onMapa = { }, onParadas = { }, onFavoritos = { }) }
 */

// ───────────── Paleta ─────────────
private val NeonCeleste = Color(0xFF19D9FF) // marco y trazos principales
private val NeonCore = Color(0xFFE8FCFF)    // núcleo blanco del tubo de neón
private val NeonPink = Color(0xFFFF2E9A)    // acento cyberpunk
private val Ink = Color(0xFF060912)         // fondo
private val TAU = (2.0 * PI).toFloat()

// ───────────── Pantalla ─────────────
@Composable
fun NeonMenuScreen(
    onLineas: () -> Unit = {},
    onMapa: () -> Unit = {},
    onParadas: () -> Unit = {},
    onFavoritos: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1024), Ink)))
            .drawBehind { // grilla sutil de fondo
                val step = 28.dp.toPx()
                val grid = NeonCeleste.copy(alpha = 0.05f)
                var x = 0f
                while (x <= size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height)); x += step }
                var y = 0f
                while (y <= size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y)); y += step }
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.Center
    ) {
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

// ───────────── Tarjeta neón ─────────────
@Composable
fun NeonCard(
    title: String,
    modifier: Modifier = Modifier,
    phaseMs: Int = 0, // desfasa las animaciones para que las tarjetas no latan al unísono
    onClick: () -> Unit = {},
    icon: DrawScope.(t: Float, pulse: Float, glow: Float) -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "neon")
    val offset = StartOffset(phaseMs, StartOffsetType.FastForward)
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "t",
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), initialStartOffset = offset)
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse, offset)
    )
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")

    Box(
        modifier
            .aspectRatio(0.92f)
            .graphicsLayer { val k = 1f - 0.05f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        // Los valores animados se leen solo al dibujar: no hay recomposición por frame.
        Canvas(Modifier.fillMaxSize()) {
            val flick = if (t in 0.62f..0.635f || t in 0.67f..0.68f) 0.45f else 1f // parpadeo de neón
            val glow = (0.7f + 0.45f * pulse + 0.6f * press) * flick

            val pad = 10.dp.toPx()
            val fw = size.width - 2 * pad
            val fh = size.height - 2 * pad
            val cut = fw * 0.16f
            val frame = Path().apply { // esquinas cortadas: arriba-izquierda y abajo-derecha
                moveTo(pad + cut, pad)
                lineTo(pad + fw, pad)
                lineTo(pad + fw, pad + fh - cut)
                lineTo(pad + fw - cut, pad + fh)
                lineTo(pad, pad + fh)
                lineTo(pad, pad + cut)
                close()
            }

            // panel: degradado + scanlines + barra de escaneo
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
                        listOf(NeonCeleste.copy(alpha = 0f), NeonCeleste.copy(alpha = 0.22f), NeonCeleste.copy(alpha = 0f)),
                        startY = by - band, endY = by + band
                    ),
                    topLeft = Offset(pad, by - band), size = Size(fw, band * 2)
                )
            }

            // marco celeste neón
            neon(NeonCeleste, 2.6.dp.toPx(), glow, flick) { c, st -> drawPath(frame, c, style = st) }

            // destello con cola que recorre el borde
            val measure = PathMeasure().apply { setPath(frame, true) }
            val head = measure.length * t
            val tail = measure.length * 0.18f
            val seg = Path()
            val steps = 5
            for (k in 0 until steps) {
                measure.slice(head - tail * (k + 1) / steps, head - tail * k / steps, seg)
                neon(NeonCeleste, 3.6.dp.toPx(), 1.8f, 1f - k / steps.toFloat(), layers = 2) { c, st ->
                    drawPath(seg, c, style = st)
                }
            }

            // indicadores HUD (esquina superior derecha)
            for (i in 0..2) {
                val on = (t * 9f).toInt() % 3 == i
                drawRect(
                    NeonPink.copy(alpha = if (on) 1f else 0.3f),
                    Offset(pad + fw - (30 - i * 9).dp.toPx(), pad + 8.dp.toPx()),
                    Size(6.dp.toPx(), 3.dp.toPx())
                )
            }

            // ícono (diseñado en una grilla de 100×100)
            val s = size.width * 0.54f
            translate((size.width - s) / 2f, size.height * 0.14f) {
                scale(s / 100f, Offset.Zero) { icon(t, pulse, glow) }
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = NeonCore,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 20f))
            )
        }
    }
}

// ───────────── Utilidades de neón ─────────────

/** Trazo tipo tubo de neón: halo difuso (capas anchas y transparentes) + color + núcleo blanco. */
private inline fun neon(
    color: Color,
    width: Float,
    glow: Float = 1f,
    alpha: Float = 1f,
    layers: Int = 4,
    draw: (Color, Stroke) -> Unit
) {
    for (i in layers downTo 1) {
        draw(
            color.copy(alpha = (0.09f * glow * alpha).coerceIn(0f, 1f)),
            Stroke(width * (1f + i * 1.2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
    draw(color.copy(alpha = alpha), Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    draw(NeonCore.copy(alpha = 0.85f * alpha), Stroke(width * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Tramo [from, to] de un contorno cerrado; admite valores negativos (da la vuelta al inicio). */
private fun PathMeasure.slice(from: Float, to: Float, out: Path) {
    out.reset()
    val l = length
    when {
        to <= 0f -> getSegment(l + from, l + to, out, true)
        from >= 0f -> getSegment(from, to, out, true)
        else -> { getSegment(l + from, l, out, true); getSegment(0f, to, out, true) }
    }
}

// ───────────── Íconos (coordenadas 0..100) ─────────────

/** LÍNEAS: colectivo de perfil, ruedas girando, calle en movimiento y faro con haz de luz. */
private fun DrawScope.busIcon(t: Float, pulse: Float, glow: Float) {
    drawLine( // calle
        NeonCeleste.copy(alpha = 0.55f), Offset(0f, 83f), Offset(100f, 83f), 1.6f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), t * 192f)
    )
    translate(top = sin(t * TAU * 4f) * 0.8f) { // vibración leve del motor
        val beam = Path().apply { moveTo(84f, 52f); lineTo(100f, 43f); lineTo(100f, 65f); lineTo(84f, 58f); close() }
        drawPath(
            beam,
            Brush.horizontalGradient(
                listOf(NeonPink.copy(alpha = 0.2f + 0.4f * pulse), NeonPink.copy(alpha = 0f)), 84f, 100f
            )
        )
        val pos = Offset(4f, 22f); val dim = Size(78f, 44f); val rad = CornerRadius(9f)
        drawRoundRect(Ink, pos, dim, rad)
        neon(NeonCeleste, 3.2f, glow) { c, st -> drawRoundRect(c, pos, dim, rad, style = st) }
        for (x in listOf(10f, 27f, 44f)) { // ventanas
            neon(NeonCeleste, 1.9f, glow, layers = 2) { c, st ->
                drawRoundRect(c, Offset(x, 30f), Size(13f, 15f), CornerRadius(3f), style = st)
            }
        }
        neon(NeonCeleste, 1.9f, glow, layers = 2) { c, st -> // puerta
            drawRoundRect(c, Offset(62f, 30f), Size(14f, 28f), CornerRadius(3f), style = st)
        }
        drawLine(NeonCore.copy(alpha = 0.8f), Offset(69f, 31f), Offset(69f, 57f), 1.2f)
        drawLine(NeonPink, Offset(9f, 54f), Offset(55f, 54f), 2.2f, StrokeCap.Round)
        neon(NeonCeleste, 1.6f, glow, layers = 2) { c, st -> // cartel de destino
            drawRoundRect(c, Offset(58f, 15f), Size(22f, 7f), CornerRadius(2f), style = st)
        }
        for (i in 0..2) { // LEDs del cartel
            val on = (t * 6f).toInt() % 3 == i
            drawCircle(NeonPink.copy(alpha = if (on) 1f else 0.25f), 1.5f, Offset(64f + i * 6f, 18.5f))
        }
        drawCircle(NeonPink.copy(alpha = 0.25f + 0.35f * pulse), 6f, Offset(79.5f, 56f)) // faro
        drawCircle(NeonPink, 2.6f, Offset(79.5f, 56f))
        for (cx in listOf(22f, 68f)) { // ruedas
            val ctr = Offset(cx, 68f)
            drawCircle(Ink, 8.5f, ctr)
            neon(NeonCeleste, 3.2f, glow, layers = 3) { c, st -> drawCircle(c, 8.5f, ctr, style = st) }
            for (k in 0..2) {
                val a = t * TAU * 4f + k * TAU / 3f
                drawLine(NeonCore.copy(alpha = 0.85f), ctr, ctr + Offset(cos(a), sin(a)) * 6f, 1.2f, StrokeCap.Round)
            }
        }
    }
}

/** MAPA: mapa plegado, ruta punteada en marcha, pin que salta y ondas de radar. */
private fun DrawScope.mapIcon(t: Float, pulse: Float, glow: Float) {
    val map = Path().apply {
        moveTo(6f, 34f); lineTo(35f, 26f); lineTo(65f, 34f); lineTo(94f, 26f)
        lineTo(94f, 72f); lineTo(65f, 80f); lineTo(35f, 72f); lineTo(6f, 80f); close()
    }
    drawPath(map, Ink)
    neon(NeonCeleste, 3.2f, glow) { c, st -> drawPath(map, c, style = st) }
    neon(NeonCeleste, 1.5f, glow, 0.8f, 2) { c, st -> // pliegues
        drawLine(c, Offset(35f, 26f), Offset(35f, 72f), st.width, StrokeCap.Round)
        drawLine(c, Offset(65f, 34f), Offset(65f, 80f), st.width, StrokeCap.Round)
    }
    val route = Path().apply { moveTo(12f, 72f); lineTo(24f, 56f); lineTo(41f, 63f); lineTo(50f, 56f) }
    drawPath(
        route, NeonPink,
        style = Stroke(
            1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), (1f - t) * 32f)
        )
    )
    for (k in 0..1) { // ondas de radar
        val p = (t + k * 0.5f) % 1f
        val rx = 5f + 24f * p
        drawOval(
            NeonPink.copy(alpha = 0.8f * (1f - p)),
            Offset(50f - rx, 56f - rx * 0.35f), Size(rx * 2f, rx * 0.7f), style = Stroke(1.6f)
        )
    }
    translate(top = -5f * pulse) { // pin que salta
        val pin = Path().apply {
            moveTo(50f, 54f)
            cubicTo(31f, 40f, 38f, 14f, 50f, 14f)
            cubicTo(62f, 14f, 69f, 40f, 50f, 54f)
            close()
        }
        drawPath(pin, Ink)
        neon(NeonPink, 3.2f, glow) { c, st -> drawPath(pin, c, style = st) }
        drawCircle(NeonPink.copy(alpha = 0.25f + 0.4f * pulse), 8f, Offset(50f, 30f))
        drawCircle(NeonCore, 3.6f, Offset(50f, 30f))
    }
}

/** PARADAS CERCANAS: cartel de parada de colectivo, otra parada tenue y tu ubicación emitiendo pulsos. */
private fun DrawScope.stopIcon(t: Float, pulse: Float, glow: Float) {
    neon(NeonCeleste, 1.8f, glow, 0.5f, 2) { c, st -> // parada lejana
        drawLine(c, Offset(80f, 44f), Offset(80f, 71f), st.width, StrokeCap.Round)
        drawLine(c, Offset(73f, 71f), Offset(87f, 71f), st.width, StrokeCap.Round)
        drawRoundRect(c, Offset(71f, 30f), Size(18f, 14f), CornerRadius(3f), style = st)
    }
    drawCircle(NeonPink.copy(alpha = 0.5f), 2f, Offset(80f, 37f))

    val sign = Offset(14f, 8f); val signSize = Size(36f, 30f)
    drawRoundRect(Ink, sign, signSize, CornerRadius(6f))
    neon(NeonCeleste, 3.2f, glow) { c, st -> // cartel, poste y base
        drawRoundRect(c, sign, signSize, CornerRadius(6f), style = st)
        drawLine(c, Offset(32f, 38f), Offset(32f, 84f), st.width, StrokeCap.Round)
        drawLine(c, Offset(21f, 84f), Offset(43f, 84f), st.width, StrokeCap.Round)
    }
    // pictograma de colectivo (vista frontal) dentro del cartel
    drawRoundRect(NeonCore, Offset(23f, 14f), Size(18f, 16f), CornerRadius(3f), style = Stroke(1.6f))
    drawLine(NeonCore, Offset(25f, 20f), Offset(39f, 20f), 1.4f)
    drawCircle(NeonPink, 1.5f, Offset(27.5f, 25.5f))
    drawCircle(NeonPink, 1.5f, Offset(36.5f, 25.5f))
    drawLine(NeonCore, Offset(26f, 30f), Offset(26f, 33f), 2.4f, StrokeCap.Round)
    drawLine(NeonCore, Offset(38f, 30f), Offset(38f, 33f), 2.4f, StrokeCap.Round)
    drawCircle(NeonPink.copy(alpha = 0.15f + 0.25f * pulse), 4.5f, Offset(32f, 3.5f)) // baliza
    drawCircle(NeonPink.copy(alpha = 0.3f + 0.7f * pulse), 2.2f, Offset(32f, 3.5f))

    val me = Offset(60f, 80f) // tu posición
    val walk = PathEffect.dashPathEffect(floatArrayOf(1f, 5f), (1f - t) * 24f)
    drawLine(NeonPink, me, Offset(45f, 83f), 1.8f, StrokeCap.Round, walk)
    drawLine(NeonPink, me, Offset(77f, 72f), 1.8f, StrokeCap.Round, walk)
    drawCircle(NeonPink.copy(alpha = 0.8f * (1f - t)), 3f + 13f * t, me, style = Stroke(1.5f))
    drawCircle(NeonPink, 3.4f, me)
    drawCircle(NeonCore, 1.3f, me)
}

/** FAVORITOS: estrella que late, con estrella interior y destellos titilantes. */
private fun DrawScope.starIcon(t: Float, pulse: Float, glow: Float) {
    val mid = Offset(50f, 54f)
    val star = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 40f else 17f
            val a = -TAU / 4f + i * TAU / 10f
            val x = mid.x + r * cos(a)
            val y = mid.y + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    rotate(sin(t * TAU) * 5f, mid) {
        scale(1f + 0.06f * pulse, mid) {
            drawPath(star, NeonPink.copy(alpha = 0.10f + 0.25f * pulse))
            neon(NeonCeleste, 3.2f, glow) { c, st -> drawPath(star, c, style = st) }
            scale(0.42f, mid) { drawPath(star, NeonPink, style = Stroke(5f, join = StrokeJoin.Round)) }
        }
    }
    listOf(Offset(10f, 18f), Offset(92f, 22f), Offset(92f, 78f)).forEachIndexed { i, p ->
        val a = (sin(t * TAU * 2f + i * 2.1f) + 1f) / 2f
        val l = 2f + 5f * a
        val col = NeonCore.copy(alpha = 0.25f + 0.75f * a)
        drawLine(col, p - Offset(l, 0f), p + Offset(l, 0f), 1.5f, StrokeCap.Round)
        drawLine(col, p - Offset(0f, l), p + Offset(0f, l), 1.5f, StrokeCap.Round)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060912, widthDp = 380, heightDp = 700)
@Composable
private fun NeonMenuPreview() = NeonMenuScreen()