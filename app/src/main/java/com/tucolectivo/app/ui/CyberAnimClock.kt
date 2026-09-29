package com.tucolectivo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis

/**
 * Reloj de animación basado en frames del vsync.
 * NO depende de ValueAnimator ni de "Animator duration scale" del sistema.
 * Si el usuario tiene animaciones desactivadas en Opciones de desarrollador,
 * rememberInfiniteTransition se congela; este reloj sigue corriendo.
 */
@Composable
fun rememberFrameTimeSec(): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                t.floatValue = (now - start) / 1000f
            }
        }
    }
    return t
}

/** Progreso 0..1 en un ciclo de [periodSec] segundos (lineal, loop). */
fun cycle01(timeSec: Float, periodSec: Float): Float {
    if (periodSec <= 0f) return 0f
    val x = timeSec / periodSec
    return x - x.toInt()
}

/** Ida y vuelta 0..1..0 (triángulo) en [periodSec] segundos. */
fun pulse01(timeSec: Float, periodSec: Float): Float {
    val c = cycle01(timeSec, periodSec)
    return if (c < 0.5f) c * 2f else (1f - c) * 2f
}
