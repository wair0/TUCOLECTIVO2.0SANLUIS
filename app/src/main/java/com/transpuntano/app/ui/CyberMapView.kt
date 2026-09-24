package com.transpuntano.app.ui

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CyberMapView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var points: List<Pair<Double, Double>> = emptyList()

    fun setRoute(route: List<Pair<Double, Double>>) {
        points = route
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(5, 7, 12))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(20, 50, 62)
        val step = 80f
        var x = 0f
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += step }
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += step }

        if (points.size < 2) return
        val minLat = points.minOf { it.first }
        val maxLat = points.maxOf { it.first }
        val minLon = points.minOf { it.second }
        val maxLon = points.maxOf { it.second }
        val latSpan = max(0.00001, maxLat - minLat)
        val lonSpan = max(0.00001, maxLon - minLon)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.argb(70, 0, 240, 255)
        drawRoute(canvas, minLat, maxLat, minLon, maxLon, latSpan, lonSpan)
        paint.strokeWidth = 3f
        paint.color = Color.rgb(0, 240, 255)
        drawRoute(canvas, minLat, maxLat, minLon, maxLon, latSpan, lonSpan)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 40, 180)
        val first = mapPoint(points.first(), minLat, maxLat, minLon, maxLon, latSpan, lonSpan)
        val last = mapPoint(points.last(), minLat, maxLat, minLon, maxLon, latSpan, lonSpan)
        canvas.drawCircle(first.x, first.y, 7f, paint)
        canvas.drawCircle(last.x, last.y, 7f, paint)
    }

    private fun drawRoute(canvas: Canvas, a: Double, b: Double, c: Double, d: Double, aspan: Double, dspan: Double) {
        val path = Path()
        points.forEachIndexed { i, p ->
            val q = mapPoint(p, a, b, c, d, aspan, dspan)
            if (i == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y)
        }
        canvas.drawPath(path, paint)
    }

    private fun mapPoint(p: Pair<Double, Double>, minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, latSpan: Double, lonSpan: Double): PointF {
        val pad = 32f
        val x = pad + ((p.second - minLon) / lonSpan).toFloat() * max(1f, width - pad * 2)
        val y = pad + ((maxLat - p.first) / latSpan).toFloat() * max(1f, height - pad * 2)
        return PointF(min(width - pad, max(pad, x)), min(height - pad, max(pad, y)))
    }
}
