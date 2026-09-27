package com.transpuntano.app

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * Header HUD cyberpunk: marco angular, glow neon, sin lineas cruzando el texto de estado.
 */
class CyberHeaderHud(context: Context) : View(context) {
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cyan = 0xFF00F0FF.toInt()
        val pink = 0xFFFF2DB2.toInt()
        val d = resources.displayMetrics.density
        fun px(v: Float) = v * d

        canvas.drawColor(0xFF010408.toInt())

        val inset = px(4f)
        val cut = px(12f)
        path.reset()
        path.moveTo(inset + cut, inset)
        path.lineTo(w - inset - cut, inset)
        path.lineTo(w - inset, inset + cut)
        path.lineTo(w - inset, h - inset - cut)
        path.lineTo(w - inset - cut, h - inset)
        path.lineTo(inset + cut, h - inset)
        path.lineTo(inset, h - inset - cut)
        path.lineTo(inset, inset + cut)
        path.close()
        fill.color = 0xFF030912.toInt()
        fill.alpha = 255
        canvas.drawPath(path, fill)

        glow.color = cyan
        glow.alpha = 70
        glow.strokeWidth = px(3f)
        canvas.drawPath(path, glow)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = px(1.4f)
        paint.color = cyan
        paint.alpha = 240
        canvas.drawPath(path, paint)

        paint.strokeWidth = px(0.8f)
        paint.alpha = 90
        val inset2 = inset + px(3f)
        val cut2 = (cut - px(2f)).coerceAtLeast(px(4f))
        path.reset()
        path.moveTo(inset2 + cut2, inset2)
        path.lineTo(w - inset2 - cut2, inset2)
        path.lineTo(w - inset2, inset2 + cut2)
        path.lineTo(w - inset2, h - inset2 - cut2)
        path.lineTo(w - inset2 - cut2, h - inset2)
        path.lineTo(inset2 + cut2, h - inset2)
        path.lineTo(inset2, h - inset2 - cut2)
        path.lineTo(inset2, inset2 + cut2)
        path.close()
        canvas.drawPath(path, paint)

        paint.strokeWidth = px(1.2f)
        paint.color = cyan
        paint.alpha = 250
        canvas.drawLine(inset + cut, px(2.5f), px(70f), px(2.5f), paint)
        canvas.drawLine(w - px(70f), px(2.5f), w - inset - cut, px(2.5f), paint)
        paint.alpha = 110
        canvas.drawLine(px(78f), px(2.5f), px(108f), px(2.5f), paint)
        canvas.drawLine(w - px(108f), px(2.5f), w - px(78f), px(2.5f), paint)

        paint.color = pink
        paint.alpha = 200
        paint.strokeWidth = px(1.5f)
        canvas.drawLine(px(70f), px(1.5f), px(78f), px(1.5f), paint)
        canvas.drawLine(w - px(78f), px(1.5f), w - px(70f), px(1.5f), paint)

        paint.color = cyan
        paint.strokeWidth = px(1.2f)
        paint.alpha = 210
        canvas.drawLine(inset, px(18f), inset, h - px(14f), paint)
        canvas.drawLine(inset, h - px(14f), inset + px(14f), h - px(14f), paint)
        canvas.drawLine(w - inset, px(18f), w - inset, h - px(14f), paint)
        canvas.drawLine(w - inset, h - px(14f), w - inset - px(14f), h - px(14f), paint)

        paint.alpha = 230
        paint.strokeWidth = px(1.5f)
        canvas.drawLine(inset + px(2f), inset + cut, inset + px(2f), inset + px(4f), paint)
        canvas.drawLine(inset + cut, inset + px(2f), inset + px(4f), inset + px(2f), paint)
        canvas.drawLine(w - inset - px(2f), inset + cut, w - inset - px(2f), inset + px(4f), paint)
        canvas.drawLine(w - inset - cut, inset + px(2f), w - inset - px(4f), inset + px(2f), paint)
        canvas.drawLine(inset + px(2f), h - inset - cut, inset + px(2f), h - inset - px(4f), paint)
        canvas.drawLine(inset + cut, h - inset - px(2f), inset + px(4f), h - inset - px(2f), paint)
        canvas.drawLine(w - inset - px(2f), h - inset - cut, w - inset - px(2f), h - inset - px(4f), paint)
        canvas.drawLine(w - inset - cut, h - inset - px(2f), w - inset - px(4f), h - inset - px(2f), paint)

        paint.color = cyan
        paint.alpha = 200
        paint.strokeWidth = px(1f)
        for (i in 0..3) {
            val x = px(18f + i * 6f)
            canvas.drawLine(x, h - px(10f), x, h - px(5f), paint)
        }
        for (i in 0..3) {
            val x = w - px(18f + i * 6f)
            canvas.drawLine(x, h - px(10f), x, h - px(5f), paint)
        }

        paint.color = cyan
        paint.alpha = 255
        paint.strokeWidth = px(2f)
        canvas.drawLine(inset + px(6f), h - px(2f), w - inset - px(6f), h - px(2f), paint)

        paint.color = pink
        paint.alpha = 180
        paint.strokeWidth = px(1f)
        canvas.drawLine(w * 0.38f, h - px(5.5f), w * 0.62f, h - px(5.5f), paint)

        fill.color = cyan
        fill.alpha = 220
        canvas.drawCircle(inset + cut * 0.5f, inset + cut * 0.5f, px(1.4f), fill)
        canvas.drawCircle(w - inset - cut * 0.5f, inset + cut * 0.5f, px(1.4f), fill)
        fill.color = pink
        fill.alpha = 200
        canvas.drawCircle(w * 0.5f, h - px(2f), px(1.6f), fill)
    }
}
