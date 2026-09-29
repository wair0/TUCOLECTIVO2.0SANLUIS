package com.tucolectivo.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.ui.CyberMapView

private val ShellInk = Color(0xFF060912)

/**
 * Shell Compose unificado: NeonHeader + contenido de sección + NeonBottomBar.
 * El mapa se embebe con AndroidView sin modificar CyberMapView.
 */
@Composable
fun AppShellScreen(
    section: Int,
    onNavigate: (Int) -> Unit,
    syncing: Boolean,
    onSync: () -> Unit,
    // LÍNEAS
    lines: List<TransitLine>,
    linesLoading: Boolean,
    linesError: String?,
    onRefreshLines: () -> Unit,
    onLineClick: (TransitLine) -> Unit,
    // FAVORITOS
    favorites: List<FavoriteStopUi>,
    onFavoriteClick: (FavoriteStopUi) -> Unit,
    // PARADAS CERCANAS
    nearby: List<TransitStop>,
    nearbyLoading: Boolean,
    nearbyError: String?,
    onRefreshNearby: () -> Unit,
    onNearbyClick: (TransitStop) -> Unit,
    // MAPA (factory desde MainActivity para no tocar CyberMapView)
    mapFactory: () -> CyberMapView,
    mapKey: Any? = null
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ShellInk)
        ) {
            NeonHeader(
                menuItems = listOf("INICIO", "LÍNEAS", "MAPA", "FAVORITOS", "PARADAS CERCANAS"),
                onMenuItem = { index -> onNavigate(index) },
                onSearch = { }
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
                            NeonSyncButton(
                                syncing = syncing,
                                onClick = onSync
                            )
                        }
                    }
                    1 -> LinesComposeScreen(
                        lines = lines,
                        loading = linesLoading,
                        error = linesError,
                        onRefresh = onRefreshLines,
                        onLineClick = onLineClick
                    )
                    2 -> MapComposeHost(
                        factory = mapFactory,
                        mapKey = mapKey
                    )
                    3 -> FavoritesComposeScreen(
                        favorites = favorites,
                        onFavoriteClick = onFavoriteClick
                    )
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

            NeonBottomBar(
                selected = section.coerceIn(0, 4),
                onSelect = onNavigate
            )
        }
    }
}

@Composable
private fun MapComposeHost(
    factory: () -> CyberMapView,
    mapKey: Any?
) {
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

/** Modelo UI liviano para favoritos (evita acoplar data class privada de MainActivity). */
data class FavoriteStopUi(
    val lineCode: Int,
    val lineName: String,
    val stopCode: Int,
    val description: String,
    val street: String,
    val intersection: String
)
