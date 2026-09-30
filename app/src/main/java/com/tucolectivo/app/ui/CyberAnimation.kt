package com.tucolectivo.app.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.graphicsLayer

/**
 * FASE 3: sistema centralizado de animaciones cyberpunk.
 *
 * Los componentes visuales futuros deben reutilizar estas animaciones
 * en lugar de crear temporizadores o transiciones independientes.
 *
 * Este sistema no contiene navegación, datos, lógica funcional ni
 * comportamiento del mapa.
 */
object CyberAnimation {

    const val PulseDuration = 1400
    const val SweepDuration = 2200
    const val GlowDuration = 1800
    const val GlitchDuration = 1600
    const val ScanDuration = 2600
    const val TransitionDuration = 320

    private const val MinPulse = 0.35f
    private const val MaxPulse = 1f

    /**
     * Pulso continuo para intensidad, alpha o escala.
     */
    @Composable
    fun pulse(
        durationMillis: Int = PulseDuration,
        min: Float = MinPulse,
        max: Float = MaxPulse
    ): State<Float> {
        val transition = rememberInfiniteTransition(label = "cyber_pulse")
        return transition.animateFloat(
            initialValue = min,
            targetValue = max,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cyber_pulse_value"
        )
    }

    /**
     * Barrido lineal de 0 a 1 para reflejos y bordes.
     */
    @Composable
    fun sweep(
        durationMillis: Int = SweepDuration
    ): State<Float> {
        val transition = rememberInfiniteTransition(label = "cyber_sweep")
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cyber_sweep_value"
        )
    }

    /**
     * Variación de brillo centrada alrededor de 1f.
     */
    @Composable
    fun glow(
        durationMillis: Int = GlowDuration,
        min: Float = 0.55f,
        max: Float = 1f
    ): State<Float> {
        val transition = rememberInfiniteTransition(label = "cyber_glow")
        return transition.animateFloat(
            initialValue = min,
            targetValue = max,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cyber_glow_value"
        )
    }

    /**
     * Microdesplazamiento para efectos glitch.
     *
     * El desplazamiento es pequeño deliberadamente para evitar
     * movimientos bruscos en elementos interactivos.
     */
    @Composable
    fun glitch(
        amplitude: Float = 2f,
        durationMillis: Int = GlitchDuration
    ): State<Float> {
        val transition = rememberInfiniteTransition(label = "cyber_glitch")
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = amplitude,
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
            ),
            label = "cyber_glitch_value"
        )
    }

    /**
     * Posición normalizada de una línea de escaneo.
     */
    @Composable
    fun scan(
        durationMillis: Int = ScanDuration
    ): State<Float> {
        val transition = rememberInfiniteTransition(label = "cyber_scan")
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cyber_scan_value"
        )
    }

    /**
     * Transición puntual para cambios de estado de componentes.
     */
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
            animationSpec = animationSpec,
            label = "cyber_transition"
        )
    }

    /**
     * Desplazamiento glitch reutilizable para graphicsLayer.
     */
    fun graphicsLayerGlitch(
        value: Float
    ): androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        translationX = value
    }
}
