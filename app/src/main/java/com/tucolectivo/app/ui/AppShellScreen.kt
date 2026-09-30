package com.tucolectivo.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.transpuntano.app.model.TransitArrival
import com.transpuntano.app.model.TransitIntersection
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.model.TransitStreet
import com.transpuntano.app.ui.CyberMapView

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
                Box(Modifier.weight(1f).fillMaxSize()) {
                    when (section) {
                        0 -> InicioBase(
                            syncing = syncing,
                            statusText = statusText,
                            onSync = onSync,
                            onNavigate = onNavigate,
                            onSearch = onSearch
                        )
                        1 -> LinesFlowScreen(
                            level = linesLevel,
                            lines = lines,
                            linesLoading = linesLoading,
                            linesError = linesError,
                            onRefreshLines = onRefreshLines,
                            onLineClick = onLineClick,
                            activeLine = activeLine,
                            streets = streets,
                            streetsLoading = streetsLoading,
                            streetsError = streetsError,
                            onStreetClick = onStreetClick,
                            onMapRoute = onMapRoute,
                            activeStreet = activeStreet,
                            intersections = intersections,
                            intersectionsLoading = intersectionsLoading,
                            intersectionsError = intersectionsError,
                            onIntersectionClick = onIntersectionClick,
                            activeIntersection = activeIntersection,
                            stops = stops,
                            stopsLoading = stopsLoading,
                            stopsError = stopsError,
                            onStopClick = onStopClick,
                            activeStop = activeStop,
                            arrivals = arrivals,
                            arrivalsLoading = arrivalsLoading,
                            arrivalsError = arrivalsError,
                            onRefreshArrivals = onRefreshArrivals,
                            onSaveFavorite = onSaveFavorite,
                            onBackLevel = onLinesBack
                        )
                        2 -> MapHost(mapFactory, mapKey)
                        3 -> FavoritesComposeScreen(favorites = favorites, onFavoriteClick = onFavoriteClick)
                        4 -> NearbyComposeScreen(
                            stops = nearby,
                            loading = nearbyLoading,
                            error = nearbyError,
                            onRefresh = onRefreshNearby,
                            onStopClick = onNearbyClick
                        )
                        else -> InicioBase(
                            syncing = syncing,
                            statusText = statusText,
                            onSync = onSync,
                            onNavigate = onNavigate,
                            onSearch = onSearch
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InicioBase(
    syncing: Boolean,
    statusText: String,
    onSync: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSearch: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        CyberHeader(
            statusText = statusText,
            onNavigate = onNavigate,
            onSearch = onSearch
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            Text("INICIO", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Text(
                if (syncing) "SINCRONIZANDO..." else "SISTEMA LISTO",
                color = CyberColors.Muted
            )
            androidx.compose.material3.Button(
                onClick = onSync,
                enabled = !syncing
            ) {
                Text(if (syncing) "SINCRONIZANDO..." else "SINCRONIZAR")
            }
            androidx.compose.material3.Button(onClick = { onNavigate(1) }) { Text("LÍNEAS") }
            androidx.compose.material3.Button(onClick = { onNavigate(2) }) { Text("MAPA") }
            androidx.compose.material3.Button(onClick = { onNavigate(3) }) { Text("FAVORITOS") }
            androidx.compose.material3.Button(onClick = { onNavigate(4) }) { Text("PARADAS CERCANAS") }
        }
    }
}

@Composable
private fun MapHost(factory: () -> CyberMapView, mapKey: Any?) {
    androidx.compose.runtime.key(mapKey) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val map = factory()
                    addView(
                        map,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
