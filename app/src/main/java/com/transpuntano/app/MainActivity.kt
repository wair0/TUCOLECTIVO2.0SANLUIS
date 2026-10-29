package com.tucolectivo.app

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.os.Looper
import android.os.SystemClock
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ValueCallback
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.tucolectivo.app.data.SmartMoveApi
import com.tucolectivo.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    @Volatile private var appStarted = false
    private val api = SmartMoveApi()
    private val executor = Executors.newFixedThreadPool(3)
    // El GPS de colectivos no comparte cola con líneas/paradas/arribos.
    // Esta separación replica la arquitectura que funcionaba en la rama nativa.
    private val vehicleExecutor = Executors.newSingleThreadExecutor()
    private val vehicleQueryExecutor = Executors.newFixedThreadPool(8)
    private val lineResolutionExecutor = Executors.newFixedThreadPool(8)
    private val resolvedStopLines = java.util.concurrent.ConcurrentHashMap<String, List<Int>>()
    private val routeCache = java.util.concurrent.ConcurrentHashMap<Int, List<Pair<Double, Double>>>()
    @Volatile private var transitLinesCache: List<TransitLine> = emptyList()
    @Volatile private var mapNearbyStops: List<TransitStop> = emptyList()
    @Volatile private var mapLineCode: Int = 0
    @Volatile private var mapDestinationFilter: String = ""
    @Volatile private var mapVehicleRefreshInProgress = false
    @Volatile private var pendingMapVehicleRefreshLine = 0
    @Volatile private var pendingMapVehicleRefreshManual = false
    private val anonymousMapVehicles = mutableMapOf<String, AnonymousMapVehicle>()
    private data class AnonymousMapVehicle(
        val id: String,
        var lineCode: Int,
        var destination: String,
        var lat: Double,
        var lng: Double,
        var lastSeenMs: Long
    )
    private var pendingArrivalNotifications: String? = null
    private var pendingArrivalStartTime: String = "18:00"
    private var pendingArrivalEndTime: String = "19:30"
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingFileChooser: ValueCallback<Array<Uri>>? = null
    @Volatile private var pendingLocationPurpose: String? = null
    @Volatile private var continuousLocationActive = false
    private var continuousLocationManager: LocationManager? = null
    private val continuousLocationListeners = mutableListOf<LocationListener>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        enterSplashFullscreen()
        showSplash()
    }

    private fun enterSplashFullscreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun exitSplashFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startApp() {
        if (appStarted) return
        exitSplashFullscreen()
        setContentView(webView)
        appStarted = true
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.textZoom = 100
        webView.settings.setSupportZoom(false)
        webView.settings.builtInZoomControls = false
        webView.settings.displayZoomControls = false
        webView.settings.setGeolocationEnabled(true)
        webView.addJavascriptInterface(TransitBridge(), "TuColectivoNative")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                installSearchBridge(view)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                pendingFileChooser?.onReceiveValue(null)
                pendingFileChooser = filePathCallback
                return try {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                    startActivityForResult(Intent.createChooser(intent, "SELECCIONAR FOTO DE PERFIL"), PROFILE_PHOTO_REQUEST)
                    true
                } catch (error: Exception) {
                    pendingFileChooser?.onReceiveValue(null)
                    pendingFileChooser = null
                    Log.e("TuColectivoProfile", "No se pudo abrir el selector de fotos", error)
                    false
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_REQUEST
                    )
                }
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startContinuousLocationTracking()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.TuColectivo && window.TuColectivo.handleBack ? window.TuColectivo.handleBack() : false") { result ->
                    if (result == "false") finish()
                }
            }
        })
    }

    private fun showSplash() {
        setContentView(GifSplashView(this) {
            if (!isFinishing && !isDestroyed) startApp()
        })
    }

    private class GifSplashView(
        context: Context,
        private val onFinished: () -> Unit
    ) : View(context) {

        private val movie: Movie
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // Clock starts on the first painted frame, not at construction.
        // Decoding splash.gif (~15 MB) happens before the first draw; starting
        // the clock in the constructor made the opening frame land ~25-30% in.
        private var playbackStart = 0L
        private var finished = false

        init {
            context.resources.openRawResource(R.drawable.splash).use { input ->
                movie = requireNotNull(Movie.decodeStream(input)) {
                    "No se pudo decodificar res/drawable-nodpi/splash.gif"
                }
            }
            // Park the decoder on frame 0 before anything is shown.
            movie.setTime(0)
            // Movie only seeks and paints the requested frame on a software layer.
            setLayerType(View.LAYER_TYPE_SOFTWARE, paint)
            setBackgroundColor(0xFF000000.toInt())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val movieWidth = movie.width()
            val movieHeight = movie.height()

            if (movieWidth <= 0 || movieHeight <= 0 || width <= 0 || height <= 0) {
                postInvalidateOnAnimation()
                return
            }

            // Fit-center: keep the original 450x800 GIF proportional and
            // fully inside the display bounds. The splash must never overflow
            // or be clipped by the screen edges.
            val scale = minOf(
                width.toFloat() / movieWidth.toFloat(),
                height.toFloat() / movieHeight.toFloat()
            )

            val drawWidth = movieWidth * scale
            val drawHeight = movieHeight * scale
            val left = (width - drawWidth) / 2f
            val top = (height - drawHeight) / 2f

            canvas.save()
            canvas.translate(left, top)
            canvas.scale(scale, scale)

            if (playbackStart == 0L) {
                playbackStart = SystemClock.uptimeMillis()
                movie.setTime(0)
            }
            val elapsed = SystemClock.uptimeMillis() - playbackStart
            val duration = movie.duration().toLong().takeIf { it > 0L } ?: 1L
            val frameTime = elapsed.coerceIn(0L, duration).toInt()

            movie.setTime(frameTime)
            movie.draw(canvas, 0f, 0f, paint)

            canvas.restore()

            if (elapsed >= duration && !finished) {
                finished = true
                post { onFinished() }
                return
            }

            postInvalidateOnAnimation()
        }
    }

    private fun installSearchBridge(view: WebView) {
        view.evaluateJavascript(
            """
            (() => {
              if (window.__tuColectivoSearchBridgeInstalled) return;
              window.__tuColectivoSearchBridgeInstalled = true;
              const normalize = value => String(value ?? '')
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .trim()
                .toUpperCase();

              const render = (query) => {
                const q = normalize(query);
                const panel = document.getElementById('m-search');
                if (!panel) return;
                let box = document.getElementById('nativeSearchResults');
                if (!box) {
                  box = document.createElement('div');
                  box.id = 'nativeSearchResults';
                  box.style.cssText = 'display:flex;flex-direction:column;gap:8px;margin-top:10px;';
                  panel.appendChild(box);
                }
                box.innerHTML = '';

                const cards = Array.from(document.querySelectorAll('.line-card'));
                const matches = cards.filter(card => {
                  const code = normalize(card.dataset.line);
                  const label = normalize(
                    card.querySelector('b')?.textContent ||
                    card.textContent
                  );
                  if (!q) return false;
                  return code === q || label === q || label.includes('LINEA ' + q);
                });

                if (!matches.length) {
                  box.innerHTML = '<div style="padding:10px;opacity:.8">BUSCANDO EN LÍNEAS...</div>';
                  if (window.TuColectivoNative?.loadLines) {
                    try { window.TuColectivoNative.loadLines(); } catch (_) {}
                  }
                  let tries = 0;
                  const retry = () => {
                    if (!box.isConnected || !q || tries++ > 12) return;
                    const fresh = Array.from(document.querySelectorAll('.line-card')).filter(card => {
                      const code = normalize(card.dataset.line);
                      const label = normalize(card.querySelector('b')?.textContent || card.textContent);
                      return code === q || label === q || label.includes('LINEA ' + q);
                    });
                    if (fresh.length) {
                      box.innerHTML = '';
                      fresh.forEach(card => {
                        const result = document.createElement('button');
                        result.type = 'button';
                        result.textContent = card.querySelector('b')?.textContent?.trim() || ('LINEA ' + card.dataset.line);
                        result.style.cssText = 'min-height:42px;text-align:left;padding:10px 12px;background:rgba(0,240,255,.08);border:1px solid rgba(0,240,255,.45);color:inherit;border-radius:8px;font:inherit;font-weight:800;';
                        result.addEventListener('click', () => {
                          document.querySelector('[data-menu="m-search"]')?.click();
                          card.scrollIntoView({behavior:'smooth',block:'center'});
                          card.click();
                        });
                        box.appendChild(result);
                      });
                      return;
                    }
                    setTimeout(retry, 180);
                  };
                  setTimeout(retry, 180);
                  return;
                }

                matches.forEach(card => {
                  const result = document.createElement('button');
                  result.type = 'button';
                  result.textContent = card.querySelector('b')?.textContent?.trim() || ('LINEA ' + card.dataset.line);
                  result.style.cssText = 'min-height:42px;text-align:left;padding:10px 12px;background:rgba(0,240,255,.08);border:1px solid rgba(0,240,255,.45);color:inherit;border-radius:8px;font:inherit;font-weight:800;';
                  result.addEventListener('click', () => {
                    document.querySelector('[data-menu="m-search"]')?.click();
                    card.scrollIntoView({behavior:'smooth',block:'center'});
                    card.click();
                  });
                  box.appendChild(result);
                });
              };

              document.addEventListener('app:search', event => render(event.detail?.q || ''));
            })();
            """.trimIndent(),
            null
        )
    }

    private fun dispatch(functionName: String, payload: String) {
        val safe = JSONObject.quote(payload)
        runOnUiThread { webView.evaluateJavascript("window.$functionName && window.$functionName($safe)", null) }
    }

    private fun lineJson(items: List<TransitLine>) = JSONArray().apply {
        items.forEach {
            put(
                JSONObject()
                    .put("code", it.code)
                    .put("name", publicLineLabel(it.code, items))
                    .put("raw", it.raw)
            )
        }
    }.toString()

    private fun streetJson(items: List<TransitStreet>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("code", it.code).put("name", it.name)) }
    }.toString()

    private fun intersectionJson(items: List<TransitIntersection>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("code", it.code).put("name", it.name)) }
    }.toString()

    private fun publicLineLabel(code: Int, catalog: List<TransitLine> = transitLinesCache): String {
        val raw = catalog.firstOrNull { it.code == code }?.name.orEmpty().trim()
        return raw
            .replace(Regex("(?i)^l[ií]nea\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { code.toString() }
    }

    private fun stopJson(items: List<TransitStop>, catalog: List<TransitLine> = transitLinesCache) = JSONArray().apply {
        items.forEach { stop ->
            val lineCodes = JSONArray().apply { stop.lineCodes.forEach { put(it) } }
            val lineLabels = JSONObject().apply {
                stop.lineCodes.filter { it > 0 }.distinct().forEach { code ->
                    put(code.toString(), publicLineLabel(code, catalog))
                }
            }
            put(
                JSONObject()
                    .put("code", stop.code)
                    .put("description", stop.description)
                    .put("identifier", stop.identifier)
                    .put("latitude", stop.latitude)
                    .put("longitude", stop.longitude)
                    .put("street", stop.street)
                    .put("intersection", stop.intersection)
                    .put("lineCode", stop.lineCode)
                    .put("lineCodes", lineCodes)
                    .put("lineLabels", lineLabels)
            )
        }
    }.toString()

    private fun lineLabelsJson(codes: List<Int>, catalog: List<TransitLine> = transitLinesCache) = JSONObject().apply {
        codes.filter { it > 0 }.distinct().forEach { code -> put(code.toString(), publicLineLabel(code, catalog)) }
    }.toString()

    private fun normalizePublicLineLabel(raw: String, code: Int): String {
        val cleaned = raw.trim().replace(Regex("(?i)^l[ií]nea\\s*"), "").trim()
        if (cleaned.isBlank()) return if (code > 0) publicLineLabel(code) else "SERVICIO"
        // Algunos registros de SmartMove traen "N" como etiqueta vacía de línea.
        // En ese caso usamos el código solicitado y su nombre público del catálogo.
        if (
            code > 0 && (
                cleaned.matches(Regex("\\d+")) ||
                cleaned.matches(Regex("(?i)(?:N|N/A|NA|S/D|SD|SIN DATO|DESCONOCIDA)"))
            )
        ) return publicLineLabel(code)
        return cleaned
    }

    private fun arrivalJson(items: List<TransitArrival>, fallbackLineCode: Int = 0) = JSONArray().apply {
        items.forEach { arrival ->
            put(
                JSONObject()
                    .put("line", normalizePublicLineLabel(arrival.line, fallbackLineCode))
                    .put("destination", arrival.destination)
                    .put("minutes", arrival.minutes)
                    .put("status", arrival.status)
                    .put("vehicleId", arrival.vehicleId)
                    .put("latitude", arrival.latitude)
                    .put("longitude", arrival.longitude)
                    .put("gpsTimestamp", arrival.gpsTimestamp)
            )
        }
    }.toString()

    private fun routeJson(points: List<Pair<Double, Double>>) = JSONArray().apply {
        points.forEach { point ->
            put(JSONObject().put("lat", point.first).put("lng", point.second))
        }
    }.toString()

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2).let { it * it }
        return earth * 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
    }

    private fun refreshMapVehiclesNow(lineCode: Int = mapLineCode, manualRequest: Boolean = false) {
        val destinationFilter = mapDestinationFilter
        if (mapNearbyStops.isEmpty()) {
            if (manualRequest) {
                dispatch("onNativeMapManualRefreshError", JSONObject()
                    .put("message", "NO HAY PARADAS DISPONIBLES PARA ACTUALIZAR EL GPS")
                    .toString())
            }
            return
        }
        if (mapVehicleRefreshInProgress) {
            if (lineCode > 0) {
                pendingMapVehicleRefreshLine = lineCode
                pendingMapVehicleRefreshManual = pendingMapVehicleRefreshManual || manualRequest
            }
            return
        }
        mapVehicleRefreshInProgress = true

        vehicleExecutor.execute {
            try {
                val found = LinkedHashMap<String, JSONObject>()

                // La rama nativa que funcionaba consultaba las paradas en paralelo.
                // Una consulta lenta no puede bloquear toda la actualización GPS.
                val tasks = mapNearbyStops.take(8).map { stop ->
                    Callable {
                        runCatching {
                            val identifier = stop.identifier.ifBlank { stop.code.toString() }
                            if (lineCode > 0) {
                                runCatching {
                                    api.getArrivals(identifier, lineCode, timeoutMs = 4_000)
                                }.getOrElse {
                                    if (identifier != stop.code.toString()) {
                                        api.getArrivals(stop.code.toString(), lineCode, timeoutMs = 8_000)
                                    } else {
                                        emptyList()
                                    }
                                }
                            } else {
                                stop.lineCodes.filter { it > 0 }.distinct().flatMap { code ->
                                    runCatching {
                                        api.getArrivals(identifier, code, timeoutMs = 8_000)
                                    }.getOrElse { emptyList() }
                                }
                            }
                        }.getOrDefault(emptyList())
                    }
                }

                val usedAnonymousIds = mutableSetOf<String>()
                val nowMs = System.currentTimeMillis()
                anonymousMapVehicles.entries.removeIf { nowMs - it.value.lastSeenMs > 30_000L }

                fun stableVehicleId(code: Int, destination: String, lat: Double, lng: Double, vehicleId: String): String {
                    if (vehicleId.isNotBlank()) return vehicleId
                    val normalizedDestination = destination.trim().lowercase()
                    val duplicate = found.values.firstOrNull { existing ->
                        existing.optInt("lineCode", code) == code &&
                            existing.optString("destination").trim().lowercase() == normalizedDestination &&
                            distanceMeters(existing.optDouble("lat"), existing.optDouble("lng"), lat, lng) < 300.0
                    }
                    if (duplicate != null) return "__duplicate__"

                    val candidate = anonymousMapVehicles.values
                        .filter { it.id !in usedAnonymousIds && it.lineCode == code && it.destination == normalizedDestination }
                        .minByOrNull { distanceMeters(it.lat, it.lng, lat, lng) }
                    val id = if (candidate != null && distanceMeters(candidate.lat, candidate.lng, lat, lng) <= 1500.0) {
                        candidate.id
                    } else {
                        "anon-" + code + "-" + (anonymousMapVehicles.size + usedAnonymousIds.size + 1)
                    }
                    anonymousMapVehicles[id] = AnonymousMapVehicle(id, code, normalizedDestination, lat, lng, nowMs)
                    usedAnonymousIds += id
                    return id
                }

                runCatching {
                    vehicleQueryExecutor.invokeAll(
                        tasks,
                        4_500,
                        TimeUnit.MILLISECONDS
                    )
                }.getOrNull().orEmpty().forEach { future ->
                    runCatching { future.get() }.getOrNull().orEmpty().forEach { arrival ->
                        val lat = arrival.latitude ?: return@forEach
                        val lng = arrival.longitude ?: return@forEach
                        if (lat == 0.0 || lng == 0.0) return@forEach
                        if (destinationFilter.isNotBlank()) {
                            val actual = arrival.destination.trim().lowercase()
                            val wanted = destinationFilter.trim().lowercase()
                            if (actual != wanted && !actual.contains(wanted) && !wanted.contains(actual)) return@forEach
                        }

                        val code = lineCode
                        val id = stableVehicleId(code, arrival.destination, lat, lng, arrival.vehicleId)
                        if (id == "__duplicate__") return@forEach

                        found[id] = JSONObject()
                            .put("id", id)
                            .put("lineCode", code)
                            .put("line", normalizePublicLineLabel(arrival.line, code))
                            .put("destination", arrival.destination)
                            .put("lat", lat)
                            .put("lng", lng)
                            .put("gpsTimestamp", arrival.gpsTimestamp)
                    }
                }

                dispatch(
                    "onNativeMapVehicles",
                    JSONArray().apply {
                        found.values.forEach { put(it) }
                    }.toString()
                )
                if (manualRequest) {
                    dispatch(
                        "onNativeMapManualRefreshComplete",
                        JSONObject()
                            .put("lineCode", lineCode)
                            .put("vehicleCount", found.size)
                            .toString()
                    )
                }
            } finally {
                mapVehicleRefreshInProgress = false
                val pendingLine = pendingMapVehicleRefreshLine
                val pendingManual = pendingMapVehicleRefreshManual
                pendingMapVehicleRefreshLine = 0
                pendingMapVehicleRefreshManual = false
                if (pendingLine > 0 && pendingLine == mapLineCode && mapNearbyStops.isNotEmpty()) {
                    refreshMapVehiclesNow(pendingLine, pendingManual)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startContinuousLocationTracking() {
        if (continuousLocationActive) return
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return
        continuousLocationManager = manager
        continuousLocationListeners.clear()
        providers.forEach { provider ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    dispatch(
                        "onNativeContinuousLocation",
                        JSONObject()
                            .put("latitude", location.latitude)
                            .put("longitude", location.longitude)
                            .put("accuracy", location.accuracy)
                            .put("provider", location.provider ?: provider)
                            .toString()
                    )
                }
            }
            runCatching {
                // Mantener una solicitud activa mientras la Activity está visible hace
                // que Android mantenga el indicador de ubicación y entregue nuevas posiciones.
                manager.requestLocationUpdates(provider, 5_000L, 5f, listener, Looper.getMainLooper())
                continuousLocationListeners += listener
            }
        }
        continuousLocationActive = continuousLocationListeners.isNotEmpty()
    }

    private fun stopContinuousLocationTracking() {
        val manager = continuousLocationManager ?: return
        continuousLocationListeners.forEach { listener -> runCatching { manager.removeUpdates(listener) } }
        continuousLocationListeners.clear()
        continuousLocationManager = null
        continuousLocationActive = false
    }

    @SuppressLint("MissingPermission")
    private fun requestNativeLocation(purpose: String) {
        pendingLocationPurpose = purpose
        startContinuousLocationTracking()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
            return
        }
        try {
            val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            if (providers.isEmpty()) { failNativeLocation(purpose, "UBICACIÓN DESACTIVADA"); return }
            var delivered = false
            val listeners = mutableListOf<LocationListener>()
            val finish: (Location) -> Unit = { location ->
                if (!delivered) {
                    delivered = true
                    listeners.forEach { listener -> runCatching { manager.removeUpdates(listener) } }
                    listeners.clear()
                    deliverNativeLocation(purpose, location)
                }
            }
            providers.forEach { provider ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) { finish(location) }
                }
                listeners += listener
                runCatching { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }
                    .onFailure { listeners.remove(listener) }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                if (!delivered && pendingLocationPurpose == purpose) {
                    listeners.forEach { listener -> runCatching { manager.removeUpdates(listener) } }
                    listeners.clear()
                    failNativeLocation(purpose, "TIEMPO DE ESPERA AGOTADO")
                }
            }, 12000L)
        } catch (e: Exception) { failNativeLocation(purpose, e.message ?: "ERROR DE UBICACIÓN") }
    }
    private fun deliverNativeLocation(purpose: String, location: Location) {
        if (pendingLocationPurpose != purpose) return
        pendingLocationPurpose = null
        dispatch("onNativeLocation", JSONObject().put("purpose", purpose).put("latitude", location.latitude).put("longitude", location.longitude).toString())
        if (purpose == "map") {
            // MAPA: sincronización/centrado solamente. La carga de paradas se solicita aparte.
        } else executor.execute {
            runCatching { api.getNearby(location.latitude, location.longitude) }
                .onSuccess { stops ->
                    dispatch("onNativeNearbyStops", JSONObject()
                        .put("latitude", location.latitude)
                        .put("longitude", location.longitude)
                        .put("stops", JSONArray(stopJson(stops)))
                        .toString())
                }
                .onFailure { dispatch("onNativeNearbyStopsError", JSONObject().put("message", it.message ?: "No se pudieron localizar las paradas").toString()) }
        }
    }
    private fun failNativeLocation(purpose: String, message: String) {
        pendingLocationPurpose = null
        dispatch("onNativeLocationError", JSONObject().put("purpose", purpose).put("message", message).toString())
    }

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private fun beginGoogleSignIn() {
        val clientId = getString(R.string.default_web_client_id)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId).requestEmail().build()
        val client = GoogleSignIn.getClient(this, options)
        client.signOut().addOnCompleteListener(this) {
            startActivityForResult(client.signInIntent, GOOGLE_SIGN_IN_REQUEST)
        }
    }

    @Deprecated("Google Sign-In activity result compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_PHOTO_REQUEST) {
            val callback = pendingFileChooser
            pendingFileChooser = null
            callback?.onReceiveValue(if (resultCode == RESULT_OK) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null)
            return
        }
        if (requestCode != GOOGLE_SIGN_IN_REQUEST) return
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val token = account.idToken ?: throw IllegalStateException("NO SE RECIBIÓ TOKEN DE GOOGLE")
            firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                .addOnSuccessListener(this) { result ->
                    val user = result.user
                    if (user == null) {
                        dispatch("onNativeGoogleAuthError", JSONObject().put("message", "NO SE PUDO VALIDAR LA CUENTA").toString())
                    } else {
                        val payload = JSONObject().put("uid", user.uid)
                            .put("email", user.email ?: "").put("name", user.displayName ?: "")
                        dispatch("onNativeGoogleAuthSuccess", payload.toString())
                    }
                }.addOnFailureListener(this) {
                    dispatch("onNativeGoogleAuthError", JSONObject().put("message", "NO SE PUDO AUTENTICAR CON GOOGLE").toString())
                }
        } catch (error: Exception) {
            val status = (error as? ApiException)?.statusCode
            Log.e("TuColectivoAuth", "Google Sign-In falló. Status: $status", error)
            val detail = if (status != null) " (CÓDIGO GOOGLE: $status)" else ""
            dispatch("onNativeGoogleAuthError", JSONObject().put("message", "NO SE PUDO COMPLETAR EL ACCESO CON GOOGLE$detail. REVISÁ LA FIRMA SHA-1 REGISTRADA EN FIREBASE.").toString())
        }
    }

    private fun emailAuthError(error: Exception): String {
        val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
        return when (code) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "ESTE CORREO YA TIENE UNA CUENTA."
            "ERROR_INVALID_EMAIL" -> "EL CORREO ELECTRÓNICO NO ES VÁLIDO."
            "ERROR_WEAK_PASSWORD" -> "LA CONTRASEÑA ES DEMASIADO DÉBIL (MÍNIMO 6 CARACTERES)."
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "CORREO O CONTRASEÑA INCORRECTOS."
            "ERROR_NETWORK_REQUEST_FAILED" -> "SIN CONEXIÓN. REVISÁ INTERNET E INTENTÁ NUEVAMENTE."
            "ERROR_OPERATION_NOT_ALLOWED" -> "HABILITÁ CORREO/CONTRASEÑA EN FIREBASE AUTHENTICATION."
            else -> "NO SE PUDO COMPLETAR LA OPERACIÓN. INTENTÁ NUEVAMENTE."
        }
    }

    private fun emailAuthResult(user: com.google.firebase.auth.FirebaseUser?) {
        if (user == null) {
            dispatch("onNativeEmailAuthError", JSONObject().put("message", "NO SE PUDO CONFIRMAR LA SESIÓN.").toString())
            return
        }
        dispatch("onNativeEmailAuthSuccess", JSONObject().put("uid", user.uid)
            .put("email", user.email ?: "").put("name", user.displayName ?: "")
            .put("verified", user.isEmailVerified).toString())
    }

    inner class TransitBridge {
        @JavascriptInterface fun signOutAccount() {
            runOnUiThread { firebaseAuth.signOut(); GoogleSignIn.getClient(this@MainActivity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut() }
        }

        @JavascriptInterface fun getCurrentAccount(): String {
            val user = FirebaseAuth.getInstance().currentUser ?: return "{}"
            return JSONObject().put("uid", user.uid)
                .put("email", user.email ?: "").put("name", user.displayName ?: "")
                .put("verified", user.isEmailVerified).toString()
        }

        @JavascriptInterface fun emailAuth(mode: String, email: String, password: String) {
            runOnUiThread {
                val auth = FirebaseAuth.getInstance()
                val cleanEmail = email.trim()
                if (cleanEmail.isBlank() || (mode != "recover" && password.length < 6)) {
                    dispatch("onNativeEmailAuthError", JSONObject().put("message", "INGRESÁ UN CORREO VÁLIDO Y UNA CONTRASEÑA DE AL MENOS 6 CARACTERES.").toString())
                    return@runOnUiThread
                }
                when (mode) {
                    "login" -> auth.signInWithEmailAndPassword(cleanEmail, password)
                        .addOnSuccessListener(this@MainActivity) { emailAuthResult(it.user) }
                        .addOnFailureListener(this@MainActivity) { dispatch("onNativeEmailAuthError", JSONObject().put("message", emailAuthError(it)).toString()) }
                    "register" -> auth.createUserWithEmailAndPassword(cleanEmail, password)
                        .addOnSuccessListener(this@MainActivity) {
                            val user = it.user
                            user?.sendEmailVerification()?.addOnFailureListener { error -> Log.w("TuColectivoAuth", "No se pudo enviar verificación", error) }
                            emailAuthResult(user)
                        }
                        .addOnFailureListener(this@MainActivity) { dispatch("onNativeEmailAuthError", JSONObject().put("message", emailAuthError(it)).toString()) }
                    "recover" -> auth.sendPasswordResetEmail(cleanEmail)
                        .addOnSuccessListener(this@MainActivity) { dispatch("onNativeEmailAuthRecovery", JSONObject().put("message", "SI EL CORREO ESTÁ REGISTRADO, RECIBIRÁS INSTRUCCIONES PARA RECUPERAR TU CONTRASEÑA.").toString()) }
                        .addOnFailureListener(this@MainActivity) { dispatch("onNativeEmailAuthError", JSONObject().put("message", emailAuthError(it)).toString()) }
                    else -> dispatch("onNativeEmailAuthError", JSONObject().put("message", "OPERACIÓN NO VÁLIDA.").toString())
                }
            }
        }
        @JavascriptInterface fun registerAccount(email: String, password: String, first: String, last: String) {
            runOnUiThread {
                val cleanEmail = email.trim()
                val cleanFirst = first.trim()
                val cleanLast = last.trim()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ||
                    password.length < 6 || cleanFirst.isBlank() || cleanLast.isBlank() ||
                    cleanFirst.length > 60 || cleanLast.length > 60) {
                    dispatch("onNativeEmailAuthError", JSONObject().put("message", "REVISÁ LOS DATOS DEL FORMULARIO.").toString())
                    return@runOnUiThread
                }
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener(this@MainActivity) { result ->
                        val user = result.user
                        if (user == null) {
                            dispatch("onNativeEmailAuthError", JSONObject().put("message", "NO SE PUDO CREAR LA CUENTA.").toString())
                            return@addOnSuccessListener
                        }
                        val name = "$cleanFirst $cleanLast"
                        val change = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        user.updateProfile(change).addOnSuccessListener(this@MainActivity) {
                            user.sendEmailVerification().addOnSuccessListener(this@MainActivity) {
                                emailAuthResult(user)
                            }.addOnFailureListener(this@MainActivity) {
                                dispatch("onNativeEmailAuthError", JSONObject().put("message", "CUENTA CREADA, PERO NO SE PUDO ENVIAR LA VERIFICACIÓN. INTENTÁ REENVIARLA.").toString())
                                emailAuthResult(user)
                            }
                        }.addOnFailureListener(this@MainActivity) {
                            dispatch("onNativeEmailAuthError", JSONObject().put("message", "CUENTA CREADA, PERO NO SE PUDO GUARDAR EL NOMBRE.").toString())
                        }
                    }.addOnFailureListener(this@MainActivity) {
                        dispatch("onNativeEmailAuthError", JSONObject().put("message", emailAuthError(it)).toString())
                    }
            }
        }

        @JavascriptInterface fun resendEmailVerification() {
            runOnUiThread {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    dispatch("onNativeEmailAuthError", JSONObject().put("message", "NO HAY UNA SESIÓN PENDIENTE.").toString())
                    return@runOnUiThread
                }
                user.sendEmailVerification().addOnSuccessListener(this@MainActivity) {
                    dispatch("onNativeEmailVerificationSent", JSONObject().put("message", "ENVIAMOS UN NUEVO CORREO DE VERIFICACIÓN.").toString())
                }.addOnFailureListener(this@MainActivity) {
                    dispatch("onNativeEmailAuthError", JSONObject().put("message", emailAuthError(it)).toString())
                }
            }
        }

        @JavascriptInterface fun checkEmailVerification() {
            runOnUiThread {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    dispatch("onNativeEmailAuthError", JSONObject().put("message", "INICIÁ SESIÓN NUEVAMENTE.").toString())
                    return@runOnUiThread
                }
                user.reload().addOnSuccessListener(this@MainActivity) {
                    dispatch("onNativeEmailVerificationStatus", JSONObject()
                        .put("verified", user.isEmailVerified).put("uid", user.uid)
                        .put("email", user.email ?: "").put("name", user.displayName ?: "")
                        .put("message", if (user.isEmailVerified) "CORREO VERIFICADO." else "TODAVÍA NO SE VERIFICÓ EL CORREO.")
                        .toString())
                }.addOnFailureListener(this@MainActivity) {
                    dispatch("onNativeEmailAuthError", JSONObject().put("message", emailAuthError(it)).toString())
                }
            }
        }

        @JavascriptInterface fun signInWithGoogle() {
            runOnUiThread { beginGoogleSignIn() }
        }

        @JavascriptInterface fun loadMapAvailableLines() {
            executor.execute {
                runCatching { api.getLines() }
                    .onSuccess { transitLinesCache = it; dispatch("onNativeMapAvailableLines", lineJson(it)) }
                    .onFailure { dispatch("onNativeMapAvailableLinesError", JSONObject().put("message", it.message ?: "No se pudieron cargar las líneas").toString()) }
            }
        }

        @JavascriptInterface fun loadMapRoute(lineCode: Int) {
            executor.execute {
                runCatching { api.getRoute(lineCode) }
                    .onSuccess { dispatch("onNativeMapRoute", routeJson(it)) }
                    .onFailure { dispatch("onNativeMapRouteError", JSONObject().put("message", it.message ?: "No se pudo cargar el recorrido").toString()) }
            }
        }

        @JavascriptInterface fun requestMapLocation(lineCode: Int, destinationFilter: String) { mapLineCode = lineCode; mapDestinationFilter = destinationFilter.trim(); requestNativeLocation("map") }
        @JavascriptInterface fun requestNearbyLocation() { requestNativeLocation("nearby") }
        @JavascriptInterface fun loadMapData(latitude: Double, longitude: Double, lineCode: Int, destinationFilter: String) { mapDestinationFilter = destinationFilter.trim(); loadMapStopsAt(latitude, longitude, lineCode) }
        @JavascriptInterface fun loadMapDataAtLocation(latitude: Double, longitude: Double, lineCode: Int, destinationFilter: String) { mapDestinationFilter = destinationFilter.trim(); loadMapStopsAt(latitude, longitude, lineCode) }
        private fun loadMapStopsAt(latitude: Double, longitude: Double, lineCode: Int) {
            executor.execute {
                runCatching { api.getNearby(latitude, longitude) }
                    .onSuccess { stops -> mapNearbyStops = stops; mapLineCode = lineCode; dispatch("onNativeMapStops", stopJson(stops)); refreshMapVehiclesNow(lineCode) }
                    .onFailure { dispatch("onNativeMapStopsError", JSONObject().put("message", it.message ?: "No se pudieron localizar las paradas").toString()) }
            }
        }

        @JavascriptInterface fun refreshMapVehicles(lineCode: Int, destinationFilter: String) {
            mapLineCode = lineCode
            mapDestinationFilter = destinationFilter.trim()
            refreshMapVehiclesNow(lineCode)
        }

        @JavascriptInterface fun refreshMapVehiclesManual(lineCode: Int, destinationFilter: String) {
            mapLineCode = lineCode
            mapDestinationFilter = destinationFilter.trim()
            if (mapVehicleRefreshInProgress) {
                pendingMapVehicleRefreshLine = lineCode
                pendingMapVehicleRefreshManual = true
            } else {
                refreshMapVehiclesNow(lineCode, manualRequest = true)
            }
        }

        @JavascriptInterface fun loadLines() {
            executor.execute {
                runCatching { api.getLines() }
                    .onSuccess { transitLinesCache = it; dispatch("onNativeLines", lineJson(it)) }
                    .onFailure { dispatch("onNativeLinesError", JSONObject().put("message", it.message ?: "No se pudieron cargar las líneas").toString()) }
            }
        }

        @JavascriptInterface fun loadStreets(lineCode: Int) {
            executor.execute {
                runCatching { api.getStreets(lineCode) }
                    .onSuccess { dispatch("onNativeStreets", streetJson(it)) }
                    .onFailure { dispatch("onNativeStreetsError", JSONObject().put("message", it.message ?: "No se pudieron cargar las calles").toString()) }
            }
        }

        @JavascriptInterface fun loadIntersections(lineCode: Int, streetCode: Int) {
            executor.execute {
                runCatching { api.getIntersections(lineCode, streetCode) }
                    .onSuccess { dispatch("onNativeIntersections", intersectionJson(it)) }
                    .onFailure { dispatch("onNativeIntersectionsError", JSONObject().put("message", it.message ?: "No se pudieron cargar las intersecciones").toString()) }
            }
        }

        @JavascriptInterface fun loadStops(lineCode: Int, streetCode: Int, intersectionCode: Int) {
            executor.execute {
                runCatching { api.getStops(lineCode, streetCode, intersectionCode) }
                    .onSuccess { dispatch("onNativeStops", stopJson(it)) }
                    .onFailure { dispatch("onNativeStopsError", JSONObject().put("message", it.message ?: "No se pudieron cargar las paradas").toString()) }
            }
        }

        @JavascriptInterface fun loadNearbyStops(latitude: Double, longitude: Double) {
            executor.execute {
                runCatching { api.getNearby(latitude, longitude) }
                    .onSuccess { dispatch("onNativeNearbyStops", stopJson(it)) }
                    .onFailure { dispatch("onNativeNearbyStopsError", JSONObject().put("message", it.message ?: "No se pudieron localizar las paradas").toString()) }
            }
        }

        @JavascriptInterface fun resolveNearbyStopLines(identifier: String, latitude: Double, longitude: Double) {
            executor.execute {
                try {
                    val cacheKey = identifier.ifBlank { "%.6f,%.6f".format(java.util.Locale.US, latitude, longitude) }
                    resolvedStopLines[cacheKey]?.let { cached ->
                        dispatch("onNativeNearbyStopLines", JSONObject()
                            .put("identifier", identifier)
                            .put("lines", JSONArray(cached))
                            .put("lineLabels", JSONObject(lineLabelsJson(cached)))
                            .toString())
                        return@execute
                    }

                    val nearbyAtStop = runCatching { api.getNearby(latitude, longitude) }.getOrDefault(emptyList())
                    val exact = nearbyAtStop.firstOrNull {
                        it.identifier == identifier || it.code.toString() == identifier
                    } ?: nearbyAtStop.minByOrNull { stop ->
                        val dy = (stop.latitude - latitude) * 111_320.0
                        val dx = (stop.longitude - longitude) * 111_320.0 * kotlin.math.cos(Math.toRadians(latitude))
                        kotlin.math.sqrt(dx * dx + dy * dy)
                    }?.takeIf { stop ->
                        val dy = (stop.latitude - latitude) * 111_320.0
                        val dx = (stop.longitude - longitude) * 111_320.0 * kotlin.math.cos(Math.toRadians(latitude))
                        kotlin.math.sqrt(dx * dx + dy * dy) <= 45.0
                    }

                    var matched = exact?.lineCodes.orEmpty().filter { it > 0 }.distinct().sorted()

                    if (matched.isEmpty()) {
                        val candidates = if (transitLinesCache.isNotEmpty()) transitLinesCache
                        else runCatching { api.getLines() }.getOrDefault(emptyList()).also { transitLinesCache = it }

                        val tasks = candidates.map { line ->
                            java.util.concurrent.Callable {
                                val arrivalIdentifier = exact?.identifier?.takeIf { it.isNotBlank() } ?: exact?.code?.toString() ?: identifier
                                val arrivals = runCatching { api.getArrivals(arrivalIdentifier, line.code, timeoutMs = 2_500) }.getOrDefault(emptyList())
                                if (arrivals.isNotEmpty()) {
                                    line.code to true
                                } else {
                                    val route = routeCache[line.code] ?: runCatching { api.getRoute(line.code, timeoutMs = 2_500) }.getOrDefault(emptyList()).also { routeCache[line.code] = it }
                                    line.code to route.any { point -> distanceMeters(latitude, longitude, point.first, point.second) <= 300.0 }
                                }
                            }
                        }

                        matched = runCatching {
                            lineResolutionExecutor.invokeAll(tasks, 12, java.util.concurrent.TimeUnit.SECONDS)
                                .mapNotNull { future -> runCatching { future.get() }.getOrNull() }
                                .filter { it.second }
                                .map { it.first }
                                .distinct()
                                .sorted()
                        }.getOrDefault(emptyList())
                    }

                    if (matched.isNotEmpty()) resolvedStopLines[cacheKey] = matched
                    dispatch("onNativeNearbyStopLines", JSONObject()
                        .put("identifier", identifier)
                        .put("lines", JSONArray(matched))
                        .put("lineLabels", JSONObject(lineLabelsJson(matched)))
                        .toString())
                } catch (e: Exception) {
                    dispatch("onNativeNearbyStopLines", JSONObject()
                        .put("identifier", identifier)
                        .put("lines", JSONArray())
                        .put("lineLabels", JSONObject())
                        .put("message", e.message ?: "No se pudieron consultar las líneas asociadas a la parada")
                        .toString())
                }
            }
        }

        @JavascriptInterface fun loadArrivals(identifier: String, lineCode: Int) {
            executor.execute {
                runCatching { api.getArrivals(identifier.ifBlank { lineCode.toString() }, lineCode) }
                    .onSuccess { dispatch("onNativeArrivals", arrivalJson(it, lineCode)) }
                    .onFailure { dispatch("onNativeArrivalsError", JSONObject().put("message", it.message ?: "No se pudieron cargar los arribos").toString()) }
            }
        }

        @JavascriptInterface fun setAlertConfig(configJson: String) {
            runOnUiThread {
                val config = runCatching { JSONObject(configJson) }.getOrNull()
                val arrival = config?.optJSONObject("arrival")
                val key = config?.optString("arrivalKey").orEmpty()
                val valid = arrival != null && key.isNotBlank() && arrival.optString("key") == key &&
                    arrival.optInt("line") > 0 && arrival.optJSONObject("stop")?.optString("identifier").orEmpty().isNotBlank()
                val times = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
                val start = config?.optString("start").orEmpty()
                val end = config?.optString("end").orEmpty()
                val thresholds = config?.optJSONArray("thresholds")
                val chosen = (0 until (thresholds?.length() ?: 0)).mapNotNull { thresholds?.optInt(it) }.filter { it in listOf(15, 10, 5, 3, 1) }.distinct()
                if (!valid || !times.matches(start) || !times.matches(end) || start == end || chosen.isEmpty()) {
                    dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", "CONFIGURACIÓN DE ALERTAS INVÁLIDA").toString())
                    return@runOnUiThread
                }
                val prefs = getSharedPreferences(ArrivalNotificationService.PREFS, MODE_PRIVATE)
                prefs.edit().putString("thresholds", chosen.joinToString(",")).putBoolean("sound", config?.optBoolean("sound", true) ?: true)
                    .putBoolean("vibrate", config?.optBoolean("vibrate", true) ?: true).apply()
                val favorites = JSONArray().put(arrival).toString()
                pendingArrivalStartTime = start
                pendingArrivalEndTime = end
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingArrivalNotifications = favorites
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
                    return@runOnUiThread
                }
                startArrivalNotifications(favorites, start, end)
            }
        }

        @JavascriptInterface fun setArrivalNotifications(favoritesJson: String, startTime: String, endTime: String) {
            dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", "CONFIGURÁ LAS ALERTAS DESDE LA SECCIÓN ALERTAS").toString())
        }

        @JavascriptInterface fun updateArrivalNotificationFavorites(favoritesJson: String) {
            // La selección se administra únicamente desde Alertas.
        }

        @JavascriptInterface fun stopArrivalNotifications() {
            runOnUiThread {
                getSharedPreferences(ArrivalNotificationService.PREFS, MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
                stopService(android.content.Intent(this@MainActivity, ArrivalNotificationService::class.java))
                dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", "NOTIFICACIONES DESACTIVADAS").toString())
            }
        }

        @JavascriptInterface fun getArrivalNotificationState() {
            val prefs = getSharedPreferences(ArrivalNotificationService.PREFS, MODE_PRIVATE)
            val history = runCatching { JSONArray(prefs.getString("history", "[]")) }.getOrDefault(JSONArray())
            dispatch("onNativeArrivalNotificationsState", JSONObject()
                .put("enabled", prefs.getBoolean("enabled", false))
                .put("startTime", prefs.getString("start_time", "18:00"))
                .put("endTime", prefs.getString("end_time", "19:30"))
                .put("hasSchedule", prefs.contains("start_time") && prefs.contains("end_time"))
                .put("history", history)
                .put("message", if (prefs.getBoolean("enabled", false)) "ALERTAS PROGRAMADAS" else "ALERTAS INACTIVAS")
                .toString())
        }
    }

    private fun startArrivalNotifications(favoritesJson: String, startTime: String, endTime: String) {
        val prefs = getSharedPreferences(ArrivalNotificationService.PREFS, MODE_PRIVATE)
        prefs.edit().putString("favorites", favoritesJson).putString("start_time", startTime)
            .putString("end_time", endTime).putBoolean("enabled", true).apply()
        val intent = android.content.Intent(this, ArrivalNotificationService::class.java)
            .putExtra(ArrivalNotificationService.EXTRA_FAVORITES, favoritesJson)
            .putExtra(ArrivalNotificationService.EXTRA_START_TIME, startTime)
            .putExtra(ArrivalNotificationService.EXTRA_END_TIME, endTime)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", true).put("message", "SEGUIMIENTO DE ARRIBOS ACTIVADO").toString())
        } catch (error: Exception) {
            prefs.edit().putBoolean("enabled", false).apply()
            dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", error.message ?: "NO SE PUDO ACTIVAR EL SEGUIMIENTO").toString())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            val favorites = pendingArrivalNotifications
            pendingArrivalNotifications = null
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED } && favorites != null) {
                startArrivalNotifications(favorites, pendingArrivalStartTime, pendingArrivalEndTime)
            } else {
                dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", "PERMISO DE NOTIFICACIONES DENEGADO").toString())
            }
            return
        }
        if (requestCode != LOCATION_PERMISSION_REQUEST) return
        val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        pendingGeoCallback?.invoke(pendingGeoOrigin ?: "file://", granted, false)
        pendingGeoCallback = null
        pendingGeoOrigin = null
        val pendingPurpose = pendingLocationPurpose
        if (granted) startContinuousLocationTracking()
        if (granted && pendingPurpose != null) {
            requestNativeLocation(pendingPurpose)
        } else if (!granted && pendingPurpose != null) {
            failNativeLocation(pendingPurpose, "PERMISO DE UBICACIÓN DENEGADO")
        }
    }

    override fun onDestroy() {
        pendingGeoCallback?.invoke(pendingGeoOrigin ?: "file://", false, false)
        pendingGeoCallback = null
        pendingGeoOrigin = null
        executor.shutdownNow()
        vehicleExecutor.shutdownNow()
        vehicleQueryExecutor.shutdownNow()
        lineResolutionExecutor.shutdownNow()
        stopContinuousLocationTracking()
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val GOOGLE_SIGN_IN_REQUEST = 4204
        private const val PROFILE_PHOTO_REQUEST = 4205
        private const val LOCATION_PERMISSION_REQUEST = 4102
        private const val NOTIFICATION_PERMISSION_REQUEST = 4103
    }
}
