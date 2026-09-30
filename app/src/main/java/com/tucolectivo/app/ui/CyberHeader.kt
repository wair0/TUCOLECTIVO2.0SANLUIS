package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class HeaderPanel { NONE, MENU, SEARCH, NOTIFICATIONS }

@Composable
fun CyberHeader(
    statusText: String,
    onNavigate: (Int) -> Unit,
    onSearch: (String) -> Unit
) {
    var panel by remember { mutableStateOf(HeaderPanel.NONE) }
    var query by remember { mutableStateOf("") }
    val pulse by CyberAnimation.pulse(min = 0.35f, max = 1f)
    val sweep by CyberAnimation.sweep()
    val glitch by CyberAnimation.glitch(amplitude = 1.6f)

    Column(
        Modifier
            .fillMaxWidth()
            .background(CyberColors.Background)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            Canvas(Modifier.fillMaxWidth().height(112.dp)) {
                val pad = 8.dp.toPx()
                val left = pad
                val top = pad
                val right = size.width - pad
                val bottom = size.height - pad

                drawRoundRect(
                    color = CyberColors.Surface,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawRoundRect(
                    color = CyberColors.Primary.copy(alpha = 0.10f + 0.07f * pulse),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(7.dp.toPx())
                )
                drawRoundRect(
                    color = CyberColors.Primary.copy(alpha = 0.65f + 0.25f * pulse),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(1.6.dp.toPx())
                )

                val scanX = left + (right - left) * sweep
                drawLine(
                    color = CyberColors.Secondary.copy(alpha = 0.85f),
                    start = Offset(scanX, top + 2.dp.toPx()),
                    end = Offset(scanX, bottom - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val bracket = 18.dp.toPx()
                drawLine(CyberColors.Secondary, Offset(left, top + bracket), Offset(left, top), 2.dp.toPx())
                drawLine(CyberColors.Secondary, Offset(left, top), Offset(left + bracket, top), 2.dp.toPx())
                drawLine(CyberColors.Secondary, Offset(right - bracket, bottom), Offset(right, bottom), 2.dp.toPx())
                drawLine(CyberColors.Secondary, Offset(right, bottom - bracket), Offset(right, bottom), 2.dp.toPx())

                val dotAlpha = 0.45f + 0.55f * pulse
                drawCircle(CyberColors.Tertiary.copy(alpha = dotAlpha), 4.dp.toPx(), Offset(left + 18.dp.toPx(), bottom - 18.dp.toPx()))
                drawCircle(CyberColors.Tertiary.copy(alpha = dotAlpha * 0.25f), 9.dp.toPx(), Offset(left + 18.dp.toPx(), bottom - 18.dp.toPx()))
            }

            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp, end = 168.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    "TUCOLECTIVO",
                    color = CyberColors.OnSurface,
                    fontWeight = FontWeight.Black,
                    fontSize = 25.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = glitch.dp)
                )
                Text(
                    "SISTEMA DE TRANSPORTE",
                    color = CyberColors.Primary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 2.2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    statusText,
                    color = CyberColors.Muted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }

            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeaderAction("☰", panel == HeaderPanel.MENU) {
                    panel = if (panel == HeaderPanel.MENU) HeaderPanel.NONE else HeaderPanel.MENU
                }
                HeaderAction("⌕", panel == HeaderPanel.SEARCH) {
                    panel = if (panel == HeaderPanel.SEARCH) HeaderPanel.NONE else HeaderPanel.SEARCH
                }
                HeaderAction("◉", panel == HeaderPanel.NOTIFICATIONS) {
                    panel = if (panel == HeaderPanel.NOTIFICATIONS) HeaderPanel.NONE else HeaderPanel.NOTIFICATIONS
                }
            }
        }

        when (panel) {
            HeaderPanel.MENU -> HeaderPanelCard {
                listOf(
                    "INICIO" to 0,
                    "LÍNEAS" to 1,
                    "MAPA" to 2,
                    "FAVORITOS" to 3,
                    "PARADAS CERCANAS" to 4
                ).forEach { (label, index) ->
                    HeaderPanelRow(label) {
                        panel = HeaderPanel.NONE
                        onNavigate(index)
                    }
                }
            }
            HeaderPanel.SEARCH -> HeaderSearch(
                query = query,
                onQueryChange = { query = it },
                onSubmit = {
                    panel = HeaderPanel.NONE
                    onSearch(query)
                }
            )
            HeaderPanel.NOTIFICATIONS -> HeaderPanelCard {
                HeaderPanelRow("● SISTEMA LISTO") {}
                HeaderPanelRow("● DATOS SINCRONIZADOS") {}
                HeaderPanelRow("● RED OPERATIVA") {}
            }
            HeaderPanel.NONE -> Unit
        }
    }
}

@Composable
private fun HeaderAction(label: String, active: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by CyberAnimation.transition(if (pressed) 1f else 0f)
    Box(
        Modifier
            .size(40.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(40.dp)) {
            val alpha = 0.55f + 0.25f * press + if (active) 0.2f else 0f
            drawRoundRect(
                color = CyberColors.Primary.copy(alpha = 0.07f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(1.2.dp.toPx())
            )
            drawRoundRect(
                color = if (active) CyberColors.Secondary else CyberColors.Primary,
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke((1.1f + press).dp.toPx())
            )
            drawCircle(
                color = CyberColors.Primary.copy(alpha = alpha),
                radius = 2.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
        Text(label, color = if (active) CyberColors.Secondary else CyberColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeaderPanelCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(CyberColors.SurfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) { content() }
}

@Composable
private fun HeaderPanelRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = CyberColors.OnSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    )
}

@Composable
private fun HeaderSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("BUSCAR PARADA, CALLE O LÍNEA") }
        )
        Text(
            "OK",
            color = CyberColors.Primary,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clickable(onClick = onSubmit)
                .padding(12.dp)
        )
    }
}
