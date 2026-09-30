package com.tucolectivo.app.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * FASE 3: sistema centralizado de animaciones cyberpunk.
 *
 * Los componentes visuales futuros reutilizarán estas animaciones
 * en lugar de implementar temporizadores o ciclos independientes.
 *
 * No contiene navegación, datos, lógica funcional ni comportamiento del mapa.
 */
object CyberAnimation {

    const val PulseDuration = 1400
    const val SweepDuration = 2200
    const val GlowDuration = 1800
    const val GlitchDuration = 1600
    const val ScanDuration = 2600
    const val TransitionDuration = 320

    @Composable
    fun pulse(
        durationMillis: Int = PulseDuration,
        min: Float = 0.35f,
        max: Float = 1f
    ): State<Float> {
        val transition = rememberInfiniteTransition()
        return transition.animateFloat(
            initialValue = min,
            targetValue = max,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    @Composable
    fun sweep(
        durationMillis: Int = SweepDuration
    ): State<Float> {
        val transition = rememberInfiniteTransition()
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    @Composable
    fun glow(
        durationMillis: Int = GlowDuration,
        min: Float = 0.55f,
        max: Float = 1f
    ): State<Float> {
        val transition = rememberInfiniteTransition()
        return transition.animateFloat(
            initialValue = min,
            targetValue = max,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    @Composable
    fun glitch(
        amplitude: Float = 2f,
        durationMillis: Int = GlitchDuration
    ): State<Float> {
        val transition = rememberInfiniteTransition()
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = durationMillis
                    0f at 0
                    amplitude at durationMillis / 8
                    -amplitude at durationMillis / 5
                    amplitude * 0.35f at durationMillis / 3
                    0f at durationMillis
                },
                repeatMode = RepeatMode.Restart
            )
        )
    }

    @Composable
    fun scan(
        durationMillis: Int = ScanDuration
    ): State<Float> {
        val transition = rememberInfiniteTransition()
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    @Composable
    fun transition(
        targetValue: Float,
        animationSpec: AnimationSpec<Float> = tween(
            durationMillis = TransitionDuration,
            easing = FastOutSlowInEasing
        )
    ): State<Float> {
        return animateFloatAsState(
            targetValue = targetValue,
            animationSpec = animationSpec
        )
    }
}
