package com.tucolectivo.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val Panel = Color(0xFF050714)
private val Muted = Color(0xFF6B7A8A)

@Composable
fun LinesComposeScreen(
    lines: List<TransitLine>, loading: Boolean, error: String?,
    onRefresh: () -> Unit, onLineClick: (TransitLine) -> Unit
) {
    SectionScaffold(
        title = "LÍNEAS",
        subtitle = when {
            loading -> "CARGANDO CATÁLOGO..."
            error != null -> error
            lines.isEmpty() -> "SIN DATOS"
            else -> "${lines.size} LÍNEAS DISPONIBLES"
        },
        actionLabel = if (loading) "..." else "ACTUALIZAR",
        onAction = onRefresh, actionEnabled = !loading
    ) {
        if (!loading && lines.isEmpty() && error == null) EmptyNeonMessage("NO SE ENCONTRARON LÍNEAS")
        else LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()
        ) {
            items(lines, key = { it.code }) { line ->
                NeonLineCard(line = line, index = lines.indexOf(line), onClick = { onLineClick(line) })
            }
        }
    }
}

@Composable
private fun NeonLineCard(line: TransitLine, index: Int, onClick: () -> Unit) {
    val cyberFont = rememberCyberpunkFontFamily()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(110), label = "linePress")
    val infinite = rememberInfiniteTransition(label = "lineCard")
    val scan by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing)), label = "lineScan")
    val pulse by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "linePulse")
    Box(
        Modifier.fillMaxWidth().height(86.dp)
            .graphicsLayer { val k = 1f - 0.025f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val p = 2.dp.toPx()
            val l = p; val t = p; val r = size.width - p; val b = size.height - p
            drawRect(Panel, Offset(l, t), Size(r - l, b - t))
            val glow = 0.35f + 0.3f * pulse + 0.4f * press
            drawRect(NeonCeleste.copy(alpha = 0.12f * glow), Offset(l, t), Size(r - l, b - t), style = Stroke(7.dp.toPx()))
            drawRect(NeonCeleste.copy(alpha = 0.7f + 0.2f * pulse), Offset(l, t), Size(r - l, b - t), style = Stroke(1.7.dp.toPx()))
            val x = l + (r - l) * scan
            drawRect(NeonPink.copy(alpha = 0.9f), Offset(x - 28.dp.toPx(), t), Size(56.dp.toPx(), 2.5.dp.toPx()))
            for (g in 0..3) {
                val y = t + 20.dp.toPx() + g * 13.dp.toPx()
                drawLine(NeonCeleste.copy(alpha = 0.055f), Offset(l + 76.dp.toPx(), y), Offset(r - 12.dp.toPx(), y), 1.dp.toPx())
            }
            drawRect(NeonPink.copy(alpha = 0.9f), Offset(l, t), Size(44.dp.toPx(), 3.dp.toPx()))
            drawRect(NeonPink.copy(alpha = 0.8f), Offset(r - 28.dp.toPx(), b - 3.dp.toPx()), Size(28.dp.toPx(), 3.dp.toPx()))
            for (d in 0..2) drawCircle(
                if ((index + d) % 2 == 0) NeonPink else NeonCeleste,
                (1.7f + pulse * 1.1f).dp.toPx(),
                Offset(r - 54.dp.toPx() + d * 14.dp.toPx(), b - 12.dp.toPx())
            )
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(58.dp).height(54.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(NeonCeleste.copy(alpha = 0.08f), Offset.Zero, Size(size.width, size.height))
                    drawRect(NeonCeleste.copy(alpha = 0.7f), Offset.Zero, Size(size.width, size.height), style = Stroke(1.dp.toPx()))
                    drawLine(NeonPink, Offset(6.dp.toPx(), size.height - 7.dp.toPx()), Offset(size.width - 6.dp.toPx(), size.height - 7.dp.toPx()), 2.dp.toPx())
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LÍNEA", color = NeonCeleste.copy(alpha = 0.72f), fontFamily = cyberFont, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text(line.code.toString(), color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Black, fontSize = 22.sp,
                        style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(NeonCeleste, Offset.Zero, 15f)))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(line.name.uppercase(), color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(24.dp).height(2.dp).background(NeonPink))
                    Spacer(Modifier.width(7.dp))
                    Text("RECORRIDO DISPONIBLE", color = NeonCeleste.copy(alpha = 0.68f), fontFamily = cyberFont, fontSize = 8.sp, letterSpacing = 1.sp)
                }
            }
            Text("›", color = NeonPink, fontFamily = cyberFont, fontWeight = FontWeight.Black, fontSize = 25.sp,
                style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(NeonPink, Offset.Zero, 14f)))
        }
    }
}

@Composable
fun FavoritesComposeScreen(favorites: List<FavoriteStopUi>, onFavoriteClick: (FavoriteStopUi) -> Unit) {
    SectionScaffold(
        title = "FAVORITOS",
        subtitle = if (favorites.isEmpty()) "NO HAY FAVORITOS" else "${favorites.size} PARADAS GUARDADAS",
        actionLabel = null, onAction = null
    ) {
        if (favorites.isEmpty()) EmptyNeonMessage("GUARDÁ PARADAS DESDE ARRIBOS O MAPA")
        else LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()
        ) {
            items(favorites, key = { "${it.lineCode}-${it.stopCode}" }) { fav ->
                NeonListRow(
                    title = fav.description.uppercase(),
                    subtitle = listOf(fav.street, fav.intersection).filter { it.isNotBlank() }.joinToString(" · ").uppercase(),
                    accent = NeonPink, onClick = { onFavoriteClick(fav) }
                )
            }
        }
    }
}

@Composable
fun NearbyComposeScreen(
    stops: List<TransitStop>, loading: Boolean, error: String?,
    onRefresh: () -> Unit, onStopClick: (TransitStop) -> Unit
) {
    SectionScaffold(
        title = "PARADAS CERCANAS",
        subtitle = when {
            loading -> "BUSCANDO CERCA TUYO..."
            error != null -> error
            stops.isEmpty() -> "SIN PARADAS CERCANAS"
            else -> "${stops.size} PARADAS DETECTADAS"
        },
        actionLabel = if (loading) "..." else "ACTUALIZAR",
        onAction = onRefresh, actionEnabled = !loading
    ) {
        if (!loading && stops.isEmpty() && error == null) EmptyNeonMessage("ACTIVÁ UBICACIÓN O ACERCATE A UNA PARADA")
        else LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()
        ) {
            items(stops, key = { it.code }) { stop ->
                val linesLabel = when {
                    stop.lineCodes.isNotEmpty() -> stop.lineCodes.joinToString(",")
                    stop.lineCode != 0 -> stop.lineCode.toString()
                    else -> "—"
                }
                NeonListRow(
                    title = stop.description.uppercase(),
                    subtitle = "${stop.street} · ${stop.intersection}".uppercase().ifBlank { "LÍNEAS $linesLabel" },
                    accent = NeonCeleste, onClick = { onStopClick(stop) }
                )
            }
        }
    }
}

@Composable
private fun SectionScaffold(
    title: String, subtitle: String, actionLabel: String?,
    onAction: (() -> Unit)?, actionEnabled: Boolean = true, content: @Composable () -> Unit
) {
    val cyberFont = rememberCyberpunkFontFamily()
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.88f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = "///  $title", color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Black,
                    fontSize = 17.sp, letterSpacing = 1.8.sp,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(NeonCeleste, Offset.Zero, 15f)))
                Text(text = subtitle, color = NeonCeleste.copy(alpha = 0.78f), fontFamily = cyberFont, fontSize = 10.sp, letterSpacing = 0.8.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (actionLabel != null && onAction != null) {
                NeonMiniButton(label = actionLabel, enabled = actionEnabled, onClick = onAction)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(5.dp)) {
            val y = size.height / 2f
            drawRect(NeonCeleste.copy(alpha = 0.12f), Offset(0f, y - 1.dp.toPx()), Size(size.width, 2.dp.toPx()))
            drawLine(NeonCeleste, Offset(12.dp.toPx(), y), Offset(size.width - 12.dp.toPx(), y), 1.2.dp.toPx())
            drawCircle(NeonPink, 2.2.dp.toPx(), Offset(14.dp.toPx(), y))
            drawCircle(NeonPink, 2.2.dp.toPx(), Offset(size.width - 14.dp.toPx(), y))
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun EmptyNeonMessage(message: String) {
    val cyberFont = rememberCyberpunkFontFamily()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Muted, fontFamily = cyberFont, fontSize = 12.sp,
            letterSpacing = 1.sp, modifier = Modifier.padding(24.dp))
    }
}

@Composable
fun NeonListRow(
    title: String, subtitle: String, accent: Color = NeonCeleste, onClick: () -> Unit
) {
    val cyberFont = rememberCyberpunkFontFamily()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")
    val infinite = rememberInfiniteTransition(label = "row")
    val pulse by infinite.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    val breath by infinite.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath"
    )

    Box(
        modifier = Modifier.fillMaxWidth().height(72.dp)
            .graphicsLayer { val k = 1f - 0.02f * press; scaleX = k; scaleY = k }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pad = 2.dp.toPx()
            val left = pad; val top = pad; val right = size.width - pad; val bottom = size.height - pad
            val cut = 10.dp.toPx()
            drawRect(Panel, Offset(left, top), Size(right - left, bottom - top))
            val frame = Path().apply {
                moveTo(left + cut, top); lineTo(right - cut, top); lineTo(right, top + cut)
                lineTo(right, bottom - cut); lineTo(right - cut, bottom); lineTo(left + cut, bottom)
                lineTo(left, bottom - cut); lineTo(left, top + cut); close()
            }
            val glow = (0.25f + 0.2f * breath + 0.35f * pulse + 0.35f * press)
            drawPath(frame, accent.copy(alpha = 0.12f * glow), style = Stroke(width = 6.dp.toPx()))
            drawPath(frame, accent.copy(alpha = 0.55f + 0.25f * press), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
            drawLine(accent.copy(alpha = 0.85f), Offset(left + 3.dp.toPx(), top + cut),
                Offset(left + 3.dp.toPx(), bottom - cut), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        Column(
            Modifier.fillMaxSize().padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, color = NeonCore, fontFamily = cyberFont, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(text = subtitle, color = Muted, fontFamily = cyberFont,
                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NeonMiniButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val cyberFont = rememberCyberpunkFontFamily()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(100), label = "mpress")

    Box(
        modifier = Modifier.height(32.dp)
            .graphicsLayer {
                val k = 1f - 0.04f * press; scaleX = k; scaleY = k
                alpha = if (enabled) 1f else 0.45f
            }
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val cut = 6.dp.toPx()
            val path = Path().apply {
                moveTo(cut, 0f); lineTo(size.width - cut, 0f); lineTo(size.width, cut)
                lineTo(size.width, size.height - cut); lineTo(size.width - cut, size.height)
                lineTo(cut, size.height); lineTo(0f, size.height - cut); lineTo(0f, cut); close()
            }
            drawPath(path, NeonCeleste.copy(alpha = 0.12f))
            drawPath(path, NeonCeleste.copy(alpha = 0.8f), style = Stroke(1.4.dp.toPx()))
        }
        Text(text = label, color = NeonCeleste, fontFamily = cyberFont, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 12.dp))
    }
}
