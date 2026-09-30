package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Ink = Color(0xFF060912)
private val Deep = Color(0xFF0A1220)
private val NeonCeleste = Color(0xFF19D9FF)
private val NeonPink = Color(0xFFFF2E9A)

/**
 * Fondo cyberpunk ESTÁTICO.
 *
 * No usa reloj, frame callbacks ni animaciones. Se mantiene visualmente
 * cyberpunk pero deja libre el ciclo de renderizado para el contenido.
 */
@Composable
fun CyberBackground(modifier: Modifier = Modifier, active: Boolean = true) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(Deep, Ink, Color(0xFF080E18))
            )
        )

        val horizonY = size.height * 0.62f
        val step = 28.dp.toPx()

        // Horizonte y rejilla fija.
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    NeonCeleste.copy(alpha = 0.06f),
                    NeonPink.copy(alpha = 0.035f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, horizonY - 80.dp.toPx()),
            size = Size(size.width, 160.dp.toPx())
        )

        var x = -step
        while (x < size.width + step) {
            drawLine(
                NeonCeleste.copy(alpha = 0.13f),
                Offset(x, horizonY),
                Offset(x, size.height),
                1.2f
            )
            x += step
        }

        var y = horizonY
        var row = 0
        while (y < size.height) {
            val a = 0.055f + 0.035f * (1f - (y - horizonY) / (size.height - horizonY + 1f))
            drawLine(
                NeonCeleste.copy(alpha = a),
                Offset(0f, y),
                Offset(size.width, y),
                1f
            )
            y += step * (0.7f + row * 0.08f)
            row++
        }

        val upperStep = 32.dp.toPx()
        var ux = 0f
        while (ux < size.width) {
            drawLine(
                NeonCeleste.copy(alpha = 0.055f),
                Offset(ux, 0f),
                Offset(ux, horizonY),
                1f
            )
            ux += upperStep
        }
        var uy = 0f
        while (uy < horizonY) {
            drawLine(
                NeonCeleste.copy(alpha = 0.04f),
                Offset(0f, uy),
                Offset(size.width, uy),
                1f
            )
            uy += upperStep
        }

        // Siluetas urbanas fijas.
        val buildingColor = Color(0xFF0C1828)
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
                        val col = if ((idx + (wxPos / wx).toInt()) % 3 == 0) {
                            NeonPink.copy(alpha = 0.28f)
                        } else {
                            NeonCeleste.copy(alpha = 0.32f)
                        }
                        drawRect(col, Offset(wxPos, wyPos), Size(wx * 0.7f, wy * 0.5f))
                    }
                    wxPos += wx + 3.dp.toPx()
                }
                wyPos += wy + 4.dp.toPx()
            }

            if (fh > 0.6f) {
                drawLine(
                    NeonPink.copy(alpha = 0.42f),
                    Offset(bx, top),
                    Offset(bx + bw, top),
                    2.dp.toPx()
                )
            }
        }

        drawLine(
            NeonCeleste.copy(alpha = 0.25f),
            Offset(0f, horizonY + 4.dp.toPx()),
            Offset(size.width, horizonY + 4.dp.toPx()),
            1.5.dp.toPx()
        )

        // Marcos fijos en las esquinas.
        val br = 22.dp.toPx()
        val thick = 2.5.dp.toPx()
        val cCol = NeonPink.copy(alpha = 0.52f)
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