package com.tucolectivo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transpuntano.app.model.TransitArrival
import com.transpuntano.app.model.TransitIntersection
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.model.TransitStreet

private val NeonCeleste = Color(0xFF19D9FF)
private val NeonCore = Color(0xFFE8FCFF)
private val NeonPink = Color(0xFFFF2E9A)
private val Ink = Color(0xFF060912)
private val Muted = Color(0xFF6B7A8A)

enum class LinesLevel {
    CATALOG, STREETS, INTERSECTIONS, STOPS, ARRIVALS
}

@Composable
fun LinesFlowScreen(
    level: LinesLevel,
    lines: List<TransitLine>,
    linesLoading: Boolean,
    linesError: String?,
    onRefreshLines: () -> Unit,
    onLineClick: (TransitLine) -> Unit,
    activeLine: TransitLine?,
    streets: List<TransitStreet>,
    streetsLoading: Boolean,
    streetsError: String?,
    onStreetClick: (TransitStreet) -> Unit,
    onMapRoute: (TransitLine) -> Unit,
    activeStreet: TransitStreet?,
    intersections: List<TransitIntersection>,
    intersectionsLoading: Boolean,
    intersectionsError: String?,
    onIntersectionClick: (TransitIntersection) -> Unit,
    activeIntersection: TransitIntersection?,
    stops: List<TransitStop>,
    stopsLoading: Boolean,
    stopsError: String?,
    onStopClick: (TransitStop) -> Unit,
    activeStop: TransitStop?,
    arrivals: List<TransitArrival>,
    arrivalsLoading: Boolean,
    arrivalsError: String?,
    onRefreshArrivals: () -> Unit,
    onSaveFavorite: () -> Unit,
    onBackLevel: () -> Unit
) {
    when (level) {
        LinesLevel.CATALOG -> LinesComposeScreen(
            lines = lines, loading = linesLoading, error = linesError,
            onRefresh = onRefreshLines, onLineClick = onLineClick
        )
        LinesLevel.STREETS -> HierarchyListScreen(
            title = activeLine?.let { "LÍNEA ${it.code}" } ?: "CALLES",
            subtitle = activeLine?.name?.uppercase() ?: "RECORRIDO · CALLES",
            loading = streetsLoading, error = streetsError,
            emptyMessage = "NO SE ENCONTRARON CALLES", onBack = onBackLevel,
            extraActions = {
                if (activeLine != null) {
                    NeonListRow(title = "MAPA DEL RECORRIDO", subtitle = "VER EN MAPA",
                        accent = NeonPink, onClick = { onMapRoute(activeLine) })
                    Spacer(Modifier.height(8.dp))
                }
            },
            items = streets, key = { it.code },
            rowTitle = { it.name.uppercase() }, rowSubtitle = { "INTERSECCIONES" },
            onItemClick = onStreetClick
        )
        LinesLevel.INTERSECTIONS -> HierarchyListScreen(
            title = "INTERSECCIONES",
            subtitle = activeStreet?.name?.uppercase() ?: "",
            loading = intersectionsLoading, error = intersectionsError,
            emptyMessage = "NO SE ENCONTRARON INTERSECCIONES", onBack = onBackLevel,
            items = intersections, key = { it.code },
            rowTitle = { it.name.uppercase() }, rowSubtitle = { "PARADAS" },
            onItemClick = onIntersectionClick
        )
        LinesLevel.STOPS -> HierarchyListScreen(
            title = "PARADAS",
            subtitle = activeIntersection?.name?.uppercase() ?: "",
            loading = stopsLoading, error = stopsError,
            emptyMessage = "NO SE ENCONTRARON PARADAS", onBack = onBackLevel,
            items = stops, key = { it.code },
            rowTitle = { it.description.uppercase() },
            rowSubtitle = { listOf(it.street, it.intersection).filter { s -> s.isNotBlank() }.joinToString(" · ").uppercase() },
            onItemClick = onStopClick
        )
        LinesLevel.ARRIVALS -> ArrivalsComposeScreen(
            stop = activeStop, line = activeLine, arrivals = arrivals,
            loading = arrivalsLoading, error = arrivalsError,
            onBack = onBackLevel, onRefresh = onRefreshArrivals, onSaveFavorite = onSaveFavorite
        )
    }
}

@Composable
private fun <T> HierarchyListScreen(
    title: String, subtitle: String, loading: Boolean, error: String?,
    emptyMessage: String, onBack: () -> Unit,
    extraActions: @Composable () -> Unit = {},
    items: List<T>, key: (T) -> Any,
    rowTitle: (T) -> String, rowSubtitle: (T) -> String, onItemClick: (T) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.72f))) {
        HierarchyHeader(
            title = title,
            subtitle = when {
                loading -> "CARGANDO..."
                error != null -> error
                items.isEmpty() -> emptyMessage
                else -> subtitle.ifBlank { "${items.size} ÍTEMS" }
            },
            onBack = onBack
        )
        Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            when {
                loading && items.isEmpty() -> CenterMsg("CARGANDO...")
                error != null && items.isEmpty() -> CenterMsg(error)
                items.isEmpty() -> CenterMsg(emptyMessage)
                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { extraActions() }
                    items(items, key = key) { item ->
                        NeonListRow(
                            title = rowTitle(item), subtitle = rowSubtitle(item),
                            accent = NeonCeleste, onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrivalsComposeScreen(
    stop: TransitStop?, line: TransitLine?, arrivals: List<TransitArrival>,
    loading: Boolean, error: String?, onBack: () -> Unit,
    onRefresh: () -> Unit, onSaveFavorite: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.72f))) {
        HierarchyHeader(title = "ARRIBOS", subtitle = stop?.description?.uppercase() ?: "PARADA", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { NeonListRow(title = "ACTUALIZAR ARRIBOS", subtitle = "ACCIÓN", accent = NeonCeleste, onClick = onRefresh) }
            item { NeonListRow(title = "☆ GUARDAR PARADA", subtitle = "FAVORITOS", accent = NeonCeleste, onClick = onSaveFavorite) }
            when {
                loading && arrivals.isEmpty() -> item { CenterMsg("CONSULTANDO PRÓXIMOS ARRIBOS...") }
                error != null && arrivals.isEmpty() -> item { CenterMsg(error) }
                arrivals.isEmpty() && !loading -> item { CenterMsg("SIN ARRIBOS") }
                else -> items(arrivals.size) { index ->
                    val arrival = arrivals[index]
                    val lineLabel = if (arrival.line.isBlank()) "LÍNEA ${line?.code ?: ""}" else arrival.line
                    NeonArrivalCard(
                        lineLabel = lineLabel,
                        destination = arrival.destination.ifBlank { "DESTINO" },
                        minutes = arrival.minutes
                    )
                }
            }
        }
    }
}

@Composable
private fun HierarchyHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        NeonListRow(title = "← ATRÁS", subtitle = "VOLVER", accent = NeonCeleste, onClick = onBack)
        Spacer(Modifier.height(8.dp))
        Text(
            text = title, color = NeonCore, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.2.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle, color = Muted, fontFamily = FontFamily.Monospace,
            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CenterMsg(message: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}
