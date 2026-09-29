package com.tucolectivo.app.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/** Fuente cyberpunk.ttf (assets/fonts/cyberpunk.ttf) para toda la UI Compose. */
@Composable
fun rememberCyberpunkFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember {
        runCatching {
            FontFamily(Typeface.createFromAsset(context.assets, "fonts/cyberpunk.ttf"))
        }.getOrElse { FontFamily.Monospace }
    }
}
