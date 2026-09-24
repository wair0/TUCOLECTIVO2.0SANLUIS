package com.transpuntano.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashLayout: RelativeLayout
    private var isSplashHidden = false
    private var splashAnimator: ObjectAnimator? = null

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
        }

        webView.webChromeClient = WebChromeClient()

        Handler(Looper.getMainLooper()).postDelayed({
            hideSplash()
        }, 2200)

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                if (url != null && url.contains("/static/js/badge.js")) {
                    return WebResourceResponse(
                        "application/javascript",
                        "UTF-8",
                        ByteArrayInputStream("".toByteArray())
                    )
                }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideSplash()

                val cyberUiJs = """
                    (function() {
                        if (window.__transpuntanoUiFixV10) return;
                        window.__transpuntanoUiFixV10 = true;

                        function isHome() {
                            try {
                                return new URL(location.href).pathname === '/';
                            } catch (e) {
                                return false;
                            }
                        }

                        function removeBase44Badge() {
                            try {
                                var b = document.getElementById('base44-edit-badge');
                                if (b) b.remove();
                            } catch (e) {}
                        }

                        function cleanHome() {
                            if (!isHome()) return;

                            try {
                                // 1. Ocultar tarjetas de líneas
                                document.querySelectorAll('a[href^="/lineas/"]').forEach(function(el) {
                                    el.style.setProperty('display', 'none', 'important');
                                });

                                // 2. Ocultar "Ver todo >"
                                document.querySelectorAll('a, span, button, div, p').forEach(function(el) {
                                    var txt = (el.textContent || '').trim();
                                    if (/^ver todo\s*>?$/i.test(txt) && el.children.length === 0) {
                                        el.style.setProperty('display', 'none', 'important');
                                        var p = el.parentElement;
                                        if (p && p.children.length <= 2) {
                                            p.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                });

                                // 3. Ocultar el título de sección "LÍNEAS"
                                document.querySelectorAll('h1, h2, h3, h4, span, div, p').forEach(function(el) {
                                    var txt = (el.textContent || '').trim();
                                    if (/^líneas$/i.test(txt) && el.children.length <= 1) {
                                        // No tocar la barra de navegación inferior
                                        if (el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]')) return;
                                        el.style.setProperty('display', 'none', 'important');
                                        var p = el.parentElement;
                                        if (p && p.children.length <= 3) {
                                            p.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                });

                                // 4. Ocultar los botones rápidos (Líneas, Paradas, Mapa, Cercanas)
                                // Estos botones suelen estar en un contenedor horizontal debajo del logo
                                document.querySelectorAll('a, button, div').forEach(function(el) {
                                    var txt = (el.textContent || '').trim().toLowerCase();
                                    // Solo textos cortos exactos de los botones
                                    if (txt === 'líneas' || txt === 'lineas' ||
                                        txt === 'paradas' ||
                                        txt === 'mapa' ||
                                        txt === 'cercanas') {
                                        // No tocar la navegación inferior
                                        if (el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]')) return;
                                        // Ocultar el botón o su contenedor inmediato
                                        var target = el.closest('a, button, div') || el;
                                        if (target && !target.textContent.toUpperCase().includes('TRANSPUNTANO')) {
                                            target.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                });

                                // 5. Ocultar tagline "Transporte urbano de San Luis..."
                                document.querySelectorAll('p, span, small').forEach(function(el) {
                                    if (el.children.length > 0) return;
                                    var t = (el.textContent || '').toLowerCase()
                                        .replace(/[·•,.\-–—]/g, ' ')
                                        .replace(/\s+/g, ' ')
                                        .trim();
                                    if (t.indexOf('transporte urbano') !== -1 &&
                                        t.indexOf('san luis') !== -1 &&
                                        t.indexOf('tiempo real') !== -1) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });

                                // 6. Ocultar cualquier input de búsqueda en Inicio
                                document.querySelectorAll('input').forEach(function(inp) {
                                    var ph = (inp.placeholder || '').toLowerCase();
                                    if (ph.indexOf('buscar') !== -1) {
                                        var parent = inp.parentElement;
                                        if (parent) parent.style.setProperty('display', 'none', 'important');
                                        else inp.style.setProperty('display', 'none', 'important');
                                    }
                                });

                            } catch (e) {
                                console.error('cleanHome error', e);
                            }
                        }

                        var running = false;
                        function applyUiFix() {
                            if (running) return;
                            running = true;
                            try {
                                removeBase44Badge();
                                cleanHome();
                            } catch (e) {}
                            running = false;
                        }

                        // Ejecuciones escalonadas
                        applyUiFix();
                        setTimeout(applyUiFix, 400);
                        setTimeout(applyUiFix, 1000);
                        setTimeout(applyUiFix, 2000);
                        setTimeout(applyUiFix, 3500);
                        setTimeout(applyUiFix, 5500);

                        // Observer con debounce
                        var timer = null;
                        var observer = new MutationObserver(function() {
                            if (timer) clearTimeout(timer);
                            timer = setTimeout(applyUiFix, 600);
                        });
                        observer.observe(document.documentElement, {
                            childList: true,
                            subtree: true
                        });

                        // Detectar navegación SPA
                        var last = location.href;
                        setInterval(function() {
                            if (location.href !== last) {
                                last = location.href;
                                setTimeout(applyUiFix, 300);
                                setTimeout(applyUiFix, 900);
                            }
                        }, 800);
                    })();
                """.trimIndent()

                view?.evaluateJavascript(cyberUiJs, null)
            }
        }

        webView.loadUrl("https://trans-puntano-go.base44.app/")
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
