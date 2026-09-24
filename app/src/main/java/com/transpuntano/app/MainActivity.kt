package com.tucolectivo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashLayout: RelativeLayout
    private var isSplashHidden = false
    private var splashAnimator: ObjectAnimator? = null

    companion object {
        private const val HOME_URL = "https://cuandollega.smartmovepro.net/transpuntano"
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splashLayout = findViewById(R.id.splashLayout)

        webView.visibility = View.VISIBLE
        startSplashAnimation()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setGeolocationEnabled(true)
            builtInZoomControls = false
            displayZoomControls = false
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            hideSplash()
        }, 2200)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && (url.contains("cuandollega.smartmovepro.net") || url.contains("smartmovepro.net"))) {
                    view?.loadUrl(url)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideSplash()
                applyCyberpunkTheme(view)
            }
        }

        webView.loadUrl(HOME_URL)
    }

    private fun applyCyberpunkTheme(view: WebView?) {
        val cyberJs = """
            (function() {
                try {
                    var styleId = 'tucolectivo-cyberpunk-v16';
                    var existing = document.getElementById(styleId);

                    if (!existing) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.type = 'text/css';
                        style.textContent = `
                            :root {
                                --tc-cyan: #00f0ff;
                                --tc-cyan-soft: rgba(0,240,255,.22);
                                --tc-cyan-faint: rgba(0,240,255,.08);
                                --tc-blue: #0875b9;
                                --tc-bg: #070b14;
                                --tc-panel: rgba(10,18,31,.88);
                                --tc-panel-2: rgba(13,23,40,.94);
                                --tc-text: #f1f7ff;
                                --tc-muted: #718096;
                            }

                            @keyframes tcGridMove {
                                0% { background-position: 0 0, 0 0; }
                                100% { background-position: 40px 40px, 40px 40px; }
                            }

                            @keyframes tcScan {
                                0% { transform: translateY(-100%); opacity: 0; }
                                12% { opacity: .35; }
                                50% { opacity: .16; }
                                88% { opacity: .35; }
                                100% { transform: translateY(100vh); opacity: 0; }
                            }

                            @keyframes tcPulse {
                                0%,100% { box-shadow: 0 0 8px rgba(0,240,255,.10), inset 0 0 8px rgba(0,240,255,.03); }
                                50% { box-shadow: 0 0 20px rgba(0,240,255,.24), inset 0 0 14px rgba(0,240,255,.06); }
                            }

                            @keyframes tcGlow {
                                0%,100% { filter: drop-shadow(0 0 2px rgba(0,240,255,.35)); }
                                50% { filter: drop-shadow(0 0 7px rgba(0,240,255,.75)); }
                            }

                            @keyframes tcShimmer {
                                0% { transform: translateX(-120%); }
                                100% { transform: translateX(120%); }
                            }

                            @keyframes tcFloat {
                                0%,100% { transform: translateY(0); }
                                50% { transform: translateY(-2px); }
                            }

                            html, body {
                                background-color: var(--tc-bg) !important;
                                color: var(--tc-text) !important;
                                min-height: 100% !important;
                            }

                            body {
                                position: relative !important;
                                overflow-x: hidden !important;
                                background-image:
                                    linear-gradient(rgba(0,240,255,.035) 1px, transparent 1px),
                                    linear-gradient(90deg, rgba(0,240,255,.035) 1px, transparent 1px),
                                    radial-gradient(circle at 50% -10%, rgba(0,153,255,.14), transparent 48%) !important;
                                background-size: 40px 40px, 40px 40px, 100% 100% !important;
                                animation: tcGridMove 18s linear infinite !important;
                            }

                            body::before {
                                content: '' !important;
                                position: fixed !important;
                                inset: 0 !important;
                                pointer-events: none !important;
                                z-index: 2147483000 !important;
                                background: linear-gradient(to bottom, transparent 0%, rgba(0,240,255,.06) 50%, transparent 100%) !important;
                                height: 28vh !important;
                                animation: tcScan 8s linear infinite !important;
                            }

                            body::after {
                                content: '' !important;
                                position: fixed !important;
                                inset: 0 !important;
                                pointer-events: none !important;
                                z-index: 2147482999 !important;
                                box-shadow: inset 0 0 80px rgba(0,0,0,.55) !important;
                            }

                            body, button, input, textarea, select {
                                -webkit-font-smoothing: antialiased !important;
                            }

                            header,
                            [class*="header"],
                            [class*="navbar"],
                            [class*="topbar"],
                            [class*="top-bar"] {
                                background: linear-gradient(180deg, rgba(6,12,23,.98), rgba(7,15,28,.92)) !important;
                                border-bottom: 1px solid rgba(0,240,255,.28) !important;
                                box-shadow: 0 0 24px rgba(0,240,255,.12) !important;
                                backdrop-filter: blur(12px) !important;
                                -webkit-backdrop-filter: blur(12px) !important;
                            }

                            main, section, .container, .content, .wrapper {
                                background: transparent !important;
                            }

                            h1, h2, h3, h4 {
                                color: var(--tc-text) !important;
                                text-shadow: 0 0 10px rgba(0,240,255,.18) !important;
                            }

                            a {
                                text-decoration: none !important;
                            }

                            /* Buscador */
                            input[type="search"],
                            input[placeholder*="Buscar"],
                            input[placeholder*="buscar"],
                            input {
                                background: rgba(12,22,38,.86) !important;
                                color: var(--tc-text) !important;
                                border: 1px solid rgba(0,240,255,.30) !important;
                                border-radius: 18px !important;
                                box-shadow: inset 0 0 12px rgba(0,240,255,.035), 0 0 12px rgba(0,240,255,.06) !important;
                                transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease !important;
                            }

                            input:focus {
                                outline: none !important;
                                border-color: var(--tc-cyan) !important;
                                box-shadow: 0 0 18px rgba(0,240,255,.22), inset 0 0 14px rgba(0,240,255,.05) !important;
                            }

                            input::placeholder {
                                color: #65748a !important;
                            }

                            /* Acciones rápidas */
                            button,
                            [role="button"],
                            a[class*="button"],
                            a[class*="btn"] {
                                background: linear-gradient(180deg, rgba(14,27,45,.94), rgba(8,17,30,.94)) !important;
                                color: var(--tc-text) !important;
                                border: 1px solid rgba(0,240,255,.25) !important;
                                box-shadow: 0 0 10px rgba(0,240,255,.06) !important;
                                transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease, background .18s ease !important;
                            }

                            button:hover,
                            [role="button"]:hover,
                            a[class*="button"]:hover,
                            a[class*="btn"]:hover {
                                transform: translateY(-1px) !important;
                                border-color: var(--tc-cyan) !important;
                                background: linear-gradient(180deg, rgba(0,240,255,.14), rgba(7,20,35,.96)) !important;
                                box-shadow: 0 0 18px rgba(0,240,255,.20) !important;
                            }

                            /* Tarjetas y filas */
                            main a,
                            main button,
                            [class*="card"],
                            [class*="Card"],
                            [class*="list-item"],
                            [class*="ListItem"] {
                                position: relative !important;
                                background: linear-gradient(135deg, rgba(9,19,33,.94), rgba(8,14,25,.90)) !important;
                                color: var(--tc-text) !important;
                                border: 1px solid rgba(0,240,255,.20) !important;
                                border-radius: 15px !important;
                                box-shadow: 0 0 12px rgba(0,240,255,.055), inset 0 0 18px rgba(0,240,255,.018) !important;
                                overflow: hidden !important;
                            }

                            main a::after,
                            main button::after,
                            [class*="card"]::after,
                            [class*="Card"]::after {
                                content: '' !important;
                                position: absolute !important;
                                left: -120% !important;
                                top: 0 !important;
                                width: 45% !important;
                                height: 100% !important;
                                background: linear-gradient(90deg, transparent, rgba(0,240,255,.10), transparent) !important;
                                transform: skewX(-18deg) !important;
                                pointer-events: none !important;
                            }

                            main a:hover::after,
                            main button:hover::after,
                            [class*="card"]:hover::after,
                            [class*="Card"]:hover::after {
                                animation: tcShimmer .7s ease-out !important;
                            }

                            /* Enlaces de líneas/paradas */
                            a[href*="/lineas/"],
                            a[href*="/paradas/"],
                            a[href*="/mapa"],
                            a[href*="/cercanas"],
                            a[href*="/favoritos"] {
                                border-color: rgba(0,240,255,.18) !important;
                                background: linear-gradient(135deg, rgba(8,19,33,.96), rgba(7,13,24,.94)) !important;
                            }

                            a[href*="/lineas/"] {
                                animation: tcPulse 4s ease-in-out infinite !important;
                            }

                            a[href*="/lineas/"] svg,
                            a[href*="/paradas/"] svg,
                            a[href*="/mapa"] svg,
                            a[href*="/cercanas"] svg,
                            a[href*="/favoritos"] svg,
                            nav svg {
                                color: var(--tc-cyan) !important;
                                stroke: currentColor !important;
                                filter: drop-shadow(0 0 4px rgba(0,240,255,.38)) !important;
                            }

                            /* Textos */
                            p, span, label, li, td, th {
                                color: #aeb9c9 !important;
                            }

                            a, button, [role="button"] {
                                color: var(--tc-text) !important;
                            }

                            /* Valores destacados */
                            [class*="font-bold"],
                            [class*="font-semibold"],
                            strong, b {
                                color: #f5fbff !important;
                                text-shadow: 0 0 7px rgba(0,240,255,.13) !important;
                            }

                            /* Barra inferior */
                            nav[class*="fixed"],
                            [class*="bottom-0"],
                            [class*="bottom-"] {
                                background: rgba(6,12,22,.96) !important;
                                border-top: 1px solid rgba(0,240,255,.25) !important;
                                box-shadow: 0 -8px 28px rgba(0,0,0,.45), 0 0 18px rgba(0,240,255,.06) !important;
                                backdrop-filter: blur(14px) !important;
                                -webkit-backdrop-filter: blur(14px) !important;
                            }

                            nav a,
                            nav button {
                                background: transparent !important;
                                border: 0 !important;
                                box-shadow: none !important;
                            }

                            nav a:hover,
                            nav button:hover {
                                transform: none !important;
                                background: rgba(0,240,255,.06) !important;
                            }

                            /* Elemento activo de navegación */
                            nav [aria-current="page"],
                            nav [class*="active"],
                            nav .active {
                                color: var(--tc-cyan) !important;
                                text-shadow: 0 0 9px rgba(0,240,255,.55) !important;
                            }

                            nav [aria-current="page"] svg,
                            nav [class*="active"] svg,
                            nav .active svg {
                                animation: tcGlow 1.8s ease-in-out infinite !important;
                            }

                            /* Quitar cualquier marca Base44 si reaparece */
                            #base44-edit-badge,
                            [id*="base44"],
                            [class*="base44"],
                            [href*="base44"] {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }

                            ::-webkit-scrollbar {
                                width: 5px !important;
                            }

                            ::-webkit-scrollbar-track {
                                background: #070b14 !important;
                            }

                            ::-webkit-scrollbar-thumb {
                                background: rgba(0,240,255,.38) !important;
                                border-radius: 8px !important;
                                box-shadow: 0 0 8px rgba(0,240,255,.35) !important;
                            }

                            ::selection {
                                background: rgba(0,240,255,.25) !important;
                                color: #fff !important;
                            }

                            @media (prefers-reduced-motion: reduce) {
                                *, *::before, *::after {
                                    animation-duration: .001ms !important;
                                    animation-iteration-count: 1 !important;
                                    transition-duration: .001ms !important;
                                }
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);
                    }

                    document.documentElement.classList.add('tucolectivo-cyberpunk');
                    if (document.body) document.body.classList.add('tucolectivo-cyberpunk');

                    /* Reaplicación ligera para SPA: no usa MutationObserver ni intervalos */
                    setTimeout(function() {
                        document.documentElement.classList.add('tucolectivo-cyberpunk');
                        if (document.body) document.body.classList.add('tucolectivo-cyberpunk');
                    }, 350);
                    setTimeout(function() {
                        if (document.body) document.body.classList.add('tucolectivo-cyberpunk');
                    }, 1200);
                } catch (e) {
                    console.log('Cyberpunk theme error', e);
                }
            })();
        """.trimIndent()

        view?.evaluateJavascript(cyberJs, null)
    }

    private fun startSplashAnimation() {
        val title = findViewById<android.widget.TextView>(R.id.splashTitle)
        splashAnimator = ObjectAnimator.ofFloat(title, View.ALPHA, 1f, 0.22f, 1f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun hideSplash() {
        splashAnimator?.cancel()
        splashAnimator = null
        if (!isSplashHidden) {
            isSplashHidden = true
            if (::webView.isInitialized) webView.visibility = View.VISIBLE
            if (::splashLayout.isInitialized) {
                splashLayout.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { splashLayout.visibility = View.GONE }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
