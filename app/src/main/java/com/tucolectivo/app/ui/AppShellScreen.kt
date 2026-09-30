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
  Box(
   modifier = Modifier
    .weight(1f)
    .fillMaxWidth()
  ) {
   when (level) {
   LinesLevel.CATALOG -> {
    CyberLinesRefreshPanel(loading = linesLoading, error = linesError, onRefresh = onRefreshLines)
    LazyColumn(
     modifier = Modifier.fillMaxWidth(),
     verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
     contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp)
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
    CyberRouteListHeader(
     eyebrow = "07 // TRAYECTO ACTIVO",
     title = activeLine?.name ?: "LÍNEA ACTIVA",
     context = if (streetsLoading) "SINCRONIZANDO CALLES..." else if (streets.isEmpty()) "SIN CALLES DISPONIBLES" else "${streets.size} CALLES DETECTADAS",
     error = streetsError
    )
    LazyColumn(
     modifier = Modifier.fillMaxWidth(),
     verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
     contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
    ) {
     items(streets) { item ->
      CyberRouteNodeCard(
       index = streets.indexOf(item),
       eyebrow = "CALLE // ${streets.indexOf(item) + 1}",
       title = item.name,
       subtitle = "SEGMENTO DEL RECORRIDO // LÍNEA ${activeLine?.code ?: "--"}",
       accent = CyberColors.Primary,
       onClick = { onStreetClick(item) }
      )
     }
    }
   }
   LinesLevel.INTERSECTIONS -> {
    CyberRouteListHeader(
     eyebrow = "08 // NODOS DE RED",
     title = activeStreet?.name ?: "CALLE ACTIVA",
     context = if (intersectionsLoading) "SINCRONIZANDO INTERSECCIONES..." else if (intersections.isEmpty()) "SIN INTERSECCIONES DISPONIBLES" else "${intersections.size} NODOS DETECTADOS",
     error = intersectionsError
    )
    LazyColumn(
     modifier = Modifier.fillMaxWidth(),
     verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
     contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
    ) {
     items(intersections) { item ->
      CyberRouteNodeCard(
       index = intersections.indexOf(item),
       eyebrow = "NODO // ${intersections.indexOf(item) + 1}",
       title = item.name,
       subtitle = "INTERSECCIÓN ACTIVA // CONEXIÓN DE RED",
       accent = CyberColors.Secondary,
       onClick = { onIntersectionClick(item) }
      )
     }
    }
   }
   LinesLevel.STOPS -> {
    CyberRouteListHeader(
     eyebrow = "09 // PUNTOS DE ACCESO",
     title = activeIntersection?.name ?: "INTERSECCIÓN ACTIVA",
     context = if (stopsLoading) "SINCRONIZANDO PARADAS..." else if (stops.isEmpty()) "SIN PARADAS DISPONIBLES" else "${stops.size} PARADAS DETECTADAS",
     error = stopsError
    )
    LazyColumn(
     modifier = Modifier.fillMaxWidth(),
     verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
     contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
    ) {
     items(stops) { item ->
      CyberRouteNodeCard(
       index = stops.indexOf(item),
       eyebrow = "PARADA // ${stops.indexOf(item) + 1}",
       title = item.description,
       subtitle = "PUNTO DE ACCESO // TRANSPORTE URBANO",
       accent = CyberColors.Tertiary,
       onClick = { onStopClick(item) }
      )
     }
    }
   }
   LinesLevel.ARRIVALS -> {
    CyberArrivalsPanel(
     stopName = activeStop?.description ?: "PARADA ACTIVA",
     activeLineCode = activeLine?.code,
     arrivals = arrivals,
     loading = arrivalsLoading,
     error = arrivalsError,
     onRefresh = onRefreshArrivals,
     onSaveFavorite = onSaveFavorite,
     onMapRoute = { activeLine?.let { onMapRoute(it) } }
    )
   }
  }
  }
  CyberLinesStatusRail(level = level, activeLine = activeLine, activeStop = activeStop)
 }
}

@Composable
private fun CyberLinesStatusRail(
 level: LinesLevel,
 activeLine: TransitLine?,
 activeStop: TransitStop?
) {
 val pulse by CyberAnimation.pulse(min = 0.35f, max = 1f)
 val sweep by CyberAnimation.sweep(durationMillis = 2600)
 val label = when (level) {
  LinesLevel.CATALOG -> "CATÁLOGO // RED COMPLETA"
  LinesLevel.STREETS -> "TRAYECTO // CALLES"
  LinesLevel.INTERSECTIONS -> "TRAYECTO // NODOS"
  LinesLevel.STOPS -> "TRAYECTO // PARADAS"
  LinesLevel.ARRIVALS -> "TIEMPO REAL // ARRIBOS"
 }
 val context = when (level) {
  LinesLevel.CATALOG -> "LINK ONLINE"
  LinesLevel.STREETS -> "LÍNEA ${activeLine?.code ?: "--"}"
  LinesLevel.INTERSECTIONS -> "RUTA ACTIVA"
  LinesLevel.STOPS -> "NODO ACTIVO"
  LinesLevel.ARRIVALS -> "PARADA ${activeStop?.description?.uppercase() ?: "ACTIVA"}"
 }
 Box(
  modifier = Modifier
   .fillMaxWidth()
   .height(44.dp)
   .padding(horizontal = 12.dp, vertical = 4.dp)
 ) {
  Canvas(Modifier.fillMaxWidth().height(36.dp)) {
   drawRoundRect(
    color = CyberColors.SurfaceVariant,
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
   )
   drawRoundRect(
    color = CyberColors.Primary.copy(alpha = .38f + .22f * pulse),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
   )
   val x = 8.dp.toPx() + (size.width - 16.dp.toPx()) * sweep
   drawLine(
    color = CyberColors.Secondary.copy(alpha = .65f),
    start = androidx.compose.ui.geometry.Offset(x, 4.dp.toPx()),
    end = androidx.compose.ui.geometry.Offset(x, size.height - 4.dp.toPx()),
    strokeWidth = 1.5.dp.toPx()
   )
  }
  androidx.compose.foundation.layout.Row(
   Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
   verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
   horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
  ) {
   Text(label, color = CyberColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
   Text(context, color = CyberColors.Tertiary.copy(alpha = .85f), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
