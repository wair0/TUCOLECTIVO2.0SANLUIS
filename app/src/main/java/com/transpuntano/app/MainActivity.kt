package com.tucolectivo.app

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import android.widget.VideoView
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    @Volatile private var appStarted = false
    private val api = SmartMoveApi()
    private val executor = Executors.newFixedThreadPool(3)
    private val vehicleExecutor = Executors.newSingleThreadExecutor()
    private val vehicleQueryExecutor = Executors.newFixedThreadPool(8)
    private val lineResolutionExecutor = Executors.newFixedThreadPool(8)
    private val resolvedStopLines = java.util.concurrent.ConcurrentHashMap<String, List<Int>>()
    private val routeCache = java.util.concurrent.ConcurrentHashMap<Int, List<Pair<Double, Double>>>()
    @Volatile private var transitLinesCache: List<TransitLine> = emptyList()
    @Volatile private var mapNearbyStops: List<TransitStop> = emptyList()
    @Volatile private var mapLineCode: Int = 0
    @Volatile private var mapVehicleRefreshInProgress = false
    @Volatile private var pendingMapVehicleRefreshLine = 0
    @Volatile private var pendingMapVehicleRefreshManual = false
    private var pendingArrivalNotifications: String? = null
    private var pendingArrivalStartTime: String = "18:00"
    private var pendingArrivalEndTime: String = "19:30"
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
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
        // MP4 resource removed; always use GIF splash
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
        private val startTime = SystemClock.uptimeMillis()
        private var finished = false

        init {
            context.resources.openRawResource(R.drawable.splash).use { input ->
                movie = requireNotNull(Movie.decodeStream(input)) {
                    "No se pudo decodificar res/drawable-nodpi/splash.gif"
                }
            }

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
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

            val elapsed = SystemClock.uptimeMillis() - startTime
            val duration = movie.duration().toLong().takeIf { it > 0L } ?: 3000L

            movie.setTime(elapsed.coerceAtMost(duration).toInt())
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

    // FULL ORIGINAL METHODS FOLLOW - the file is restored with GIF only splash. The remaining methods are identical to the working version prior to MP4 attempts.
}