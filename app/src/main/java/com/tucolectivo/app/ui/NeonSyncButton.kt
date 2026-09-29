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
 * Botón cyberpunk "SINCRONIZAR LINEAS" dibujado 100 % con Canvas (Jetpack Compose + Material3).
 * Mismo lenguaje visual que NeonMenu.kt: marco neón con esquinas cortadas, scanlines,
 * destello que recorre el borde e ícono animado.
 *
 * Uso: NeonSyncButton(syncing = viewModel.syncing, onClick = { viewModel.sincronizar() })
 * Mientras syncing = true el color pasa de celeste a rosa, las flechas giran y aparece una barra de progreso.
 */

// ───────────── Paleta ─────────────
private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val TAU = (2.0 * PI).toFloat()

// ───────────── Botón ─────────────
@Composable
fun NeonSyncButton(
    modifier: Modifier = Modifier,
    text: String = "SINCRONIZAR LINEAS",
    syncingText: String = "SINCRONIZANDO...",
    syncing: Boolean = false,
    onClick: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "sync")
    val t by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t"
    )
    val pulse by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    val spin by infinite.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin"
    )
    val mix by animateFloatAsState(if (syncing) 1f else 0f, tween(300), label = "mix")

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")

    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer { val k = 1f - 0.03f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, enabled = !syncing, onClick = onClick)
    ) {
        // Los valores animados se leen solo al dibujar: no hay recomposición por frame.
        Canvas(Modifier.fillMaxSize()) {
            val flick = if (t in 0.62f..0.635f || t in 0.67f..0.68f) 0.45f else 1f
            val glow = (0.7f + 0.45f * pulse + 0.6f * press) * flick
            val accent = lerp(NeonCeleste, NeonPink, mix)

            val pad = 8.dp.toPx()
            val fw = size.width - 2 * pad
            val fh = size.height - 2 * pad
            val cut = fh * 0.34f
            val frame = Path().apply { // esquinas cortadas: arriba-izquierda y abajo-derecha
                moveTo(pad + cut, pad)
                lineTo(pad + fw, pad)
                lineTo(pad + fw, pad + fh - cut)
                lineTo(pad + fw - cut, pad + fh)
                lineTo(pad, pad + fh)
                lineTo(pad, pad + cut)
                close()
            }

            clipPath(frame) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0B2236), Ink)))
                var y = pad
                while (y < pad + fh) {
                    drawLine(accent.copy(alpha = 0.05f), Offset(pad, y), Offset(pad + fw, y), 1f)
                    y += 5.dp.toPx()
                }
                val band = 40.dp.toPx() // barra de escaneo vertical que cruza el botón
                val bx = pad + fw * t
                drawRect(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.20f), accent.copy(alpha = 0f)),
                        startX = bx - band, endX = bx + band
                    ),
                    topLeft = Offset(bx - band, pad), size = Size(band * 2, fh)
                )
                if (mix > 0f) { // barra de progreso indeterminada
                    val w = fw * 0.3f
                    val px = pad - w + (fw + w) * ((t * 2f) % 1f)
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(NeonPink.copy(alpha = 0f), NeonPink.copy(alpha = 0.9f * mix), NeonPink.copy(alpha = 0f)),
                            startX = px, endX = px + w
                        ),
                        topLeft = Offset(px, pad + fh - 3.dp.toPx()), size = Size(w, 3.dp.toPx())
                    )
                }
            }

            neon(accent, 2.4.dp.toPx(), glow, flick) { c, st -> drawPath(frame, c, style = st) }

            // destello con cola que recorre el borde
            val measure = PathMeasure().apply { setPath(frame, true) }
            val head = measure.length * t
            val tail = measure.length * 0.14f
            val seg = Path()
            val steps = 5
            for (k in 0 until steps) {
                measure.slice(head - tail * (k + 1) / steps, head - tail * k / steps, seg)
                neon(accent, 3.4.dp.toPx(), 1.8f, 1f - k / steps.toFloat(), layers = 2) { c, st ->
                    drawPath(seg, c, style = st)
                }
            }

            // indicadores HUD (esquina superior derecha)
            for (i in 0..2) {
                val on = (t * 9f).toInt() % 3 == i
                drawRect(
                    NeonPink.copy(alpha = if (on) 1f else 0.3f),
                    Offset(pad + fw - (30 - i * 9).dp.toPx(), pad + 6.dp.toPx()),
                    Size(6.dp.toPx(), 3.dp.toPx())
                )
            }

            // ícono (diseñado en una grilla de 100×100)
            val s = fh * 0.72f
            translate(pad + cut * 0.55f + 4.dp.toPx(), (size.height - s) / 2f) {
                scale(s / 100f, Offset.Zero) { syncIcon(if (syncing) spin else 0f, pulse, glow, accent) }
            }
        }

        Text(
            text = if (syncing) syncingText else text,
            color = NeonCore,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(if (syncing) NeonPink else NeonCeleste, Offset.Zero, 20f)),
            modifier = Modifier.align(Alignment.Center).padding(start = 44.dp, end = 12.dp)
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

// ───────────── Ícono (coordenadas 0..100) ─────────────

/** Dos flechas circulares que giran; núcleo rosa que late. */
private fun DrawScope.syncIcon(angle: Float, pulse: Float, glow: Float, accent: Color) {
    val c = Offset(50f, 50f)
    val r = 30f
    drawCircle(NeonPink.copy(alpha = 0.10f + 0.20f * pulse), 42f, c)
    rotate(angle, c) {
        for (k in 0..1) {
            val start = 25f + k * 180f
            val sweep = 130f
            neon(accent, 5f, glow, layers = 3) { col, st ->
                drawArc(col, start, sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = st)
            }
            val a = (start + sweep) * PI.toFloat() / 180f // punta de flecha al final del arco
            val nrm = Offset(cos(a), sin(a))
            val dir = Offset(-sin(a), cos(a))
            val tip = c + nrm * r
            val head = Path().apply {
                val p1 = tip + dir * 13f
                val p2 = tip + nrm * 10f
                val p3 = tip - nrm * 10f
                moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
            }
            drawPath(head, Ink)
            neon(accent, 3.4f, glow, layers = 2) { col, st -> drawPath(head, col, style = st) }
        }
    }
    drawCircle(NeonPink.copy(alpha = 0.3f + 0.4f * pulse), 8f, c)
    drawCircle(NeonCore, 3.4f, c)
}

@Preview(showBackground = true, backgroundColor = 0xFF060912, widthDp = 380, heightDp = 260)
@Composable
private fun NeonSyncButtonPreview() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NeonSyncButton()
        NeonSyncButton(syncing = true)
    }
}