package com.tucolectivo.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.tucolectivo.app.data.SmartMoveApi
import com.tucolectivo.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val api = SmartMoveApi()
    private val executor = Executors.newFixedThreadPool(3)
    @Volatile private var mapNearbyStops: List<TransitStop> = emptyList()
    @Volatile private var mapLineCode: Int = 0
    @Volatile private var mapVehicleRefreshInProgress = false
    private var pendingArrivalNotifications: String? = null
    private var pendingArrivalStartTime: String = "18:00"
    private var pendingArrivalEndTime: String = "19:30"
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showSplash() }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startApp() {
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            settings.allowFileAccess = true; settings.allowContentAccess = true
            settings.loadsImagesAutomatically = true; settings.textZoom = 100
            settings.setSupportZoom(false); settings.builtInZoomControls = false
            settings.displayZoomControls = false; settings.setGeolocationEnabled(true)
            addJavascriptInterface(TransitBridge(), "TuColectivoNative")
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) callback.invoke(origin, true, false)
                    else { pendingGeoOrigin=origin; pendingGeoCallback=callback; requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST) }
                }
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.TuColectivo && window.TuColectivo.handleBack ? window.TuColectivo.handleBack() : false") { result ->
                    if (result == "false") finish()
                }
            }
        })
    }

    private fun showSplash() {
        val root=FrameLayout(this); root.setBackgroundColor(0xFF050316.toInt())
        root.addView(AnimatedGifBackgroundView(this),FrameLayout.LayoutParams(-1,-1))
        val overlay=FrameLayout(this); root.addView(overlay,FrameLayout.LayoutParams(-1,-1))
        val title=TextView(this).apply{
            text="TU COLECTIVO 2.0 SAN LUIS"; setTextColor(0xFFFF1744.toInt()); textSize=25f; gravity=Gravity.CENTER
            typeface=runCatching{Typeface.createFromAsset(assets,"fonts/cyberpunk.ttf")}.getOrDefault(Typeface.create(Typeface.MONOSPACE,Typeface.BOLD))
            setShadowLayer(14f,0f,0f,0xFFFF1744.toInt())
        }
        overlay.addView(title,FrameLayout.LayoutParams(-1,-2,Gravity.CENTER).apply{leftMargin=18;rightMargin=18;topMargin=-55})
        overlay.addView(SplashProgressView(this,10000L),FrameLayout.LayoutParams(-1,46,Gravity.CENTER).apply{leftMargin=42;rightMargin=42;topMargin=45})
        val statusMessages=arrayOf("ACTIVANDO SISTEMA...","CARGANDO LINEAS...","CARGANDO MAPA...","CARGANDO PARADAS CERCANAS...","CARGANDO FAVORITOS...","SINCRONIZANDO GPS...")
        val status=TextView(this).apply{
            text=statusMessages[0]; setTextColor(0xFFFF1744.toInt()); textSize=13f; gravity=Gravity.CENTER
            typeface=runCatching{Typeface.createFromAsset(assets,"fonts/cyberpunk.ttf")}.getOrDefault(Typeface.create(Typeface.MONOSPACE,Typeface.BOLD))
            setShadowLayer(10f,0f,0f,0xFFFF1744.toInt()); alpha=1f
        }
        overlay.addView(status,FrameLayout.LayoutParams(-1,-2,Gravity.CENTER).apply{leftMargin=18;rightMargin=18;topMargin=105})
        val statusHandler=Handler(Looper.getMainLooper())
        val statusInterval=1500L
        var statusIndex=0
        val statusRunnable=object:Runnable{
            override fun run(){
                if(!status.isAttachedToWindow)return
                status.animate().alpha(0f).setDuration(180L).withEndAction{
                    statusIndex++
                    if(statusIndex<statusMessages.size){
                        status.text=statusMessages[statusIndex]
                        status.animate().alpha(1f).setDuration(180L).start()
                        statusHandler.postDelayed(this,statusInterval-360L)
                    }
                }.start()
            }
        }
        statusHandler.postDelayed(statusRunnable,statusInterval)
        setContentView(root); Handler(Looper.getMainLooper()).postDelayed({startApp()},10000L)
    }

    private class SplashProgressView(context: android.content.Context,private val durationMs:Long):View(context){
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG); private val started=SystemClock.uptimeMillis()
        private val tick=object:Runnable{override fun run(){if(!isAttachedToWindow)return;invalidate();postOnAnimation(this)}}
        init{postOnAnimation(tick)}
        override fun onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow()}
        override fun onDraw(c:Canvas){val p=((SystemClock.uptimeMillis()-started).toFloat()/durationMs).coerceIn(0f,1f);val w=width.toFloat();val y=height*.5f
            paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;paint.color=0xFFFF1744.toInt();c.drawRoundRect(2f,y-8f,w-2f,y+8f,8f,8f,paint)
            paint.style=Paint.Style.FILL;paint.color=0xFFFF1744.toInt();c.drawRoundRect(5f,y-5f,5f+(w-10f)*p,y+5f,5f,5f,paint);paint.color=0xFFFF1744.toInt();c.drawCircle(5f+(w-10f)*p,y,5f,paint)}
    }

    private class AnimatedGifBackgroundView(context: android.content.Context):View(context){
        private var movie:android.graphics.Movie?=null; private var startedAt=0L
        private val invalidator=object:Runnable{override fun run(){if(!isAttachedToWindow)return;invalidate();postOnAnimation(this)}}
        init{setWillNotDraw(false);isClickable=false;isFocusable=false;setLayerType(View.LAYER_TYPE_SOFTWARE,null);movie=runCatching{context.assets.open("background_cyberpunk.gif").use{android.graphics.Movie.decodeStream(it)}}.getOrNull()}
        override fun onAttachedToWindow(){super.onAttachedToWindow();startedAt=SystemClock.uptimeMillis();postOnAnimation(invalidator)}
        override fun onDetachedFromWindow(){removeCallbacks(invalidator);super.onDetachedFromWindow()}
        override fun onDraw(c:Canvas){val gif=movie?:return;if(width<=0||height<=0)return;val duration=gif.duration().takeIf{it>0}?:12000;gif.setTime(((SystemClock.uptimeMillis()-startedAt)%duration).toInt());val mw=gif.width().toFloat();val mh=gif.height().toFloat();if(mw<=0f||mh<=0f)return;val scale=maxOf(width/mw,height/mh);val dw=mw*scale;val dh=mh*scale;c.save();c.translate((width-dw)*.5f,(height-dh)*.5f);c.scale(scale,scale);gif.draw(c,0f,0f);c.restore()}
    }

    private fun dispatch(functionName: String, payload: String) {
        val safe = JSONObject.quote(payload)
        runOnUiThread { webView.evaluateJavascript("window.$functionName && window.$functionName($safe)", null) }
    }

    private fun lineJson(items: List<TransitLine>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("code", it.code).put("name", it.name).put("raw", it.raw)) }
    }.toString()

    private fun streetJson(items: List<TransitStreet>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("code", it.code).put("name", it.name)) }
    }.toString()

    private fun intersectionJson(items: List<TransitIntersection>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("code", it.code).put("name", it.name)) }
    }.toString()

    private fun stopJson(items: List<TransitStop>) = JSONArray().apply {
        items.forEach { stop ->
            val lineCodes = JSONArray().apply { stop.lineCodes.forEach { put(it) } }
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
            )
        }
    }.toString()

    private fun arrivalJson(items: List<TransitArrival>) = JSONArray().apply {
        items.forEach { arrival ->
            put(
                JSONObject()
                    .put("line", arrival.line)
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

    private fun refreshMapVehiclesNow(lineCode: Int = mapLineCode) {
        if (mapVehicleRefreshInProgress || mapNearbyStops.isEmpty()) return
        mapVehicleRefreshInProgress = true
        executor.execute {
            try {
                val vehicles = linkedMapOf<String, JSONObject>()
                for (stop in mapNearbyStops) {
                    val codes = if (lineCode > 0) {
                        if (lineCode in stop.lineCodes) listOf(lineCode) else emptyList()
                    } else stop.lineCodes.filter { it > 0 }.distinct()
                    for (code in codes) {
                        val arrivals = runCatching {
                            api.getArrivals(stop.identifier, code, timeoutMs = 5_000)
                        }.getOrDefault(emptyList())
                        for (arrival in arrivals) {
                            val lat = arrival.latitude ?: continue
                            val lng = arrival.longitude ?: continue
                            if (lat == 0.0 || lng == 0.0) continue
                            val id = arrival.vehicleId.ifBlank {
                                "line-" + code + "-" + stop.code + "-" + lat + "-" + lng
                            }
                            vehicles[id] = JSONObject()
                                .put("id", id)
                                .put("line", arrival.line.ifBlank { "LÍNEA " + code })
                                .put("destination", arrival.destination)
                                .put("lat", lat)
                                .put("lng", lng)
                                .put("gpsTimestamp", arrival.gpsTimestamp)
                        }
                    }
                }
                dispatch("onNativeMapVehicles", JSONArray().apply {
                    vehicles.values.forEach { put(it) }
                }.toString())
            } finally {
                mapVehicleRefreshInProgress = false
            }
        }
    }

    inner class TransitBridge {
        @JavascriptInterface fun loadMapAvailableLines() {
            executor.execute {
                runCatching { api.getLines() }
                    .onSuccess { dispatch("onNativeMapAvailableLines", lineJson(it)) }
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

        @JavascriptInterface fun loadMapData(latitude: Double, longitude: Double, lineCode: Int) {
            executor.execute {
                runCatching { api.getNearby(latitude, longitude) }
                    .onSuccess { stops ->
                        mapNearbyStops = stops
                        mapLineCode = lineCode
                        dispatch("onNativeMapStops", stopJson(stops))
                        refreshMapVehiclesNow(lineCode)
                    }
                    .onFailure { dispatch("onNativeMapStopsError", JSONObject().put("message", it.message ?: "No se pudieron localizar las paradas").toString()) }
            }
        }

        @JavascriptInterface fun refreshMapVehicles(lineCode: Int) {
            mapLineCode = lineCode
            refreshMapVehiclesNow(lineCode)
        }

        @JavascriptInterface fun loadLines() {
            executor.execute {
                runCatching { api.getLines() }
                    .onSuccess { dispatch("onNativeLines", lineJson(it)) }
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
                    val lines = api.getLines()
                    val matched = mutableListOf<Int>()
                    for (line in lines) {
                        val points = runCatching { api.getRoute(line.code) }.getOrDefault(emptyList())
                        // Medir la distancia a cada segmento del recorrido, no solo a los vértices:
                        // los puntos de SmartMove pueden estar separados y una parada caer entre ellos.
                        val cosLat = kotlin.math.cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
                        fun distanceMeters(point: Pair<Double, Double>): Double {
                            val dy = (point.first - latitude) * 111_320.0
                            val dx = (point.second - longitude) * 111_320.0 * cosLat
                            return kotlin.math.sqrt(dx * dx + dy * dy)
                        }
                        var nearest = Double.POSITIVE_INFINITY
                        if (points.size == 1) nearest = distanceMeters(points[0])
                        for (i in 0 until (points.size - 1).coerceAtLeast(0)) {
                            val a = points[i]
                            val b = points[i + 1]
                            val ax = (a.second - longitude) * 111_320.0 * cosLat
                            val ay = (a.first - latitude) * 111_320.0
                            val bx = (b.second - longitude) * 111_320.0 * cosLat
                            val by = (b.first - latitude) * 111_320.0
                            val vx = bx - ax
                            val vy = by - ay
                            val denom = vx * vx + vy * vy
                            val t = if (denom <= 0.0001) 0.0 else ((-ax * vx - ay * vy) / denom).coerceIn(0.0, 1.0)
                            val dx = ax + t * vx
                            val dy = ay + t * vy
                            nearest = minOf(nearest, kotlin.math.sqrt(dx * dx + dy * dy))
                        }
                        // 80 m permite separar corredores cercanos y tolera GPS/trazado imperfecto.
                        if (nearest <= 80.0) matched.add(line.code)
                    }
                    dispatch("onNativeNearbyStopLines", JSONObject()
                        .put("identifier", identifier)
                        .put("lines", JSONArray(matched.distinct().sorted())).toString())
                } catch (e: Exception) {
                    dispatch("onNativeNearbyStopLines", JSONObject()
                        .put("identifier", identifier)
                        .put("lines", JSONArray()).toString())
                }
            }
        }

        @JavascriptInterface fun loadArrivals(identifier: String, lineCode: Int) {
            executor.execute {
                runCatching { api.getArrivals(identifier, lineCode) }
                    .onSuccess { dispatch("onNativeArrivals", arrivalJson(it)) }
                    .onFailure { dispatch("onNativeArrivalsError", JSONObject().put("message", it.message ?: "No se pudieron cargar los arribos").toString()) }
            }
        }

        @JavascriptInterface fun setArrivalNotifications(favoritesJson: String, startTime: String, endTime: String) {
            runOnUiThread {
                val favorites = runCatching { JSONArray(favoritesJson) }.getOrNull()
                if (favorites == null || favorites.length() == 0) {
                    dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", "GUARDÁ AL MENOS UN ARRIBO EN FAVORITOS").toString())
                    return@runOnUiThread
                }
                val timePattern = Regex("^(?:([01]\\d|2[0-3])):([0-5]\\d)$")
                if (!timePattern.matches(startTime) || !timePattern.matches(endTime) || endTime <= startTime) {
                    dispatch("onNativeArrivalNotificationsState", JSONObject().put("enabled", false).put("message", "CONFIGURÁ UN HORARIO VÁLIDO").toString())
                    return@runOnUiThread
                }
                pendingArrivalStartTime = startTime
                pendingArrivalEndTime = endTime
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingArrivalNotifications = favoritesJson
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
                    return@runOnUiThread
                }
                startArrivalNotifications(favoritesJson, startTime, endTime)
            }
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
    }

    override fun onDestroy() {
        pendingGeoCallback?.invoke(pendingGeoOrigin ?: "file://", false, false)
        pendingGeoCallback = null
        pendingGeoOrigin = null
        executor.shutdownNow()
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 4102
        private const val NOTIFICATION_PERMISSION_REQUEST = 4103
    }
}
