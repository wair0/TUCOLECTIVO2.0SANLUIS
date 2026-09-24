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
                        if (window.__tucolectivoV13) return;
                        window.__tucolectivoV13 = true;

                        function isHome() {
                            try {
                                return new URL(location.href).pathname === '/';
                            } catch (e) {
                                return false;
                            }
                        }

                        function isNav(el) {
                            return !!(el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]'));
                        }

                        function removeBadge() {
                            try {
                                var b = document.getElementById('base44-edit-badge');
                                if (b) b.remove();
                            } catch (e) {}
                        }

                        function replaceLogo() {
                            if (!isHome()) return;
                            try {
                                var nodes = document.querySelectorAll('h1, h2, h3, div, span');
                                for (var i = 0; i < nodes.length; i++) {
                                    var el = nodes[i];
                                    var t = (el.textContent || '').trim().toUpperCase();
                                    if (t.indexOf('TRANSPUNTANO') !== -1 && el.children.length <= 3) {
                                        el.innerHTML = '<div style="color:#00F0FF;font-weight:bold;letter-spacing:0.06em;">TU COLECTIVO</div><div style="color:#fff;font-weight:bold;font-size:1.7em;">2.0</div>';
                                        break;
                                    }
                                }
                            } catch (e) {}
                        }

                        function cleanHome() {
                            if (!isHome()) return;
                            try {
                                // Tarjetas de líneas
                                document.querySelectorAll('a[href^="/lineas/"]').forEach(function(el) {
                                    el.style.setProperty('display', 'none', 'important');
                                });

                                // Ver todo
                                document.querySelectorAll('a, span, button').forEach(function(el) {
                                    var txt = (el.textContent || '').trim();
                                    if (/^ver todo\s*>?$/i.test(txt) && !isNav(el)) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });

                                // Botones rápidos
                                var labels = ['líneas', 'lineas', 'paradas', 'mapa', 'cercanas', 'favoritos'];
                                document.querySelectorAll('a, button, span, div').forEach(function(el) {
                                    var txt = (el.textContent || '').trim().toLowerCase();
                                    if (labels.indexOf(txt) === -1) return;
                                    if (isNav(el)) return;
                                    if ((el.textContent || '').toUpperCase().indexOf('TU COLECTIVO') !== -1) return;
                                    if ((el.textContent || '').toUpperCase().indexOf('TRANSPUNTANO') !== -1) return;
                                    el.style.setProperty('display', 'none', 'important');
                                });

                                // Título LÍNEAS
                                document.querySelectorAll('h1, h2, h3, span').forEach(function(el) {
                                    var txt = (el.textContent || '').trim();
                                    if (/^líneas$/i.test(txt) && !isNav(el) && el.children.length <= 1) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });

                                // Tagline
                                document.querySelectorAll('p, span, small').forEach(function(el) {
                                    if (el.children.length > 0) return;
                                    var t = (el.textContent || '').toLowerCase();
                                    if (t.indexOf('transporte urbano') !== -1 && t.indexOf('tiempo real') !== -1) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });

                                // Buscador
                                document.querySelectorAll('input').forEach(function(inp) {
                                    var ph = (inp.placeholder || '').toLowerCase();
                                    if (ph.indexOf('buscar') !== -1) {
                                        var p = inp.parentElement;
                                        if (p) p.style.setProperty('display', 'none', 'important');
                                        else inp.style.setProperty('display', 'none', 'important');
                                    }
                                });
                            } catch (e) {}
                        }

                        var running = false;
                        function apply() {
                            if (running) return;
                            running = true;
                            try {
                                removeBadge();
                                replaceLogo();
                                cleanHome();
                            } catch (e) {}
                            running = false;
                        }

                        apply();
                        setTimeout(apply, 600);
                        setTimeout(apply, 1500);
                        setTimeout(apply, 3000);
                        setTimeout(apply, 5000);

                        var t = null;
                        new MutationObserver(function() {
                            if (t) clearTimeout(t);
                            t = setTimeout(apply, 800);
                        }).observe(document.documentElement, { childList: true, subtree: true });

                        var last = location.href;
                        setInterval(function() {
                            if (location.href !== last) {
                                last = location.href;
                                setTimeout(apply, 400);
                                setTimeout(apply, 1200);
                            }
                        }, 1000);
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
