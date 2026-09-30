package com.tucolectivo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transpuntano.app.model.TransitStop

@Composable
fun FavoritesComposeScreen(
    favorites: List<FavoriteStopUi>,
    onFavoriteClick: (FavoriteStopUi) -> Unit
) {
    val pulse = CyberAnimation.pulse(min = 0.28f, max = 1f)
    val sweep = CyberAnimation.sweep(durationMillis = 2100)
    val glow = CyberAnimation.glow(min = 0.42f, max = 1f)
    val scan = CyberAnimation.scan(durationMillis = 3200)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberColors.Background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = 28.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = CyberColors.Primary.copy(alpha = 0.035f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += grid
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = CyberColors.Secondary.copy(alpha = 0.025f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += grid
            }

            val scanY = size.height * scan.value
            drawLine(
                color = CyberColors.Primary.copy(alpha = 0.12f),
                start = androidx.compose.ui.geometry.Offset(0f, scanY),
                end = androidx.compose.ui.geometry.Offset(size.width, scanY),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FavoritesHudHeader(
                favoriteCount = favorites.size,
                pulse = pulse.value,
                sweep = sweep.value,
                glow = glow.value
            )

            Spacer(Modifier.height(10.dp))

            if (favorites.isEmpty()) {
                FavoritesEmptyState(pulse = pulse.value, glow = glow.value)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
                ) {
                    itemsIndexed(
                        items = favorites,
                        key = { _, favorite ->
                            "${favorite.lineCode}_${favorite.stopCode}_${favorite.description}"
                        }
                    ) { index, favorite ->
                        CyberFavoriteStopNode(
                            favorite = favorite,
                            index = index,
                            pulse = pulse.value,
                            sweep = sweep.value,
                            glow = glow.value,
                            onClick = { onFavoriteClick(favorite) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesHudHeader(
    favoriteCount: Int,
    pulse: Float,
    sweep: Float,
    glow: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CyberColors.SurfaceVariant,
                        CyberColors.Surface,
                        CyberColors.Background
                    )
                )
            )
            .padding(2.dp)
    ) {
        Canvas(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
            drawRoundRect(
                color = CyberColors.Secondary.copy(alpha = 0.38f + pulse * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            val x = size.width * sweep
            drawLine(
                color = CyberColors.Primary.copy(alpha = 0.22f + glow * 0.18f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 7.dp.toPx()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawRoundRect(
                        color = CyberColors.Secondary.copy(alpha = 0.68f + glow * 0.22f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = CyberColors.Secondary.copy(alpha = 0.14f + pulse * 0.12f),
                        radius = 15.dp.toPx()
                    )
                }
                Text(
                    "★",
                    color = CyberColors.Secondary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "03 // FAVORITES NODE",
                    color = CyberColors.Secondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "FAVORITOS",
                    color = CyberColors.OnSurface,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.2.sp
                )
                Text(
                    "RED PERSONAL // ACCESO PRIORITARIO",
                    color = CyberColors.Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "ONLINE",
                    color = CyberColors.Tertiary.copy(alpha = 0.7f + glow * 0.3f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "%02d".format(favoriteCount),
                    color = CyberColors.Primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "NODOS",
                    color = CyberColors.Muted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun CyberFavoriteStopNode(
    favorite: FavoriteStopUi,
    index: Int,
    pulse: Float,
    sweep: Float,
    glow: Float,
    onClick: () -> Unit
) {
    val accent = if (index % 2 == 0) CyberColors.Primary else CyberColors.Secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CyberColors.SurfaceVariant,
                        CyberColors.Surface,
                        CyberColors.Background
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(1.5.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = accent.copy(alpha = 0.30f + pulse * 0.30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            val x = size.width * sweep
            drawLine(
                color = accent.copy(alpha = 0.08f + glow * 0.10f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 6.dp.toPx()
            )

            val corner = 12.dp.toPx()
            drawLine(
                color = accent.copy(alpha = 0.85f),
                start = androidx.compose.ui.geometry.Offset(0f, corner),
                end = androidx.compose.ui.geometry.Offset(0f, 0f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square
            )
            drawLine(
                color = accent.copy(alpha = 0.85f),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(corner, 0f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberColors.Background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "%02d".format(index + 1),
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "NODE",
                    color = CyberColors.Muted,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LÍNEA ${favorite.lineCode} // ${favorite.lineName.uppercase()}",
                    color = accent.copy(alpha = 0.82f + glow * 0.18f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    favorite.description.uppercase(),
                    color = CyberColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                    maxLines = 2
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    listOf(favorite.street, favorite.intersection)
                        .filter { it.isNotBlank() }
                        .joinToString("  //  ")
                        .ifBlank { "UBICACIÓN REGISTRADA" }
                        .uppercase(),
                    color = CyberColors.Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.35.sp,
                    maxLines = 1
                )
            }

            Spacer(Modifier.size(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "★",
                    color = CyberColors.Secondary.copy(alpha = 0.55f + glow * 0.45f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "OPEN",
                    color = CyberColors.Tertiary.copy(alpha = 0.65f + pulse * 0.35f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp
                )
            }
        }
    }
}

@Composable
fun NearbyComposeScreen(
    stops: List<TransitStop>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onStopClick: (TransitStop) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("PARADAS CERCANAS")
        Button(
            onClick = onRefresh,
            enabled = !loading
        ) {
            Text(if (loading) "BUSCANDO..." else "ACTUALIZAR")
        }
        error?.let { Text(it) }
        if (!loading && stops.isEmpty() && error == null) {
            Text("SIN PARADAS CERCANAS", Modifier.padding(12.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stops) { stop ->
                    Button(
                        onClick = { onStopClick(stop) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stop.description)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesEmptyState(
    pulse: Float,
    glow: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberColors.Surface)
            .padding(2.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = CyberColors.Secondary.copy(alpha = 0.30f + pulse * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "★",
                color = CyberColors.Secondary.copy(alpha = 0.55f + glow * 0.45f),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "NO HAY NODOS GUARDADOS",
                color = CyberColors.OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "GUARDÁ UNA PARADA DESDE ARRIBOS\nPARA CONSTRUIR TU RED PERSONAL.",
                color = CyberColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                lineHeight = 15.sp
            )
        }
    }
}
