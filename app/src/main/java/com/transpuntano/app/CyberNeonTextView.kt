package com.transpuntano.app

import android.content.Context
import android.view.View
import android.widget.TextView

/** Texto con glow neon cyan. */
class CyberNeonTextView(context: Context) : TextView(context) {
    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setShadowLayer(8f, 0f, 0f, 0xCC00F0FF.toInt())
    }
}
