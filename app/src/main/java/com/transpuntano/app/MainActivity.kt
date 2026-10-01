package com.tucolectivo.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
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
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadsImagesAutomatically = true
            settings.textZoom = 100
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setGeolocationEnabled(true)
            addJavascriptInterface(TransitBridge(), "TuColectivoNative")
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        callback.invoke(origin, true, false)
                    } else {
                        pendingGeoOrigin = origin
                        pendingGeoCallback = callback
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
                    }
                }
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.TuColectivo && TuColectivo.closeMenus ? TuColectivo.closeMenus() : false") { result ->
                    if (result != "true") finish()
                }
            }
        })
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

    inner class TransitBridge {
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

        @JavascriptInterface fun loadArrivals(identifier: String, lineCode: Int) {
            executor.execute {
                runCatching { api.getArrivals(identifier, lineCode) }
                    .onSuccess { dispatch("onNativeArrivals", arrivalJson(it)) }
                    .onFailure { dispatch("onNativeArrivalsError", JSONObject().put("message", it.message ?: "No se pudieron cargar los arribos").toString()) }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
    }
}
