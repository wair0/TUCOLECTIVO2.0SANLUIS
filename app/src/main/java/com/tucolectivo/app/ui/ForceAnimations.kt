package com.tucolectivo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Fuerza ValueAnimator duration scale = 1.0.
 * Si el dispositivo tiene "Escala de duración del animador" en 0
 * (Opciones de desarrollador), rememberInfiniteTransition se congela.
 * El reloj vsync (rememberFrameTimeSec) no depende de esto, pero
 * Animatable/tween sí. Ejecutar una vez al montar el shell.
 */
@Composable
fun EnsureAnimationsEnabled() {
    LaunchedEffect(Unit) {
        runCatching {
            val clazz = Class.forName("android.animation.ValueAnimator")
            val method = clazz.getMethod("setDurationScale", Float::class.javaPrimitiveType)
            method.invoke(null, 1.0f)
        }
    }
}
