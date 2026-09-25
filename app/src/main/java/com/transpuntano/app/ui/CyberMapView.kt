package com.transpuntano.app.ui

import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.net.HttpURLConnection
import java.net.URL
import android.util.LruCache
import java.util.concurrent.Executors
import kotlin.math.*

data class MapStop(
    val id: Int,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

class CyberMapView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val cache =
        object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
            override fun sizeOf(
                key: String,
                bitmap: Bitmap
            ): Int = bitmap.byteCount
        }

    private val tileExecutor =
        Executors.newFixedThreadPool(4)

    private val inFlight =
        HashSet<String>()

    private var route = emptyList<Pair<Double, Double>>()
    private var stops = emptyList<MapStop>()
    private var user: Pair<Double, Double>? = null

    private var lat = -33.3017
    private var lon = -66.3378
    private var zoom = 13

    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var scaleChanged = false

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    private var tap: ((MapStop) -> Unit)? = null

    private val mapRect = RectF()

    private val scaleDetector =
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                private var accumulator = 1f

                override fun onScaleBegin(
                    detector: ScaleGestureDetector
                ): Boolean {
                    scaleChanged = true
                    accumulator = 1f
                    return true
                }

                override fun onScale(
                    detector: ScaleGestureDetector
                ): Boolean {
                    accumulator *= detector.scaleFactor

                    if (accumulator > 1.22f) {
                        if (zoom < 21) zoom++
                        accumulator = 1f
                        invalidate()
                    } else if (accumulator < 0.82f) {
                        if (zoom > 11) zoom--
                        accumulator = 1f
                        invalidate()
                    }

                    return true
                }

                override fun onScaleEnd(
                    detector: ScaleGestureDetector
                ) {
                    accumulator = 1f
                }
            }
        )

    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean = true

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (mapRect.contains(e.x, e.y)) {
                        zoom = (zoom + 1).coerceAtMost(21)
                        invalidate()
                    }
                    return true
                }
            }
        )

    fun setRoute(v: List<Pair<Double, Double>>) {
        route = v
        if (v.isNotEmpty()) fit(v)
        invalidate()
    }

    fun setStops(
        v: List<MapStop>,
        fit: Boolean = true
    ) {
        stops = v
        if (fit && v.isNotEmpty()) {
            fit(v.map { it.latitude to it.longitude })
        }
        invalidate()
    }

    fun setUserLocation(
        a: Double,
        b: Double,
        center: Boolean = true
    ) {
        user = a to b
        if (center) {
            lat = a
            lon = b
        }
        invalidate()
    }

    fun setOnStopTap(v: (MapStop) -> Unit) {
        tap = v
    }

    private fun fit(v: List<Pair<Double, Double>>) {
        if (v.isEmpty()) return

        if (width <= 0 || height <= 0) {
            post { fit(v) }
            return
        }

        val minLat = v.minOf { it.first }
        val maxLat = v.maxOf { it.first }
        val minLon = v.minOf { it.second }
        val maxLon = v.maxOf { it.second }

        lat = (minLat + maxLat) / 2.0
        lon = (minLon + maxLon) / 2.0

        if(width<=0 || height<=0){
            post { fit(v) }
            return
        }

        updateMapRect()

        val availableWidth=max(mapRect.width()*0.86f,1f)
        val availableHeight=max(mapRect.height()*0.86f,1f)

        zoom=11

        for(z in 18 downTo 10){
            val topLeft=world(maxLat,minLon,z)
            val bottomRight=world(minLat,maxLon,z)

            val projectedWidth=abs(bottomRight.first-topLeft.first)
            val projectedHeight=abs(bottomRight.second-topLeft.second)

            if(projectedWidth<=availableWidth &&
               projectedHeight<=availableHeight){
                zoom=z
                break
            }
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        c.drawColor(Color.rgb(5, 7, 12))

        updateMapRect()

        if (mapRect.width() <= 0 || mapRect.height() <= 0) return

        c.save()
        c.clipRect(mapRect)

        c.drawRect(
            mapRect,
            Paint().apply {
                color = Color.rgb(238, 234, 227)
                style = Paint.Style.FILL
            }
        )

        tiles(c)
        grid(c)
        line(c)
        stops(c)
        user(c)

        c.restore()

        drawFrame(c)
        drawControls(c)
        drawAttribution(c)
    }

    private fun updateMapRect() {
    val margin = dp(16f)
    val top = dp(55f)
    val bottomMargin = dp(16f)

    val availableWidth = width.toFloat() - margin * 2f
    val availableHeight = height.toFloat() - top - bottomMargin
    val size = min(availableWidth, availableHeight)

    if (size <= 0f) {
        mapRect.setEmpty()
        return
    }

    val left = (width.toFloat() - size) / 2f

    mapRect.set(
        left,
        top,
        left + size,
        top + size
    )
}
