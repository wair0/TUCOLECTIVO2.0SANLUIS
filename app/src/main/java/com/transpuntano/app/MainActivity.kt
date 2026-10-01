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
