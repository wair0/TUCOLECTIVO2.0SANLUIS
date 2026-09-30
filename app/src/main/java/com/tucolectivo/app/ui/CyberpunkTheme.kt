package com.tucolectivo.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * FASE 2: sistema visual base.
 *
 * Centraliza colores, tipografía, formas y estados de controles.
 * No contiene navegación, datos ni lógica funcional.
 */
object CyberColors {
    val Background = Color(0xFF05070D)
    val Surface = Color(0xFF0A0F18)
    val SurfaceVariant = Color(0xFF101827)
    val Primary = Color(0xFF00E5FF)
    val Secondary = Color(0xFFFF00C8)
    val Tertiary = Color(0xFFB7FF00)
    val OnBackground = Color(0xFFE8FBFF)
    val OnSurface = Color(0xFFE8FBFF)
    val Muted = Color(0xFF78909C)
    val Error = Color(0xFFFF4D6D)
    val Border = Color(0xFF164A5A)
}

private val CyberDarkScheme: ColorScheme = darkColorScheme(
    primary = CyberColors.Primary,
    onPrimary = Color(0xFF001014),
    secondary = CyberColors.Secondary,
    onSecondary = Color.White,
    tertiary = CyberColors.Tertiary,
    onTertiary = Color(0xFF101400),
    background = CyberColors.Background,
    onBackground = CyberColors.OnBackground,
    surface = CyberColors.Surface,
    onSurface = CyberColors.OnSurface,
    surfaceVariant = CyberColors.SurfaceVariant,
    onSurfaceVariant = Color(0xFFB8D7DE),
    error = CyberColors.Error,
    onError = Color.White
)

private val CyberShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun CyberpunkTheme(content: @Composable () -> Unit) {
    val cyberFont = rememberCyberpunkFontFamily()

    val baseTypography = MaterialTheme.typography
    val typography = Typography(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = cyberFont, fontWeight = FontWeight.Normal),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = cyberFont, fontWeight = FontWeight.Normal),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = cyberFont, fontWeight = FontWeight.Normal),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = cyberFont),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = cyberFont),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = cyberFont),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = cyberFont),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = cyberFont)
    )

    MaterialTheme(
        colorScheme = CyberDarkScheme,
        typography = typography,
        shapes = CyberShapes,
        content = content
    )
}

@Composable
fun cyberButtonColors() = ButtonDefaults.buttonColors(
    containerColor = CyberColors.SurfaceVariant,
    contentColor = CyberColors.Primary,
    disabledContainerColor = CyberColors.Surface,
    disabledContentColor = CyberColors.Muted
)
