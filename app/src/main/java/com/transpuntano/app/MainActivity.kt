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
        }, 2000)

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

                // Tema cyberpunk completo (estilo anterior) - solo CSS, sin MutationObserver
                val cyberJs = """
                    (function() {
                        if (window.__tucolectivoCyber) return;
                        window.__tucolectivoCyber = true;

                        var css = document.createElement('style');
                        css.id = 'tucolectivo-cyber-theme';
                        css.innerHTML = `
                            /* ===== FONDO CYBERPUNK ===== */
                            html, body {
                                background-color: #0D0E15 !important;
                                background-image:
                                    linear-gradient(rgba(0, 240, 255, 0.03) 1px, transparent 1px),
                                    linear-gradient(90deg, rgba(0, 240, 255, 0.03) 1px, transparent 1px) !important;
                                background-size: 40px 40px !important;
                                color: #E0F7FA !important;
                            }

                            /* Header */
                            header, .navbar, .top-bar, nav, [class*="header"], [class*="navbar"] {
                                background: linear-gradient(90deg, #0D0E15 0%, #0a1628 100%) !important;
                                border-bottom: 1px solid rgba(0, 240, 255, 0.25) !important;
                                box-shadow: 0 0 20px rgba(0, 240, 255, 0.1) !important;
                            }

                            /* Títulos */
                            h1, h2, h3, h4, .title, [class*="title"] {
                                color: #00F0FF !important;
                                text-shadow: 0 0 8px rgba(0, 240, 255, 0.4) !important;
                            }

                            /* Cards / Botones principales */
                            .card, [class*="card"], a.btn, button, .btn,
                            [class*="button"], [class*="item"], [class*="list-group"] a,
                            .list-group-item, [class*="rounded"] {
                                background: rgba(13, 20, 35, 0.9) !important;
                                border: 1px solid rgba(0, 240, 255, 0.2) !important;
                                color: #E0F7FA !important;
                                border-radius: 12px !important;
                                box-shadow: 0 0 12px rgba(0, 240, 255, 0.08) !important;
                            }

                            a, button {
                                color: #00F0FF !important;
                            }

                            a:hover, button:hover, a:active, button:active {
                                background: rgba(0, 240, 255, 0.12) !important;
                                border-color: #00F0FF !important;
                                box-shadow: 0 0 16px rgba(0, 240, 255, 0.25) !important;
                            }

                            /* Inputs */
                            input, select, textarea {
                                background: #0D0E15 !important;
                                border: 1px solid rgba(0, 240, 255, 0.35) !important;
                                color: #E0F7FA !important;
                                border-radius: 8px !important;
                            }

                            input::placeholder {
                                color: rgba(0, 240, 255, 0.5) !important;
                            }

                            /* Links de líneas */
                            a[href*="codLinea"], a[href*="lineas"], a[href*="calles"],
                            a[href*="paradas"], a[href*="arribos"] {
                                background: rgba(10, 22, 40, 0.95) !important;
                                border: 1px solid rgba(0, 240, 255, 0.25) !important;
                                color: #00F0FF !important;
                                margin: 6px 0 !important;
                                padding: 12px 16px !important;
                                display: block !important;
                                border-radius: 10px !important;
                            }

                            /* Footer / barras inferiores */
                            footer, .footer, [class*="footer"] {
                                background: #0D0E15 !important;
                                border-top: 1px solid rgba(0, 240, 255, 0.2) !important;
                            }

                            /* Scrollbar */
                            ::-webkit-scrollbar {
                                width: 6px;
                            }
                            ::-webkit-scrollbar-track {
                                background: #0D0E15;
                            }
                            ::-webkit-scrollbar-thumb {
                                background: rgba(0, 240, 255, 0.4);
                                border-radius: 3px;
                            }

                            /* Textos generales */
                            p, span, label, li, td, th {
                                color: #B0BEC5 !important;
                            }

                            /* Quitar fondos blancos residuales */
                            .container, .content, main, section, .wrapper {
                                background: transparent !important;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(css);
                    })();
                """.trimIndent()

                view?.evaluateJavascript(cyberJs, null)
            }
        }

        webView.loadUrl(HOME_URL)
    }

    private fun startSplashAnimation() {
        val title = findViewById<android.widget.TextView>(R.id.splashTitle)
        splashAnimator = ObjectAnimator.ofFloat(title, View.ALPHA, 1f, 0.2f, 1f).apply {
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
