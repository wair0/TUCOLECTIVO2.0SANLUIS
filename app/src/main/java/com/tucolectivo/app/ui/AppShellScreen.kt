package com.tucolectivo.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.transpuntano.app.model.TransitArrival
import com.transpuntano.app.model.TransitIntersection
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.model.TransitStreet
import com.transpuntano.app.ui.CyberMapView

/**
 * FASE 1: shell funcional mínimo.
 *
 * La arquitectura visual cyberpunk anterior fue retirada.
 * Esta pantalla es deliberadamente simple y temporal: conserva navegación,
 * callbacks y acceso al mapa mientras se construye el nuevo sistema visual.
 *
 * IMPORTANTE: CyberMapView no forma parte de esta reconstrucción.
 */
@Composable
fun AppShellScreen(
    section: Int,
    onNavigate: (Int) -> Unit,
    syncing: Boolean,
    onSync: () -> Unit,
    statusText: String = "● SISTEMA LISTO",
    onSearch: (String) -> Unit = {},
    linesLevel: LinesLevel,
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
    onLinesBack: () -> Unit,
    favorites: List<FavoriteStopUi>,
    onFavoriteClick: (FavoriteStopUi) -> Unit,
    nearby: List<TransitStop>,
    nearbyLoading: Boolean,
    nearbyError: String?,
    onRefreshNearby: () -> Unit,
    onNearbyClick: (TransitStop) -> Unit,
    mapFactory: () -> CyberMapView,
    mapKey: Any? = null
) {
    CyberpunkTheme {
        Surface(Modifier.fillMaxSize(), color = CyberColors.Background) {
            Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("INICIO", "LÍNEAS", "MAPA", "FAVORITOS", "CERCANAS").forEachIndexed { index, label ->
                    Button(onClick = { onNavigate(index) }) { Text(label) }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (section) {
                    0 -> InicioBase(syncing, statusText, onSync, onNavigate)
                    1 -> LinesBase(
                        level = linesLevel, lines = lines, linesLoading = linesLoading, linesError = linesError,
                        onRefreshLines = onRefreshLines, onLineClick = onLineClick, activeLine = activeLine,
                        streets = streets, streetsLoading = streetsLoading, streetsError = streetsError,
                        onStreetClick = onStreetClick, onMapRoute = onMapRoute, activeStreet = activeStreet,
                        intersections = intersections, intersectionsLoading = intersectionsLoading, intersectionsError = intersectionsError,
                        onIntersectionClick = onIntersectionClick, activeIntersection = activeIntersection,
                        stops = stops, stopsLoading = stopsLoading, stopsError = stopsError,
                        onStopClick = onStopClick, activeStop = activeStop, arrivals = arrivals,
                        arrivalsLoading = arrivalsLoading, arrivalsError = arrivalsError,
                        onRefreshArrivals = onRefreshArrivals, onSaveFavorite = onSaveFavorite, onBack = onLinesBack
                    )
                    2 -> MapHost(mapFactory, mapKey)
                    3 -> SimpleList("FAVORITOS", favorites, "NO HAY FAVORITOS", onFavoriteClick) { "\${it.description} · \${it.lineName}" }
                    4 -> SimpleList("PARADAS CERCANAS", nearby, if (nearbyLoading) "BUSCANDO..." else nearbyError ?: "SIN PARADAS", onNearbyClick) { it.description }
                    else -> InicioBase(syncing, statusText, onSync, onNavigate)
                }
            }
        }
            }
        }
    }
}

@Composable
private fun InicioBase(syncing: Boolean, statusText: String, onSync: () -> Unit, onNavigate: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("BASE VISUAL FASE 1", style = MaterialTheme.typography.headlineSmall)
        Text(statusText)
        Button(onClick = onSync, enabled = !syncing) { Text(if (syncing) "SINCRONIZANDO..." else "SINCRONIZAR") }
        Text("La nueva arquitectura visual se construirá después de esta limpieza.")
        Button(onClick = { onNavigate(1) }) { Text("LÍNEAS") }
        Button(onClick = { onNavigate(2) }) { Text("MAPA") }
        Button(onClick = { onNavigate(3) }) { Text("FAVORITOS") }
        Button(onClick = { onNavigate(4) }) { Text("PARADAS CERCANAS") }
    }
}

@Composable
private fun LinesBase(
    level: LinesLevel, lines: List<TransitLine>, linesLoading: Boolean, linesError: String?,
    onRefreshLines: () -> Unit, onLineClick: (TransitLine) -> Unit, activeLine: TransitLine?,
    streets: List<TransitStreet>, streetsLoading: Boolean, streetsError: String?,
    onStreetClick: (TransitStreet) -> Unit, onMapRoute: (TransitLine) -> Unit, activeStreet: TransitStreet?,
    intersections: List<TransitIntersection>, intersectionsLoading: Boolean, intersectionsError: String?,
    onIntersectionClick: (TransitIntersection) -> Unit, activeIntersection: TransitIntersection?,
    stops: List<TransitStop>, stopsLoading: Boolean, stopsError: String?,
    onStopClick: (TransitStop) -> Unit, activeStop: TransitStop?, arrivals: List<TransitArrival>,
    arrivalsLoading: Boolean, arrivalsError: String?, onRefreshArrivals: () -> Unit,
    onSaveFavorite: () -> Unit, onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("LÍNEAS · $level", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack) { Text("VOLVER") }
        when (level) {
            LinesLevel.CATALOG -> {
                Button(onClick = onRefreshLines, enabled = !linesLoading) { Text(if (linesLoading) "CARGANDO..." else "ACTUALIZAR") }
                if (linesError != null) Text(linesError)
                LazyColumn { items(lines) { line ->
                    Button(onClick = { onLineClick(line) }, Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("\${line.code} · \${line.name}")
                    }
                } }
            }
            LinesLevel.STREETS -> {
                Text(activeLine?.name ?: "")
                LazyColumn { items(streets) { item ->
                    Button(onClick = { onStreetClick(item) }, Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(item.name) }
                } }
                if (streetsLoading) Text("CARGANDO...")
                if (streetsError != null) Text(streetsError)
                activeLine?.let { Button(onClick = { onMapRoute(it) }) { Text("VER RECORRIDO EN MAPA") } }
            }
            LinesLevel.INTERSECTIONS -> {
                Text(activeStreet?.name ?: "")
                LazyColumn { items(intersections) { item ->
                    Button(onClick = { onIntersectionClick(item) }, Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(item.name) }
                } }
                if (intersectionsLoading) Text("CARGANDO...")
                if (intersectionsError != null) Text(intersectionsError)
            }
            LinesLevel.STOPS -> {
                Text(activeIntersection?.name ?: "")
                LazyColumn { items(stops) { item ->
                    Button(onClick = { onStopClick(item) }, Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(item.description) }
                } }
                if (stopsLoading) Text("CARGANDO...")
                if (stopsError != null) Text(stopsError)
            }
            LinesLevel.ARRIVALS -> {
                Text(activeStop?.description ?: "")
                Button(onClick = onRefreshArrivals, enabled = !arrivalsLoading) { Text(if (arrivalsLoading) "CARGANDO..." else "ACTUALIZAR ARRIBOS") }
                Button(onClick = onSaveFavorite) { Text("GUARDAR FAVORITO") }
                if (arrivalsError != null) Text(arrivalsError)
                LazyColumn { items(arrivals) { item -> Text(item.toString(), Modifier.padding(8.dp)) } }
            }
        }
    }
}

@Composable
private fun MapHost(factory: () -> CyberMapView, mapKey: Any?) {
    androidx.compose.runtime.key(mapKey) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    val map = factory()
                    addView(map, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun <T> SimpleList(
    title: String, values: List<T>, empty: String, onClick: (T) -> Unit, label: (T) -> String
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (values.isEmpty()) Text(empty)
        else LazyColumn { items(values) { value ->
            Button(onClick = { onClick(value) }, Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(label(value)) }
        } }
    }
}
