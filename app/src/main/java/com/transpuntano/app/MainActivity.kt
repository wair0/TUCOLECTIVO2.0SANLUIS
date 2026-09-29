package com.tucolectivo.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.tucolectivo.app.ui.AppShellScreen
import com.tucolectivo.app.ui.FavoriteStopUi
import com.tucolectivo.app.ui.LinesLevel
import com.transpuntano.app.data.SmartMoveApi
import com.transpuntano.app.model.TransitArrival
import com.transpuntano.app.model.TransitIntersection
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.model.TransitStreet
import com.transpuntano.app.ui.CyberMapView
import com.transpuntano.app.ui.MapStop
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val api = SmartMoveApi()
    private val executor = Executors.newFixedThreadPool(3)
    private lateinit var composeHomeView: ComposeView

    private var homeSyncing by mutableStateOf(false)
    private var headerStatus by mutableStateOf("● SISTEMA LISTO")
    private var composeSection by mutableStateOf(0)
    private var composeLines by mutableStateOf<List<TransitLine>>(emptyList())
    private var composeLinesLoading by mutableStateOf(false)
    private var composeLinesError by mutableStateOf<String?>(null)

    private var linesLevel by mutableStateOf(LinesLevel.CATALOG)
    private var activeLine by mutableStateOf<TransitLine?>(null)
    private var activeStreet by mutableStateOf<TransitStreet?>(null)
    private var activeIntersection by mutableStateOf<TransitIntersection?>(null)
    private var activeStop by mutableStateOf<TransitStop?>(null)

    private var streets by mutableStateOf<List<TransitStreet>>(emptyList())
    private var streetsLoading by mutableStateOf(false)
    private var streetsError by mutableStateOf<String?>(null)
    private var intersections by mutableStateOf<List<TransitIntersection>>(emptyList())
    private var intersectionsLoading by mutableStateOf(false)
    private var intersectionsError by mutableStateOf<String?>(null)
    private var stops by mutableStateOf<List<TransitStop>>(emptyList())
    private var stopsLoading by mutableStateOf(false)
    private var stopsError by mutableStateOf<String?>(null)
    private var arrivals by mutableStateOf<List<TransitArrival>>(emptyList())
    private var arrivalsLoading by mutableStateOf(false)
    private var arrivalsError by mutableStateOf<String?>(null)

    private var composeNearby by mutableStateOf<List<TransitStop>>(emptyList())
    private var composeNearbyLoading by mutableStateOf(false)
    private var composeNearbyError by mutableStateOf<String?>(null)
    private var composeFavorites by mutableStateOf<List<FavoriteStopUi>>(emptyList())
    private var mapLineFilter by mutableStateOf<TransitLine?>(null)
    private var mapHostKey by mutableStateOf(0)
    private var lastMapNearbyStops = emptyList<TransitStop>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeHomeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppShellScreen(
                    section = composeSection,
                    onNavigate = { index -> navigateTo(index) },
                    syncing = homeSyncing,
                    onSync = { loadLines(false) },
                    statusText = headerStatus,
                    onSearch = { query -> performSearch(query) },
                    linesLevel = linesLevel,
                    lines = composeLines,
                    linesLoading = composeLinesLoading,
                    linesError = composeLinesError,
                    onRefreshLines = { refreshComposeLines() },
                    onLineClick = { line -> openLineStreets(line) },
                    activeLine = activeLine,
                    streets = streets,
                    streetsLoading = streetsLoading,
                    streetsError = streetsError,
                    onStreetClick = { street -> openIntersections(street) },
                    onMapRoute = { line ->
                        mapLineFilter = line
                        mapHostKey += 1
                        navigateTo(2)
                    },
                    activeStreet = activeStreet,
                    intersections = intersections,
                    intersectionsLoading = intersectionsLoading,
                    intersectionsError = intersectionsError,
                    onIntersectionClick = { intersection -> openStops(intersection) },
                    activeIntersection = activeIntersection,
                    stops = stops,
                    stopsLoading = stopsLoading,
                    stopsError = stopsError,
                    onStopClick = { stop -> openArrivals(stop) },
                    activeStop = activeStop,
                    arrivals = arrivals,
                    arrivalsLoading = arrivalsLoading,
                    arrivalsError = arrivalsError,
                    onRefreshArrivals = {
                        val s = activeStop; val l = activeLine
                        if (s != null && l != null) loadArrivals(s, l)
                    },
                    onSaveFavorite = {
                        val s = activeStop; val l = activeLine
                        if (s != null && l != null) { saveFavorite(s, l); toast("Parada guardada") }
                    },
                    onLinesBack = { linesGoBack() },
                    favorites = composeFavorites,
                    onFavoriteClick = { fav -> toast(fav.description) },
                    nearby = composeNearby,
                    nearbyLoading = composeNearbyLoading,
                    nearbyError = composeNearbyError,
                    onRefreshNearby = { refreshComposeNearby() },
                    onNearbyClick = { stop -> toast(stop.description) },
                    mapFactory = { createConfiguredMapView(mapLineFilter) },
                    mapKey = mapHostKey
                )
            }
        }
        setContentView(composeHomeView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    composeSection == 1 && linesLevel != LinesLevel.CATALOG -> linesGoBack()
                    composeSection != 0 -> navigateTo(0)
                    else -> finish()
                }
            }
        })
        navigateTo(0)
    }

    private fun navigateTo(index: Int) {
        composeSection = index
        when (index) {
            0 -> headerStatus = "● SISTEMA LISTO"
            1 -> { linesLevel = LinesLevel.CATALOG; refreshComposeLines() }
            2 -> { mapHostKey += 1; headerStatus = "● MAPA" }
            3 -> {
                composeFavorites = loadFavorites()
                headerStatus = "● ${composeFavorites.size} FAVORITOS"
            }
            4 -> refreshComposeNearby()
        }
    }

    private fun openLineStreets(line: TransitLine) {
        activeLine = line; activeStreet = null; activeIntersection = null; activeStop = null
        streets = emptyList(); streetsError = null; streetsLoading = true
        linesLevel = LinesLevel.STREETS
        headerStatus = "● LÍNEA ${line.code}"
        executor.execute {
            runCatching { api.getStreets(line.code) }
                .onSuccess { list -> runOnUiThread { streets = list; streetsLoading = false; streetsError = null } }
                .onFailure { e -> runOnUiThread { streetsLoading = false; streetsError = e.message ?: "Sin datos" } }
        }
    }

    private fun openIntersections(street: TransitStreet) {
        val line = activeLine ?: return
        activeStreet = street; activeIntersection = null; activeStop = null
        intersections = emptyList(); intersectionsError = null; intersectionsLoading = true
        linesLevel = LinesLevel.INTERSECTIONS
        headerStatus = "● INTERSECCIONES"
        executor.execute {
            runCatching { api.getIntersections(line.code, street.code) }
                .onSuccess { list -> runOnUiThread { intersections = list; intersectionsLoading = false; intersectionsError = null } }
                .onFailure { e -> runOnUiThread { intersectionsLoading = false; intersectionsError = e.message ?: "Sin datos" } }
        }
    }

    private fun openStops(intersection: TransitIntersection) {
        val line = activeLine ?: return
        val street = activeStreet ?: return
        activeIntersection = intersection; activeStop = null
        stops = emptyList(); stopsError = null; stopsLoading = true
        linesLevel = LinesLevel.STOPS
        headerStatus = "● PARADAS"
        executor.execute {
            runCatching { api.getStops(line.code, street.code, intersection.code) }
                .onSuccess { list -> runOnUiThread { stops = list; stopsLoading = false; stopsError = null } }
                .onFailure { e -> runOnUiThread { stopsLoading = false; stopsError = e.message ?: "Sin datos" } }
        }
    }

    private fun openArrivals(stop: TransitStop) {
        val line = activeLine ?: return
        activeStop = stop; arrivals = emptyList(); arrivalsError = null
        linesLevel = LinesLevel.ARRIVALS
        headerStatus = "● ARRIBOS"
        loadArrivals(stop, line)
    }

    private fun loadArrivals(stop: TransitStop, line: TransitLine) {
        arrivalsLoading = true; arrivalsError = null
        executor.execute {
            runCatching { api.getArrivals(stop.identifier, line.code) }
                .onSuccess { list -> runOnUiThread { arrivals = list; arrivalsLoading = false; arrivalsError = null } }
                .onFailure { e -> runOnUiThread { arrivalsLoading = false; arrivalsError = e.message ?: "Sin conexión" } }
        }
    }

    private fun linesGoBack() {
        when (linesLevel) {
            LinesLevel.ARRIVALS -> { linesLevel = LinesLevel.STOPS; arrivals = emptyList(); activeStop = null; headerStatus = "● PARADAS" }
            LinesLevel.STOPS -> { linesLevel = LinesLevel.INTERSECTIONS; stops = emptyList(); activeIntersection = null; headerStatus = "● INTERSECCIONES" }
            LinesLevel.INTERSECTIONS -> { linesLevel = LinesLevel.STREETS; intersections = emptyList(); activeStreet = null; headerStatus = "● LÍNEA ${activeLine?.code ?: ""}" }
            LinesLevel.STREETS -> { linesLevel = LinesLevel.CATALOG; streets = emptyList(); activeLine = null; headerStatus = "● LÍNEAS" }
            LinesLevel.CATALOG -> navigateTo(0)
        }
    }

    private fun saveFavorite(stop: TransitStop, line: TransitLine) {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        val key = "${line.code}_${stop.code}"
        val value = listOf(line.code, line.name, stop.code, stop.description, stop.identifier, stop.street, stop.intersection, stop.latitude, stop.longitude).joinToString("|")
        prefs.edit().putString(key, value).apply()
    }

    private fun loadLines(navigateToLines: Boolean) {
        homeSyncing = true
        headerStatus = "● SINCRONIZANDO..."
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { lines -> runOnUiThread {
                    homeSyncing = false; composeLines = lines; composeLinesLoading = false; composeLinesError = null
                    headerStatus = "● ${lines.size} LÍNEAS"
                    if (navigateToLines) navigateTo(1)
                }}
                .onFailure { error -> runOnUiThread {
                    homeSyncing = false; composeLinesError = error.message ?: "Error"
                    headerStatus = "● SIN CONEXIÓN"; toast(error.message ?: "Error")
                }}
        }
    }

    private fun refreshComposeLines() {
        composeLinesLoading = true; composeLinesError = null
        headerStatus = "● SINCRONIZANDO..."
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { items -> runOnUiThread {
                    composeLines = items; composeLinesLoading = false; composeLinesError = null
                    headerStatus = "● ${items.size} LÍNEAS"
                }}
                .onFailure { error -> runOnUiThread {
                    composeLinesLoading = false; composeLinesError = error.message ?: "No se pudo consultar."
                    headerStatus = "● SIN CONEXIÓN"
                }}
        }
    }

    private fun refreshComposeNearby() {
        composeNearbyLoading = true; composeNearbyError = null
        headerStatus = "● BUSCANDO PARADAS"
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            composeNearbyLoading = false; composeNearbyError = "PERMISOS DE UBICACIÓN REQUERIDOS"
            headerStatus = "● SIN PERMISO"
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
            return
        }
        val location = currentLocationOrNull()
        if (location == null) {
            composeNearbyLoading = false; composeNearbyError = "UBICACIÓN NO DISPONIBLE"
            headerStatus = "● SIN UBICACIÓN"
            return
        }
        executor.execute {
            runCatching { api.getNearby(location.latitude, location.longitude) }
                .onSuccess { nearby -> runOnUiThread {
                    composeNearby = nearby; lastMapNearbyStops = nearby
                    composeNearbyLoading = false; composeNearbyError = null
                    headerStatus = "● ${nearby.size} PARADAS CERCANAS"
                }}
                .onFailure { error -> runOnUiThread {
                    composeNearbyLoading = false; composeNearbyError = error.message ?: "Sin conexión"
                    headerStatus = "● ERROR"
                }}
        }
    }

    private fun performSearch(rawQuery: String) {
        val q = rawQuery.trim()
        if (q.isBlank()) { toast("Ingresá un número o nombre de línea"); return }
        headerStatus = "● BUSCANDO..."
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { lines ->
                    val normalized = q.lowercase()
                        .replace("línea", "").replace("linea", "").replace("line", "").replace("#", "")
                        .trim().replace(Regex("\\s+"), " ")
                    val matches = if (normalized.isBlank()) emptyList() else lines.filter { line ->
                        val name = line.name.lowercase()
                            .replace("línea", "").replace("linea", "").trim().replace(Regex("\\s+"), " ")
                        name == normalized || name.contains(normalized)
                    }
                    runOnUiThread {
                        composeLines = matches
                        composeLinesLoading = false
                        composeLinesError = null
                        linesLevel = LinesLevel.CATALOG
                        composeSection = 1
                        headerStatus = if (matches.isEmpty()) "● SIN RESULTADOS" else "● ${matches.size} RESULTADOS"
                    }
                }
                .onFailure { error -> runOnUiThread {
                    headerStatus = "● SIN CONEXIÓN"
                    toast(error.message ?: "Sin conexión")
                }}
        }
    }

    private fun currentLocationOrNull(): android.location.Location? {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> runCatching { manager.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun createConfiguredMapView(line: TransitLine?): CyberMapView {
        val map = CyberMapView(this)
        map.setOnLocationRequest {
            val loc = currentLocationOrNull()
            if (loc != null) {
                map.setUserLocation(loc.latitude, loc.longitude, center = true)
                executor.execute {
                    runCatching { api.getNearby(loc.latitude, loc.longitude) }
                        .onSuccess { nearby ->
                            lastMapNearbyStops = nearby
                            val mapStops = nearby.map {
                                MapStop(id = it.code, title = it.description,
                                    subtitle = listOf(it.street, it.intersection).filter { s -> s.isNotBlank() }.joinToString(" · "),
                                    latitude = it.latitude, longitude = it.longitude)
                            }
                            runOnUiThread { map.setStops(mapStops, fit = false) }
                        }
                }
            }
        }
        map.setOnStopTap { stop -> toast(stop.title) }
        if (line != null) {
            executor.execute {
                runCatching { api.getRoute(line.code) }
                    .onSuccess { route -> runOnUiThread { map.setRoute(route) } }
                    .onFailure { error -> runOnUiThread { toast(error.message ?: "No se pudo cargar el recorrido") } }
            }
        }
        map.post { currentLocationOrNull()?.let { map.setUserLocation(it.latitude, it.longitude, center = true) } }
        return map
    }

    private fun loadFavorites(): List<FavoriteStopUi> {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        return prefs.all.mapNotNull { (_, v) ->
            val parts = (v as? String)?.split("|") ?: return@mapNotNull null
            if (parts.size < 7) return@mapNotNull null
            FavoriteStopUi(lineCode = parts[0].toIntOrNull() ?: 0, lineName = parts.getOrElse(1) { "" },
                stopCode = parts[2].toIntOrNull() ?: 0, description = parts.getOrElse(3) { "" },
                street = parts.getOrElse(5) { "" }, intersection = parts.getOrElse(6) { "" })
        }
    }

    private fun toast(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
}
