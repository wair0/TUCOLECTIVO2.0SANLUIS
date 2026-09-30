package com.tucolectivo.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    section: Int, onNavigate: (Int) -> Unit, syncing: Boolean, onSync: () -> Unit,
    statusText: String = "● SISTEMA LISTO", onSearch: (String) -> Unit = {},
    linesLevel: LinesLevel, lines: List<TransitLine>, linesLoading: Boolean, linesError: String?,
    onRefreshLines: () -> Unit, onLineClick: (TransitLine) -> Unit, activeLine: TransitLine?,
    streets: List<TransitStreet>, streetsLoading: Boolean, streetsError: String?,
    onStreetClick: (TransitStreet) -> Unit, onMapRoute: (TransitLine) -> Unit, activeStreet: TransitStreet?,
    intersections: List<TransitIntersection>, intersectionsLoading: Boolean, intersectionsError: String?,
    onIntersectionClick: (TransitIntersection) -> Unit, activeIntersection: TransitIntersection?,
    stops: List<TransitStop>, stopsLoading: Boolean, stopsError: String?,
    onStopClick: (TransitStop) -> Unit, activeStop: TransitStop?, arrivals: List<TransitArrival>,
    arrivalsLoading: Boolean, arrivalsError: String?, onRefreshArrivals: () -> Unit, onSaveFavorite: () -> Unit,
    onLinesBack: () -> Unit, favorites: List<FavoriteStopUi>, onFavoriteClick: (FavoriteStopUi) -> Unit,
    nearby: List<TransitStop>, nearbyLoading: Boolean, nearbyError: String?, onRefreshNearby: () -> Unit,
    onNearbyClick: (TransitStop) -> Unit, mapFactory: () -> CyberMapView, mapKey: Any? = null
) {
    CyberpunkTheme {
        Surface(Modifier.fillMaxSize(), color = CyberColors.Background) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxSize()) {
                    when (section) {
                        0 -> InicioBase(syncing, statusText, onSync, onNavigate, onSearch)
                        1 -> LinesBase(
                            linesLevel, lines, linesLoading, linesError, onRefreshLines, onLineClick, activeLine,
                            streets, streetsLoading, streetsError, onStreetClick, onMapRoute, activeStreet,
                            intersections, intersectionsLoading, intersectionsError, onIntersectionClick, activeIntersection,
                            stops, stopsLoading, stopsError, onStopClick, activeStop, arrivals, arrivalsLoading,
                            arrivalsError, onRefreshArrivals, onSaveFavorite, onLinesBack
                        )
                        2 -> MapHost(mapFactory, mapKey)
                        3 -> FavoritesComposeScreen(favorites = favorites, onFavoriteClick = onFavoriteClick)
                        4 -> NearbyComposeScreen(nearby, nearbyLoading, nearbyError, onRefreshNearby, onNearbyClick)
                        else -> InicioBase(syncing, statusText, onSync, onNavigate, onSearch)
                    }
                }
            }
        }
    }
}

@Composable
private fun InicioBase(
    syncing: Boolean, statusText: String, onSync: () -> Unit, onNavigate: (Int) -> Unit, onSearch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 24.dp
        )
    ) {
        item {
            CyberHeader(statusText = statusText, onNavigate = onNavigate, onSearch = onSearch)
        }

        item {
            RowDashboardStatus(statusText = statusText)
        }

        item {
            CyberSyncPanel(
                syncing = syncing,
                statusText = statusText,
                onSync = onSync,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        item {
            CyberLinesCard(
                onClick = { onNavigate(1) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            CyberMapCard(
                onClick = { onNavigate(2) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            CyberNearbyCard(
                onClick = { onNavigate(4) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            CyberFavoritesCard(
                onClick = { onNavigate(3) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            Text(
                "TRANSPUNTANO // SISTEMA DE MOVILIDAD URBANA",
                color = CyberColors.Muted,
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun RowDashboardStatus(statusText: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            "01–04 // NÚCLEO DE MOVILIDAD",
            color = CyberColors.Primary,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            statusText.uppercase(),
            color = CyberColors.Tertiary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun LinesBase(
 level: LinesLevel, lines: List<TransitLine>, linesLoading: Boolean, linesError: String?,
 onRefreshLines: () -> Unit, onLineClick: (TransitLine) -> Unit, activeLine: TransitLine?,
 streets: List<TransitStreet>, streetsLoading: Boolean, streetsError: String?, onStreetClick: (TransitStreet) -> Unit,
 onMapRoute: (TransitLine) -> Unit, activeStreet: TransitStreet?, intersections: List<TransitIntersection>,
 intersectionsLoading: Boolean, intersectionsError: String?, onIntersectionClick: (TransitIntersection) -> Unit,
 activeIntersection: TransitIntersection?, stops: List<TransitStop>, stopsLoading: Boolean, stopsError: String?,
 onStopClick: (TransitStop) -> Unit, activeStop: TransitStop?, arrivals: List<TransitArrival>,
 arrivalsLoading: Boolean, arrivalsError: String?, onRefreshArrivals: () -> Unit, onSaveFavorite: () -> Unit, onBack: () -> Unit
) {
 Column(Modifier.fillMaxSize()) {
  CyberLinesHeader(
   level = level,
   activeLine = activeLine,
   activeStreet = activeStreet,
   activeIntersection = activeIntersection,
   activeStop = activeStop,
   onBack = onBack
  )
  when (level) {
   LinesLevel.CATALOG -> {
    CyberLinesRefreshPanel(
     loading = linesLoading,
     error = linesError,
     onRefresh = onRefreshLines
    )
    LazyColumn(
     modifier = Modifier.fillMaxWidth(),
     verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
     contentPadding = androidx.compose.foundation.layout.PaddingValues(
      start = 12.dp,
      end = 12.dp,
      top = 10.dp,
      bottom = 24.dp
     )
    ) {
     items(lines) { line ->
      CyberLineCard(
       lineCode = line.code,
       lineName = line.name,
       index = lines.indexOf(line),
       onClick = { onLineClick(line) }
      )
     }
    }
   }
   LinesLevel.STREETS -> {
    Text(activeLine?.name ?: ""); if (streetsLoading) Text("CARGANDO..."); streetsError?.let { Text(it) }
    LazyColumn { items(streets) { item -> Button(onClick = { onStreetClick(item) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(item.name) } } }
    activeLine?.let { line -> Button(onClick = { onMapRoute(line) }) { Text("VER RECORRIDO EN MAPA") } }
   }
   LinesLevel.INTERSECTIONS -> {
    Text(activeStreet?.name ?: ""); if (intersectionsLoading) Text("CARGANDO..."); intersectionsError?.let { Text(it) }
    LazyColumn { items(intersections) { item -> Button(onClick = { onIntersectionClick(item) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(item.name) } } }
   }
   LinesLevel.STOPS -> {
    Text(activeIntersection?.name ?: ""); if (stopsLoading) Text("CARGANDO..."); stopsError?.let { Text(it) }
    LazyColumn { items(stops) { item -> Button(onClick = { onStopClick(item) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(item.description) } } }
   }
   LinesLevel.ARRIVALS -> {
    Text(activeStop?.description ?: "")
    Button(onClick = onRefreshArrivals, enabled = !arrivalsLoading) { Text(if (arrivalsLoading) "CARGANDO..." else "ACTUALIZAR ARRIBOS") }
    Button(onClick = onSaveFavorite) { Text("GUARDAR FAVORITO") }
    Button(onClick = { activeLine?.let { line -> onMapRoute(line) } }, enabled = activeLine != null) { Text("VER RECORRIDO EN MAPA") }
    arrivalsError?.let { Text(it) }
    LazyColumn { items(arrivals) { arrival -> NeonArrivalCard(
      lineLabel = if (arrival.line.isBlank()) "LÍNEA " + (activeLine?.code ?: "") else arrival.line,
      destination = arrival.destination.ifBlank { "DESTINO" }, minutes = arrival.minutes, modifier = Modifier.padding(vertical = 4.dp)
    ) } }
   }
  }
 }
}

@Composable
private fun MapHost(factory: () -> CyberMapView, mapKey: Any?) {
 androidx.compose.runtime.key(mapKey) {
  AndroidView(factory = { context ->
   FrameLayout(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    val map = factory()
    addView(map, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
   }
  }, modifier = androidx.compose.ui.Modifier.fillMaxSize())
 }
}
