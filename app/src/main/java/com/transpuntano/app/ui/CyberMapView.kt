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

    private fun world(
        a: Double,
        b: Double,
        z: Int
    ): Pair<Double, Double> {
        val n = 2.0.pow(z)
        val x = (b + 180.0) / 360.0 * 256.0 * n
        val s = sin(Math.toRadians(a)).coerceIn(-.9999, .9999)
        val y =
            (.5 - ln((1 + s) / (1 - s)) / (4 * Math.PI)) *
                    256.0 * n

        return x to y
    }

    private fun screen(
        a: Double,
        b: Double
    ): PointF {
        val center = world(lat, lon, zoom)
        val p = world(a, b, zoom)

        return PointF(
            (
                p.first -
                        center.first +
                        mapRect.centerX()
                ).toFloat(),
            (
                p.second -
                        center.second +
                        mapRect.centerY()
                ).toFloat()
        )
    }

    private fun tiles(c: Canvas) {
        val center = world(lat, lon, zoom)

        val left = center.first - mapRect.width() / 2.0
        val top = center.second - mapRect.height() / 2.0

        val minX =
            floor(left / 256.0).toInt() - 1

        val maxX =
            floor((left + mapRect.width()) / 256.0).toInt() + 1

        val minY =
            floor(top / 256.0).toInt() - 1

        val maxY =
            floor((top + mapRect.height()) / 256.0).toInt() + 1

        val n = 1 shl zoom

        for (rx in minX..maxX) {
            for (y in minY..maxY) {

                if (y !in 0 until n) continue

                val x = ((rx % n) + n) % n
                val key = "$zoom/$x/$y"

                val dx =
                    mapRect.left +
                            (rx * 256.0 - left)

                val dy =
                    mapRect.top +
                            (y * 256.0 - top)

                val bm =
                    synchronized(cache) {
                        cache.get(key)
                    }

                if (bm != null) {
                    c.drawBitmap(
                        bm,
                        null,
                        RectF(
                            dx.toFloat(),
                            dy.toFloat(),
                            (dx + 256).toFloat(),
                            (dy + 256).toFloat()
                        ),
                        paint
                    )
                } else {
                    val fallback = findFallbackTile(x, y, zoom)

                    if (fallback != null) {
                        c.drawBitmap(
                            fallback.first,
                            fallback.second,
                            RectF(
                                dx.toFloat(),
                                dy.toFloat(),
                                (dx + 256).toFloat(),
                                (dy + 256).toFloat()
                            ),
                            paint
                        )
                    } else {
                    }

                    download(key, x, y, zoom)
                }
            }
        }
    }

    private fun findFallbackTile(
        x: Int,
        y: Int,
        z: Int
    ): Pair<Bitmap, Rect>?
    {
        for (delta in 1..4) {
            val parentZoom = z - delta
            if (parentZoom < 0) break

            val factor = 1 shl delta
            val parentXRaw = floor(x.toDouble() / factor).toInt()
            val parentY = floor(y.toDouble() / factor).toInt()
            val parentCount = 1 shl parentZoom

            if (parentY !in 0 until parentCount) continue

            val parentX =
                ((parentXRaw % parentCount) + parentCount) % parentCount

            val key = "$parentZoom/$parentX/$parentY"

            val bitmap = synchronized(cache) {
                cache.get(key)
            } ?: continue

            val localX =
                x - parentXRaw * factor

            val localY =
                y - parentY * factor

            val sourceSize =
                256 / factor

            val source = Rect(
                localX * sourceSize,
                localY * sourceSize,
                (localX + 1) * sourceSize,
                (localY + 1) * sourceSize
            )

            return bitmap to source
        }

        return null
    }

    private fun download(
        key: String,
        x: Int,
        y: Int,
        z: Int
    ) {
        if (z > 19) return

        synchronized(cache) {
            if (cache.get(key) != null) return
            if (!inFlight.add(key)) return
        }

        tileExecutor.execute {
            var bitmap: Bitmap? = null

            try {
                val connection =
                    URL(
                        "https://tile.openstreetmap.org/$z/$x/$y.png"
                    ).openConnection() as HttpURLConnection

                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.useCaches = true
                connection.setRequestProperty(
                    "User-Agent",
                    "TU-COLECTIVO-2.0 Android"
                )

                connection.inputStream.use {
                    bitmap = BitmapFactory.decodeStream(it)
                }

                if (bitmap != null) {
                    synchronized(cache) {
                        cache.put(key, bitmap!!)
                    }
                }

            } catch (_: Exception) {
                // Se elimina de inFlight abajo para permitir reintento.
            } finally {
                synchronized(cache) {
                    inFlight.remove(key)
                }
                postInvalidateOnAnimation()
            }
        }
    }

    private fun grid(c: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(35, 0, 240, 255)

        var x = mapRect.left
        while (x < mapRect.right) {
            c.drawLine(
                x,
                mapRect.top,
                x,
                mapRect.bottom,
                paint
            )
            x += dp(80f)
        }

        var y = mapRect.top
        while (y < mapRect.bottom) {
            c.drawLine(
                mapRect.left,
                y,
                mapRect.right,
                y,
                paint
            )
            y += dp(80f)
        }
    }

    private fun line(c: Canvas) {
        if (route.size < 2) return

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        paint.strokeWidth = dp(10f)
        paint.color = Color.argb(90, 0, 240, 255)
        path(c, route)

        paint.strokeWidth = dp(4f)
        paint.color = Color.rgb(0, 240, 255)
        path(c, route)
    }

    private fun path(
        c: Canvas,
        v: List<Pair<Double, Double>>
    ) {
        val p = Path()

        v.forEachIndexed { i, point ->
            val q = screen(
                point.first,
                point.second
            )

            if (i == 0) {
                p.moveTo(q.x, q.y)
            } else {
                p.lineTo(q.x, q.y)
            }
        }

        c.drawPath(p, paint)
    }

    private fun stops(c: Canvas) {
        stops.forEach { stop ->
            val p =
                screen(
                    stop.latitude,
                    stop.longitude
                )

            if (
                p.x !in mapRect.left - dp(20f)..mapRect.right + dp(20f) ||
                p.y !in mapRect.top - dp(20f)..mapRect.bottom + dp(20f)
            ) return@forEach

            paint.style = Paint.Style.FILL
            paint.color =
                Color.argb(90, 255, 45, 178)

            c.drawCircle(
                p.x,
                p.y,
                dp(12f),
                paint
            )

            paint.color =
                Color.rgb(255, 45, 178)

            c.drawCircle(
                p.x,
                p.y,
                dp(5f),
                paint
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.5f)
            paint.color = Color.WHITE

            c.drawCircle(
                p.x,
                p.y,
                dp(7f),
                paint
            )
        }
    }

    private fun user(c: Canvas) {
        val u = user ?: return

        val p =
            screen(
                u.first,
                u.second
            )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color =
            Color.rgb(0, 240, 255)

        c.drawCircle(
            p.x,
            p.y,
            dp(18f),
            paint
        )

        paint.style = Paint.Style.FILL

        c.drawCircle(
            p.x,
            p.y,
            dp(6f),
            paint
        )
    }

    private fun drawFrame(c: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color = Color.rgb(0, 240, 255)

        c.drawRoundRect(
            mapRect,
            dp(8f),
            dp(8f),
            paint
        )

        paint.strokeWidth = dp(1f)
        paint.color =
            Color.argb(80, 255, 45, 178)

        c.drawRoundRect(
            RectF(
                mapRect.left + dp(4f),
                mapRect.top + dp(4f),
                mapRect.right - dp(4f),
                mapRect.bottom - dp(4f)
            ),
            dp(6f),
            dp(6f),
            paint
        )
    }

    private fun drawControls(c: Canvas) {
        val size = dp(44f)
        val right = mapRect.right - dp(12f)

        drawControl(
            c,
            RectF(
                right - size,
                mapRect.top + dp(12f),
                right,
                mapRect.top + dp(12f) + size
            ),
            "+"
        )

        drawControl(
            c,
            RectF(
                right - size,
                mapRect.top + dp(64f),
                right,
                mapRect.top + dp(64f) + size
            ),
            "−"
        )
    }

    private fun drawControl(
        c: Canvas,
        rect: RectF,
        text: String
    ) {
        paint.style = Paint.Style.FILL
        paint.color =
            Color.argb(220, 5, 7, 12)

        c.drawRoundRect(
            rect,
            dp(6f),
            dp(6f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.5f)
        paint.color =
            Color.rgb(0, 240, 255)

        c.drawRoundRect(
            rect,
            dp(6f),
            dp(6f),
            paint
        )

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.MONOSPACE
        paint.textSize = dp(25f)
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE

        c.drawText(
            text,
            rect.centerX(),
            rect.centerY() + dp(9f),
            paint
        )

        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawAttribution(c: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color =
            Color.argb(210, 5, 7, 12)

        c.drawRoundRect(
            RectF(
                mapRect.left + dp(8f),
                mapRect.bottom - dp(28f),
                mapRect.left + dp(180f),
                mapRect.bottom - dp(6f)
            ),
            dp(4f),
            dp(4f),
            paint
        )

        paint.typeface = Typeface.MONOSPACE
        paint.textSize = dp(8f)
        paint.color = Color.WHITE

        c.drawText(
            "© OpenStreetMap contributors",
            mapRect.left + dp(14f),
            mapRect.bottom - dp(13f),
            paint
        )
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {

        scaleDetector.onTouchEvent(e)
        gestureDetector.onTouchEvent(e)

        if (
            e.actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
            scaleDetector.isInProgress
        ) {
            scaleChanged = true
            return true
        }

        when (e.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                moved = false
                scaleChanged = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (e.pointerCount > 1) {
                    scaleChanged = true
                    return true
                }

                val dx = e.x - downX
                val dy = e.y - downY

                if (
                    abs(dx) + abs(dy) >
                    dp(3f)
                ) {
                    moved = true
                }

                if (moved) {
                    val center =
                        world(lat, lon, zoom)

                    val n =
                        unworld(
                            center.first - dx,
                            center.second - dy,
                            zoom
                        )

                    lat =
                        n.first.coerceIn(
                            -85.0,
                            85.0
                        )

                    lon = n.second

                    downX = e.x
                    downY = e.y

                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_UP -> {

                if (scaleChanged) {
                    scaleChanged = false
                    moved = false
                    return true
                }

                val now = System.currentTimeMillis()

                val doubleTap =
                    now - lastTapTime < 320 &&
                            hypot(
                                e.x - lastTapX,
                                e.y - lastTapY
                            ) < dp(32f)

                if (doubleTap) {
                    lastTapTime = 0L
                    return true
                }

                lastTapTime = now
                lastTapX = e.x
                lastTapY = e.y

                val right = mapRect.right - dp(12f)
                val size = dp(44f)

                val plus =
                    RectF(
                        right - size,
                        mapRect.top + dp(12f),
                        right,
                        mapRect.top + dp(12f) + size
                    )

                val minus =
                    RectF(
                        right - size,
                        mapRect.top + dp(64f),
                        right,
                        mapRect.top + dp(64f) + size
                    )

                if (plus.contains(e.x, e.y)) {
                    zoom =
                        (zoom + 1).coerceAtMost(21)
                    invalidate()
                    return true
                }

                if (minus.contains(e.x, e.y)) {
                    zoom =
                        (zoom - 1).coerceAtLeast(11)
                    invalidate()
                    return true
                }

                if (
                    !moved &&
                    mapRect.contains(e.x, e.y)
                ) {
                    stops.minByOrNull {
                        val p =
                            screen(
                                it.latitude,
                                it.longitude
                            )

                        hypot(
                            p.x - e.x,
                            p.y - e.y
                        )
                    }?.let {
                        val p =
                            screen(
                                it.latitude,
                                it.longitude
                            )

                        if (
                            hypot(
                                p.x - e.x,
                                p.y - e.y
                            ) < dp(32f)
                        ) {
                            tap?.invoke(it)
                        }
                    }
                }

                return true
            }
        }

        return true
    }

    private fun unworld(
        x: Double,
        y: Double,
        z: Int
    ): Pair<Double, Double> {
        val n =
            Math.PI -
                    2 *
                    Math.PI *
                    y /
                    (256.0 * 2.0.pow(z))

        return Math.toDegrees(
            atan(sinh(n))
        ) to
                (
                    x /
                            (256.0 * 2.0.pow(z)) *
                            360.0 -
                            180.0
                    )
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
