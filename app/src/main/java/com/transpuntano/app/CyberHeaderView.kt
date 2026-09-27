package com.transpuntano.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.FrameLayout

/** Header con marco biselado neon. */
class CyberHeaderView(context: Context) : FrameLayout(context) {
    private val path = Path()
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val d = resources.displayMetrics.density
        val chamfer = 18f * d
        path.reset()
        path.moveTo(chamfer, 0f)
        path.lineTo(w - chamfer, 0f)
        path.lineTo(w, chamfer)
        path.lineTo(w, h - chamfer)
        path.lineTo(w - chamfer, h)
        path.lineTo(chamfer, h)
        path.lineTo(0f, h - chamfer)
        path.lineTo(0f, chamfer)
        path.close()
        glow.style = Paint.Style.STROKE
        glow.strokeWidth = 4f * d
        glow.color = 0x6600F0FF.toInt()
        canvas.drawPath(path, glow)
        line.style = Paint.Style.STROKE
        line.strokeWidth = 1.5f * d
        line.color = 0xFF00F0FF.toInt()
        canvas.drawPath(path, line)
    }
}
