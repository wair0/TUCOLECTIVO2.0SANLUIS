package com.tucolectivo.app.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Barra inferior cyberpunk dibujada con Canvas (Jetpack Compose + Material3).
 * Pestañas: INICIO · LINEAS · MAPA · FAVORITO · CERCANAS.
 * Un haz de luz con marcador rosa se desliza (con resorte) hasta la pestaña activa; el ícono activo
 * se enciende con halo de neón y animación propia, y los inactivos quedan atenuados.
 *
 * Uso: var tab by remember { mutableIntStateOf(0) }
 *      Scaffold(bottomBar = { NeonBottomBar(tab, { tab = it }, Modifier.navigationBarsPadding()) })
 */

// ───────────── Paleta ─────────────
private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val TAU = (2.0 * PI).toFloat()

private val Tabs = listOf("INICIO", "LINEAS", "MAPA", "FAVORITO", "CERCANAS")
private val TabIcons: List<DrawScope.(Float, Float, Float, Float) -> Unit> = listOf(
    DrawScope::homeIcon, DrawScope::busIcon, DrawScope::mapIcon, DrawScope::starIcon, DrawScope::nearIcon
)

// ───────────── Barra ─────────────
@Composable
fun NeonBottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "bottom")
    val t = infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t")
    val pulse = infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    val pos by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.7f, stiffness = 300f), label = "pos")

    Box(modifier.fillMaxWidth().height(76.dp)) {
        // Los valores animados se leen solo al dibujar: no hay recomposición por frame.
        Canvas(Modifier.fillMaxSize()) {
            val tt = t.value
            val flick = if (tt in 0.62f..0.635f || tt in 0.67f..0.68f) 0.45f else 1f
            val glow = (0.7f + 0.45f * pulse.value) * flick
            val w = size.width
            val h = size.height
            val cut = 16.dp.toPx()
            val y = 3.dp.toPx()

            drawRect(Brush.verticalGradient(listOf(Color(0xFF0B2236), Ink)))
            val grid = NeonCeleste.copy(alpha = 0.05f)
            var gx = 0f
            while (gx <= w) { drawLine(grid, Offset(gx, 0f), Offset(gx, h)); gx += 28.dp.toPx() }

            // haz de luz que baja desde la línea hasta la pestaña activa
            val itemW = w / Tabs.size
            val cx = (pos + 0.5f) * itemW
            val bw = itemW * 0.86f
            drawRect(
                Brush.verticalGradient(listOf(NeonCeleste.copy(alpha = 0.22f), Color.Transparent), startY = y, endY = h),
                topLeft = Offset(cx - bw / 2f, y), size = Size(bw, h - y)
            )

            // línea superior con esquinas cortadas
            val line = Path().apply { moveTo(0f, y + cut); lineTo(cut, y); lineTo(w - cut, y); lineTo(w, y + cut) }
            neon(NeonCeleste, 2.dp.toPx(), glow, flick) { c, st -> drawPath(line, c, style = st) }

            // destello con cola que recorre la línea
            val m = PathMeasure().apply { setPath(line, false) }
            val head = m.length * tt
            val tail = m.length * 0.16f
            val seg = Path()
            val steps = 5
            for (k in 0 until steps) {
                val from = (head - tail * (k + 1) / steps).coerceAtLeast(0f)
                val to = head - tail * k / steps
                if (to > from) {
                    seg.reset()
                    m.getSegment(from, to, seg, true)
                    neon(NeonCeleste, 3.dp.toPx(), 1.8f, 1f - k / steps.toFloat(), layers = 2) { c, st ->
                        drawPath(seg, c, style = st)
                    }
                }
            }

            // marcador rosa de la pestaña activa
            neon(NeonPink, 3.dp.toPx(), 1.6f, layers = 3) { c, st ->
                drawLine(c, Offset(cx - bw * 0.32f, y), Offset(cx + bw * 0.32f, y), st.width, StrokeCap.Round)
            }
        }

        Row(Modifier.fillMaxSize().padding(top = 6.dp)) {
            Tabs.forEachIndexed { i, label ->
                BarItem(label, i == selected, t, pulse, Modifier.weight(1f), { onSelect(i) }, TabIcons[i])
            }
        }
    }
}

@Composable
private fun BarItem(
    label: String,
    selected: Boolean,
    t: State<Float>,
    pulse: State<Float>,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: DrawScope.(t: Float, pulse: Float, glow: Float, sel: Float) -> Unit
) {
    val sel by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "sel")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")

    Column(
        modifier
            .fillMaxHeight()
            .graphicsLayer { val k = 1f - 0.08f * press; scaleX = k; scaleY = k }
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(30.dp)) {
            val glow = (0.7f + 0.45f * pulse.value) * sel + 0.6f * press
            scale(size.width / 100f, Offset.Zero) { icon(t.value, pulse.value, glow, sel) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = lerp(NeonCeleste.copy(alpha = 0.55f), NeonCore, sel),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            style = TextStyle(shadow = Shadow(NeonCeleste.copy(alpha = sel), Offset.Zero, 16f * sel))
        )
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

/** Opacidad base del trazo: atenuada en reposo, plena al seleccionar (sel: 0..1). */
private fun dim(sel: Float) = 0.55f + 0.45f * sel

// ───────────── Íconos (coordenadas 0..100) ─────────────

/** INICIO: casa con luz rosa que late. */
private fun DrawScope.homeIcon(t: Float, pulse: Float, glow: Float, sel: Float) {
    val a = dim(sel)
    val house = Path().apply {
        moveTo(10f, 50f); lineTo(50f, 16f); lineTo(90f, 50f)
        moveTo(22f, 42f); lineTo(22f, 84f); lineTo(78f, 84f); lineTo(78f, 42f)
    }
    neon(NeonCeleste, 6f, glow, a, 3) { c, st -> drawPath(house, c, style = st) }
    neon(NeonCeleste, 4f, glow, a, 2) { c, st -> drawRoundRect(c, Offset(42f, 58f), Size(16f, 26f), CornerRadius(3f), style = st) }
    drawCircle(NeonPink.copy(alpha = (0.35f + 0.65f * pulse) * a), 4.5f, Offset(50f, 40f))
}

/** LINEAS: colectivo de perfil con ruedas que giran al estar activo. */
private fun DrawScope.busIcon(t: Float, pulse: Float, glow: Float, sel: Float) {
    val a = dim(sel)
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawRoundRect(c, Offset(8f, 22f), Size(84f, 46f), CornerRadius(10f), style = st) }
    neon(NeonCeleste, 3.4f, glow, a, 2) { c, st ->
        for (x in listOf(16f, 39f, 62f)) drawRoundRect(c, Offset(x, 32f), Size(17f, 15f), CornerRadius(3f), style = st)
    }
    drawLine(NeonPink.copy(alpha = a), Offset(16f, 58f), Offset(58f, 58f), 3.4f, StrokeCap.Round)
    for (cx in listOf(28f, 72f)) {
        val ctr = Offset(cx, 72f)
        drawCircle(Ink, 9f, ctr)
        neon(NeonCeleste, 5f, glow, a, 2) { c, st -> drawCircle(c, 9f, ctr, style = st) }
        val ang = t * TAU * 4f * sel
        drawLine(NeonCore.copy(alpha = a), ctr, ctr + Offset(cos(ang), sin(ang)) * 6f, 2f, StrokeCap.Round)
    }
    drawCircle(NeonPink.copy(alpha = (0.4f + 0.6f * pulse) * a), 3.4f, Offset(84f, 56f))
}

/** MAPA: mapa plegado con punto rosa y ondas de radar. */
private fun DrawScope.mapIcon(t: Float, pulse: Float, glow: Float, sel: Float) {
    val a = dim(sel)
    val map = Path().apply {
        moveTo(8f, 32f); lineTo(36f, 24f); lineTo(64f, 32f); lineTo(92f, 24f)
        lineTo(92f, 74f); lineTo(64f, 82f); lineTo(36f, 74f); lineTo(8f, 82f); close()
    }
    neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawPath(map, c, style = st) }
    neon(NeonCeleste, 3f, glow, a * 0.8f, 2) { c, st ->
        drawLine(c, Offset(36f, 24f), Offset(36f, 74f), st.width, StrokeCap.Round)
        drawLine(c, Offset(64f, 32f), Offset(64f, 82f), st.width, StrokeCap.Round)
    }
    val p = (t * 2f) % 1f
    drawCircle(NeonPink.copy(alpha = 0.8f * (1f - p) * sel), 5f + 14f * p, Offset(50f, 54f), style = Stroke(2f))
    drawCircle(NeonPink.copy(alpha = a), 5f, Offset(50f, 54f))
    drawCircle(NeonCore.copy(alpha = a), 2f, Offset(50f, 54f))
}

/** FAVORITO: estrella que late y se balancea al estar activa. */
private fun DrawScope.starIcon(t: Float, pulse: Float, glow: Float, sel: Float) {
    val a = dim(sel)
    val mid = Offset(50f, 54f)
    val star = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 42f else 18f
            val ang = -TAU / 4f + i * TAU / 10f
            val x = mid.x + r * cos(ang)
            val y = mid.y + r * sin(ang)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    rotate(sin(t * TAU) * 6f * sel, mid) {
        scale(1f + 0.07f * pulse * sel, mid) {
            drawPath(star, NeonPink.copy(alpha = (0.08f + 0.22f * pulse) * sel))
            neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawPath(star, c, style = st) }
            scale(0.4f, mid) { drawPath(star, NeonPink.copy(alpha = a), style = Stroke(6f, join = StrokeJoin.Round)) }
        }
    }
}

/** CERCANAS: pin de ubicación que salta sobre ondas de radar. */
private fun DrawScope.nearIcon(t: Float, pulse: Float, glow: Float, sel: Float) {
    val a = dim(sel)
    for (k in 0..1) {
        val p = (t * 2f + k * 0.5f) % 1f
        val rx = 10f + 30f * p
        drawOval(
            NeonPink.copy(alpha = 0.7f * (1f - p) * sel),
            Offset(50f - rx, 86f - rx * 0.3f), Size(rx * 2f, rx * 0.6f), style = Stroke(2.4f)
        )
    }
    translate(top = -3f * pulse * sel) {
        val pin = Path().apply {
            moveTo(50f, 80f)
            cubicTo(24f, 56f, 32f, 12f, 50f, 12f)
            cubicTo(68f, 12f, 76f, 56f, 50f, 80f)
            close()
        }
        drawPath(pin, Ink)
        neon(NeonCeleste, 5f, glow, a, 3) { c, st -> drawPath(pin, c, style = st) }
        drawCircle(NeonPink.copy(alpha = (0.3f + 0.4f * pulse) * a), 11f, Offset(50f, 40f))
        drawCircle(NeonCore.copy(alpha = a), 4.4f, Offset(50f, 40f))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060912, widthDp = 380, heightDp = 120)
@Composable
private fun NeonBottomBarPreview() {
    var tab by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        NeonBottomBar(selected = tab, onSelect = { tab = it })
    }
}