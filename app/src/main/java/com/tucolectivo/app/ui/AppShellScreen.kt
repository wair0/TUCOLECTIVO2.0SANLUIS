package com.tucolectivo.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
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
    val gifActive = section != 2

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            GifBackground(modifier = Modifier.fillMaxSize(), active = gifActive)

            Column(Modifier.fillMaxSize()) {
                NeonHeader(
                    statusText = statusText,
                    menuItems = listOf("INICIO", "LÍNEAS", "MAPA", "FAVORITOS", "PARADAS CERCANAS"),
                    onMenuItem = { index -> onNavigate(index) },
                    onSearch = onSearch
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (section) {
                        0 -> {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f)) {
                                    NeonMenuScreen(
                                        onLineas = { onNavigate(1) },
                                        onMapa = { onNavigate(2) },
                                        onParadas = { onNavigate(4) },
                                        onFavoritos = { onNavigate(3) }
                                    )
                                }
                                NeonSyncButton(syncing = syncing, onClick = onSync)
                            }
                        }
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
                        2 -> MapComposeHost(factory = mapFactory, mapKey = mapKey)
                        3 -> FavoritesComposeScreen(favorites = favorites, onFavoriteClick = onFavoriteClick)
                        4 -> NearbyComposeScreen(
                            stops = nearby,
                            loading = nearbyLoading,
                            error = nearbyError,
                            onRefresh = onRefreshNearby,
                            onStopClick = onNearbyClick
                        )
                        else -> NeonMenuScreen(
                            onLineas = { onNavigate(1) },
                            onMapa = { onNavigate(2) },
                            onParadas = { onNavigate(4) },
                            onFavoritos = { onNavigate(3) }
                        )
                    }
                }

                NeonBottomBar(selected = section.coerceIn(0, 4), onSelect = onNavigate)
            }
        }
    }
}

@Composable
private fun MapComposeHost(factory: () -> CyberMapView, mapKey: Any?) {
    key(mapKey) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0xFF060912.toInt())
                    setPadding(12, 12, 12, 12)
                    val map = factory()
                    addView(
                        map,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    tag = map
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

data class FavoriteStopUi(
    val lineCode: Int,
    val lineName: String,
    val stopCode: Int,
    val description: String,
    val street: String,
    val intersection: String
)
