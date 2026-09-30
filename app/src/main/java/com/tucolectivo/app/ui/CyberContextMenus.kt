package com.tucolectivo.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val PanelBg = Color(0xFF0A1220)

enum class HeaderPanel { None, Menu, Search, Notifications }

@Composable
fun CyberContextOverlays(
    panel: HeaderPanel,
    menuItems: List<String>,
    notifications: List<NeonNotification>,
    onClose: () -> Unit,
    onMenuItem: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onNotification: (Int) -> Unit,
    animationsEnabled: Boolean = true
) {
    if (panel == HeaderPanel.None) return

    val cyberFont = rememberCyberpunkFontFamily()
    val infinite = if (animationsEnabled) rememberInfiniteTransition(label = "overlay") else null
    val pulse by if (animationsEnabled) infinite!!.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse") else rememberUpdatedState(0f)
    val scan by if (animationsEnabled) infinite!!.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "scan") else rememberUpdatedState(0f)

    Box(
        Modifier
            .fillMaxSize()
            .zIndex(200f)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
    ) {
        val align = when (panel) {
            HeaderPanel.Menu -> Alignment.TopStart
            else -> Alignment.TopEnd
        }
        val accent = when (panel) {
            HeaderPanel.Search -> NeonPink
            else -> NeonCeleste
        }
        val title = when (panel) {
            HeaderPanel.Menu -> "MENÚ PRINCIPAL"
            HeaderPanel.Search -> "BUSCAR"
            HeaderPanel.Notifications -> "NOTIFICACIONES"
            HeaderPanel.None -> ""
        }
        val width: Dp = when (panel) {
            HeaderPanel.Menu -> 260.dp
            else -> 310.dp
        }

        Box(
            Modifier
                .align(align)
                .padding(top = 96.dp, start = 10.dp, end = 10.dp)
                .width(width)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { })
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val inset = 4.dp.toPx()
                        val r = size.width - inset
                        val b = size.height - inset
                        val frame = Path().apply {
                            moveTo(inset, inset); lineTo(r, inset); lineTo(r, b); lineTo(inset, b); close()
                        }
                        clipPath(frame) {
                            drawRect(PanelBg)
                            var y = inset
                            while (y < b) {
                                drawLine(accent.copy(alpha = 0.07f), Offset(inset, y), Offset(r, y), 1f)
                                y += 4.dp.toPx()
                            }
                            val band = 28.dp.toPx()
                            val by = inset + (b - inset) * scan
                            drawRect(
                                Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.18f + 0.1f * pulse), Color.Transparent)),
                                Offset(inset, by - band),
                                Size(r - inset, band * 2)
                            )
                        }
                        for (i in 4 downTo 1) {
                            drawPath(frame, accent.copy(alpha = 0.08f * i * (0.7f + 0.3f * pulse)), style = Stroke(width = 1.6.dp.toPx() * (1f + i * 0.9f)))
                        }
                        drawPath(frame, accent, style = Stroke(width = 1.8.dp.toPx()))
                        drawPath(frame, NeonCore.copy(alpha = 0.7f), style = Stroke(width = 0.7.dp.toPx()))
                        val tick = 14.dp.toPx(); val thick = 2.5.dp.toPx()
                        drawLine(NeonPink, Offset(inset, inset), Offset(inset + tick, inset), thick)
                        drawLine(NeonPink, Offset(inset, inset), Offset(inset, inset + tick), thick)
                        drawLine(NeonPink, Offset(r - tick, inset), Offset(r, inset), thick)
                        drawLine(NeonPink, Offset(r, inset), Offset(r, inset + tick), thick)
                    }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(title, color = NeonPink, fontFamily = cyberFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(accent.copy(alpha = 0.4f + 0.3f * pulse)))
                Spacer(Modifier.height(8.dp))

                when (panel) {
                    HeaderPanel.Menu -> {
                        menuItems.forEachIndexed { i, label ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onClose(); onMenuItem(i) }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(width = 8.dp, height = 3.dp).background(NeonPink))
                                Spacer(Modifier.width(12.dp))
                                Text(label, color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp, style = TextStyle(shadow = Shadow(NeonCeleste, Offset.Zero, 12f)))
                            }
                        }
                    }
                    HeaderPanel.Search -> CyberSearchField(onSearch = { onClose(); onSearch(it) })
                    HeaderPanel.Notifications -> {
                        if (notifications.isEmpty()) {
                            Text("SIN NOTIFICACIONES", color = NeonCeleste.copy(alpha = 0.65f), fontFamily = cyberFont, fontSize = 12.sp, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                                notifications.forEachIndexed { i, n ->
                                    Row(Modifier.fillMaxWidth().clickable { onClose(); onNotification(i) }.padding(vertical = 10.dp)) {
                                        Box(Modifier.padding(top = 3.dp).size(8.dp).background(if (n.unread) NeonPink else NeonCeleste.copy(alpha = 0.35f)))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(n.title, color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(n.detail, color = NeonCeleste.copy(alpha = 0.85f), fontFamily = cyberFont, fontSize = 11.sp)
                                            Text(n.time, color = NeonCeleste.copy(alpha = 0.5f), fontFamily = cyberFont, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HeaderPanel.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun CyberSearchField(onSearch: (String) -> Unit) {
    val cyberFont = rememberCyberpunkFontFamily()
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val mono = TextStyle(color = NeonCore, fontFamily = cyberFont, fontSize = 14.sp, letterSpacing = 1.sp)
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp).drawBehind {
            val f = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(f, Ink)
            for (i in 3 downTo 1) drawPath(f, NeonCeleste.copy(alpha = 0.1f * i), style = Stroke(width = 1.4.dp.toPx() * (1f + i)))
            drawPath(f, NeonCeleste, style = Stroke(width = 1.4.dp.toPx()))
        }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⌕", color = NeonCeleste, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = mono,
            cursorBrush = SolidColor(NeonPink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            modifier = Modifier.weight(1f).focusRequester(focus),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("LÍNEA O PARADA", style = mono.copy(color = NeonCeleste.copy(alpha = 0.4f)))
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Text("✕", color = NeonPink, fontSize = 16.sp, modifier = Modifier.clickable { query = "" }.padding(4.dp))
        }
    }
}
