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
import com.transpuntano.app.data.SmartMoveApi
import com.transpuntano.app.model.TransitLine
import com.transpuntano.app.model.TransitStop
import com.transpuntano.app.ui.CyberMapView
import com.transpuntano.app.ui.MapStop
import java.util.concurrent.Executors

/**
 * Shell Compose unificado (NeonHeader + secciones + NeonBottomBar).
 * El mapa se embebe con AndroidView sin modificar CyberMapView.
 * Lag de INICIO: sin GIF a 60fps debajo del Canvas Compose.
 */
class MainActivity : AppCompatActivity() {
    private val api = SmartMoveApi()
    private val executor = Executors.newFixedThreadPool(3)
    private lateinit var composeHomeView: ComposeView

    private var homeSyncing by mutableStateOf(false)
    private var composeSection by mutableStateOf(0)
    private var composeLines by mutableStateOf<List<TransitLine>>(emptyList())
    private var composeLinesLoading by mutableStateOf(false)
    private var composeLinesError by mutableStateOf<String?>(null)
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
                    lines = composeLines,
                    linesLoading = composeLinesLoading,
                    linesError = composeLinesError,
                    onRefreshLines = { refreshComposeLines() },
                    onLineClick = { line ->
                        mapLineFilter = line
                        mapHostKey += 1
                        navigateTo(2)
                    },
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
                if (composeSection != 0) navigateTo(0) else finish()
            }
        })
        navigateTo(0)
    }

    private fun navigateTo(index: Int) {
        composeSection = index
        when (index) {
            1 -> refreshComposeLines()
            2 -> { mapHostKey += 1 }
            3 -> { composeFavorites = loadFavorites() }
            4 -> refreshComposeNearby()
        }
    }

    private fun loadLines(navigateToLines: Boolean) {
        homeSyncing = true
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { lines ->
                    runOnUiThread {
                        homeSyncing = false
                        composeLines = lines
                        composeLinesLoading = false
                        composeLinesError = null
                        if (navigateToLines) navigateTo(1)
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        homeSyncing = false
                        composeLinesError = error.message ?: "Error"
                        toast(error.message ?: "Error")
                    }
                }
        }
    }

    private fun refreshComposeLines() {
        composeLinesLoading = true
        composeLinesError = null
        executor.execute {
            runCatching { api.getLines() }
                .onSuccess { items ->
                    runOnUiThread {
                        composeLines = items
                        composeLinesLoading = false
                        composeLinesError = null
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        composeLinesLoading = false
                        composeLinesError = error.message ?: "No se pudo consultar."
                    }
                }
        }
    }

    private fun refreshComposeNearby() {
        composeNearbyLoading = true
        composeNearbyError = null
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            composeNearbyLoading = false
            composeNearbyError = "PERMISOS DE UBICACIÓN REQUERIDOS"
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                42
            )
            return
        }
        val location = currentLocationOrNull()
        if (location == null) {
            composeNearbyLoading = false
            composeNearbyError = "UBICACIÓN NO DISPONIBLE"
            return
        }
        executor.execute {
            runCatching { api.getNearby(location.latitude, location.longitude) }
                .onSuccess { nearby ->
                    runOnUiThread {
                        composeNearby = nearby
                        lastMapNearbyStops = nearby
                        composeNearbyLoading = false
                        composeNearbyError = null
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        composeNearbyLoading = false
                        composeNearbyError = error.message ?: "Sin conexión"
                    }
                }
        }
    }

    private fun currentLocationOrNull(): android.location.Location? {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return null
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { p ->
            runCatching { manager.getLastKnownLocation(p) }.getOrNull()
        }.maxByOrNull { it.time }
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
                            val stops = nearby.map {
                                MapStop(
                                    id = it.code,
                                    title = it.description,
                                    subtitle = listOf(it.street, it.intersection)
                                        .filter { s -> s.isNotBlank() }
                                        .joinToString(" · "),
                                    latitude = it.latitude,
                                    longitude = it.longitude
                                )
                            }
                            runOnUiThread { map.setStops(stops, fit = false) }
                        }
                }
            }
        }
        map.setOnStopTap { stop -> toast(stop.title) }
        if (line != null) {
            executor.execute {
                runCatching { api.getRoute(line.code) }
                    .onSuccess { route -> runOnUiThread { map.setRoute(route) } }
                    .onFailure { error ->
                        runOnUiThread { toast(error.message ?: "No se pudo cargar el recorrido") }
                    }
            }
        }
        map.post {
            val loc = currentLocationOrNull()
            if (loc != null) {
                map.setUserLocation(loc.latitude, loc.longitude, center = true)
            }
        }
        return map
    }

    private fun loadFavorites(): List<FavoriteStopUi> {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        return prefs.all.mapNotNull { (_, v) ->
            val parts = (v as? String)?.split("|") ?: return@mapNotNull null
            if (parts.size < 7) return@mapNotNull null
            FavoriteStopUi(
                lineCode = parts[0].toIntOrNull() ?: 0,
                lineName = parts.getOrElse(1) { "" },
                stopCode = parts[2].toIntOrNull() ?: 0,
                description = parts.getOrElse(3) { "" },
                street = parts.getOrElse(5) { "" },
                intersection = parts.getOrElse(6) { "" }
            )
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
