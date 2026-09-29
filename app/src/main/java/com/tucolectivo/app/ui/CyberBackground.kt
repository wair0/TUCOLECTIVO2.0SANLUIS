package com.tucolectivo.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val Deep = Color(0xFF0A1220)

/**
 * Fondo cyberpunk 100% Jetpack Compose + Canvas.
 * Reemplaza el GIF (AndroidView) que interfería con las animaciones Compose.
 * Grid, scan, siluetas de ciudad y partículas — todo en el mismo pipeline.
 */
@Composable
fun CyberBackground(modifier: Modifier = Modifier, active: Boolean = true) {
    if (!active) {
        Canvas(modifier.fillMaxSize()) { drawRect(Ink) }
        return
    }

    val infinite = rememberInfiniteTransition(label = "cyberBg")
    val sweep by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val gridShift by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "grid"
    )
    val flicker by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(90, easing = LinearEasing)),
        label = "flicker"
    )

    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Deep, Ink, Color(0xFF080E18))))

        val horizonY = size.height * 0.62f
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    NeonCeleste.copy(alpha = 0.06f + 0.04f * pulse),
                    NeonPink.copy(alpha = 0.04f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, horizonY - 80.dp.toPx()),
            size = Size(size.width, 160.dp.toPx())
        )

        val step = 28.dp.toPx()
        val shift = gridShift * step
        val gridA = 0.10f + 0.06f * pulse
        var x = -step + (shift % step)
        while (x < size.width + step) {
            drawLine(NeonCeleste.copy(alpha = gridA), Offset(x, horizonY), Offset(x, size.height), 1.2f)
            x += step
        }
        var y = horizonY
        var row = 0
        while (y < size.height) {
            val a = gridA * (0.4f + 0.6f * (1f - (y - horizonY) / (size.height - horizonY + 1f)))
            drawLine(NeonCeleste.copy(alpha = a), Offset(0f, y), Offset(size.width, y), 1f)
            y += step * (0.7f + row * 0.08f)
            row++
        }

        val upperStep = 32.dp.toPx()
        var ux = 0f
        while (ux < size.width) {
            drawLine(NeonCeleste.copy(alpha = 0.05f + 0.03f * pulse), Offset(ux, 0f), Offset(ux, horizonY), 1f)
            ux += upperStep
        }
        var uy = 0f
        while (uy < horizonY) {
            drawLine(NeonCeleste.copy(alpha = 0.04f), Offset(0f, uy), Offset(size.width, uy), 1f)
            uy += upperStep
        }

        val buildingColor = Color(0xFF0C1828)
        val windowOn = NeonCeleste.copy(alpha = 0.35f + 0.25f * pulse)
        val windowPink = NeonPink.copy(alpha = 0.30f + 0.20f * pulse)
        val baseH = size.height - horizonY
        val buildings = listOf(
            0.02f to 0.55f, 0.10f to 0.40f, 0.16f to 0.70f, 0.24f to 0.35f,
            0.30f to 0.60f, 0.38f to 0.45f, 0.45f to 0.75f, 0.54f to 0.38f,
            0.62f to 0.65f, 0.70f to 0.42f, 0.78f to 0.58f, 0.86f to 0.48f,
            0.93f to 0.68f
        )
        buildings.forEachIndexed { idx, (fx, fh) ->
            val bx = size.width * fx
            val bw = size.width * 0.07f
            val bh = baseH * fh
            val top = size.height - bh
            drawRect(buildingColor, Offset(bx, top), Size(bw, bh))
            val wx = 6.dp.toPx()
            val wy = 8.dp.toPx()
            var wyPos = top + 10.dp.toPx()
            while (wyPos < size.height - 8.dp.toPx()) {
                var wxPos = bx + 4.dp.toPx()
                while (wxPos < bx + bw - 4.dp.toPx()) {
                    val lit = ((idx * 7 + (wyPos / wy).toInt() * 3 + (wxPos / wx).toInt()) % 5) != 0
                    if (lit) {
                        val col = if ((idx + (wxPos / wx).toInt()) % 3 == 0) windowPink else windowOn
                        val flickOn = if ((idx * 13 + (wyPos / wy).toInt()) % 11 == 0) {
                            if (flicker > 0.5f) col else col.copy(alpha = col.alpha * 0.3f)
                        } else col
                        drawRect(flickOn, Offset(wxPos, wyPos), Size(wx * 0.7f, wy * 0.5f))
                    }
                    wxPos += wx + 3.dp.toPx()
                }
                wyPos += wy + 4.dp.toPx()
            }
            if (fh > 0.6f) {
                drawLine(
                    NeonPink.copy(alpha = 0.4f + 0.3f * pulse),
                    Offset(bx, top),
                    Offset(bx + bw, top),
                    2.dp.toPx()
                )
            }
        }

        drawLine(
            NeonCeleste.copy(alpha = 0.25f + 0.15f * pulse),
            Offset(0f, horizonY + 4.dp.toPx()),
            Offset(size.width, horizonY + 4.dp.toPx()),
            1.5.dp.toPx()
        )

        val bandH = 48.dp.toPx()
        val sy = size.height * sweep
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    NeonPink.copy(alpha = 0.18f + 0.12f * pulse),
                    NeonCeleste.copy(alpha = 0.12f),
                    Color.Transparent
                )
            ),
            Offset(0f, sy - bandH),
            Size(size.width, bandH * 2)
        )
        drawLine(
            NeonPink.copy(alpha = 0.55f + 0.25f * pulse),
            Offset(0f, sy),
            Offset(size.width, sy),
            1.5.dp.toPx()
        )

        for (i in 0..14) {
            val px = size.width * ((sweep * 0.4f + i * 0.08f + gridShift * 0.1f) % 1f)
            val py = size.height * 0.08f + i * (size.height * 0.05f)
            val r = 1.5.dp.toPx() + pulse * 1.5.dp.toPx()
            drawCircle(NeonCeleste.copy(alpha = 0.30f + 0.25f * pulse), r, Offset(px, py))
        }

        val br = 20.dp.toPx()
        val thick = 2.dp.toPx()
        val cornerA = 0.45f + 0.25f * pulse
        val cCol = NeonPink.copy(alpha = cornerA)
        drawLine(cCol, Offset(6f, 6f), Offset(6f + br, 6f), thick)
        drawLine(cCol, Offset(6f, 6f), Offset(6f, 6f + br), thick)
        drawLine(cCol, Offset(size.width - 6f - br, 6f), Offset(size.width - 6f, 6f), thick)
        drawLine(cCol, Offset(size.width - 6f, 6f), Offset(size.width - 6f, 6f + br), thick)
        drawLine(cCol, Offset(6f, size.height - 6f), Offset(6f + br, size.height - 6f), thick)
        drawLine(cCol, Offset(6f, size.height - 6f - br), Offset(6f, size.height - 6f), thick)
        drawLine(cCol, Offset(size.width - 6f - br, size.height - 6f), Offset(size.width - 6f, size.height - 6f), thick)
        drawLine(cCol, Offset(size.width - 6f, size.height - 6f - br), Offset(size.width - 6f, size.height - 6f), thick)
    }
}
