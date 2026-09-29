package com.tucolectivo.app.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Fondo GIF cyberpunk optimizado.
 *
 * API 28+: AnimatedImageDrawable (decodifica en background, sin Movie+SOFTWARE a 60fps).
 * API <28: Movie con throttle ~12 fps para no matar el UI thread.
 *
 * El lag del INICIO venía de Movie + LAYER_TYPE_SOFTWARE + postOnAnimation
 * debajo de varios Canvas Compose infinitos.
 */
@Composable
fun GifBackground(modifier: Modifier = Modifier, active: Boolean = true) {
    AndroidView(
        factory = { context ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = false
                    // Hardware por defecto: AnimatedImageDrawable rinde mucho mejor que Movie.
                    val source = ImageDecoder.createSource(context.assets, "background_cyberpunk.gif")
                    val drawable = ImageDecoder.decodeDrawable(source) { decoder, _, _ ->
                        // HARDWARE cuando sea posible: evita el path Movie+SOFTWARE a 60fps.
                        decoder.allocator = ImageDecoder.ALLOCATOR_HARDWARE
                        decoder.isMutableRequired = false
                    }
                    setImageDrawable(drawable)
                    (drawable as? AnimatedImageDrawable)?.let { anim ->
                        anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        if (active) anim.start() else anim.stop()
                        tag = anim
                    }
                }
            } else {
                ThrottledMovieGifView(context).also { it.setActive(active) }
            }
        },
        update = { view ->
            when (view) {
                is ImageView -> {
                    val anim = view.tag as? AnimatedImageDrawable
                    if (active) {
                        view.visibility = View.VISIBLE
                        if (anim != null && !anim.isRunning) anim.start()
                    } else {
                        anim?.stop()
                        view.visibility = View.VISIBLE
                    }
                }
                is ThrottledMovieGifView -> view.setActive(active)
            }
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose { }
    }
}

/** Fallback minSdk 24: Movie a ~12 fps, sin postOnAnimation a 60Hz. */
private class ThrottledMovieGifView(context: android.content.Context) : View(context) {
    private var movie: android.graphics.Movie? = null
    private var startedAt = 0L
    private var active = true
    private val frameDelayMs = 80L

    private val tick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            if (active) invalidate()
            postDelayed(this, frameDelayMs)
        }
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        movie = runCatching {
            context.assets.open("background_cyberpunk.gif").use { android.graphics.Movie.decodeStream(it) }
        }.getOrNull()
    }

    fun setActive(value: Boolean) {
        active = value
        visibility = if (value) VISIBLE else INVISIBLE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startedAt = SystemClock.uptimeMillis()
        postDelayed(tick, frameDelayMs)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        if (!active) return
        val gif = movie ?: return
        if (width <= 0 || height <= 0) return
        val duration = gif.duration().takeIf { it > 0 } ?: 12000
        val elapsed = ((SystemClock.uptimeMillis() - startedAt) % duration).toInt()
        gif.setTime(elapsed)
        val mw = gif.width().toFloat().coerceAtLeast(1f)
        val mh = gif.height().toFloat().coerceAtLeast(1f)
        val scale = maxOf(width / mw, height / mh)
        val drawW = mw * scale
        val drawH = mh * scale
        val left = (width - drawW) * 0.5f
        val top = (height - drawH) * 0.5f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        gif.draw(canvas, 0f, 0f)
        canvas.restore()
    }
}
