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
                        if (window.__transpuntanoUiFixV9) return;
                        window.__transpuntanoUiFixV9 = true;

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

                        function hideHomeLineCards() {
                            if (!isHome()) return;
                            try {
                                document.querySelectorAll('a[href^="/lineas/"]').forEach(function(el) {
                                    el.style.setProperty('display', 'none', 'important');
                                });
                            } catch (e) {}
                        }

                        function hideHomeTagline() {
                            if (!isHome()) return;
                            try {
                                // Solo textos pequeños que contengan exactamente la frase del subtítulo
                                var nodes = document.querySelectorAll('p, span, small');
                                for (var i = 0; i < nodes.length; i++) {
                                    var el = nodes[i];
                                    if (el.children.length > 0) continue;
                                    var t = (el.textContent || '').toLowerCase()
                                        .replace(/[·•,.\-–—]/g, ' ')
                                        .replace(/\s+/g, ' ')
                                        .trim();
                                    if (t.indexOf('transporte urbano') !== -1 &&
                                        t.indexOf('san luis') !== -1 &&
                                        t.indexOf('tiempo real') !== -1) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                }
                            } catch (e) {}
                        }

                        function hideVerTodo() {
                            if (!isHome()) return;
                            try {
                                var all = document.querySelectorAll('a, span, button, div, p');
                                for (var i = 0; i < all.length; i++) {
                                    var el = all[i];
                                    var txt = (el.textContent || '').trim();
                                    // Solo el texto exacto "Ver todo >" o "Ver todo"
                                    if (/^ver todo\s*>?$/i.test(txt) && el.children.length === 0) {
                                        el.style.setProperty('display', 'none', 'important');
                                        // también el contenedor inmediato si es pequeño
                                        var p = el.parentElement;
                                        if (p && p.children.length <= 2 && !(p.textContent || '').toLowerCase().includes('transpuntano')) {
                                            p.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                }
                            } catch (e) {}
                        }

                        function hideHomeSearch() {
                            if (!isHome()) return;
                            try {
                                document.querySelectorAll('input').forEach(function(inp) {
                                    var ph = (inp.placeholder || '').toLowerCase();
                                    if (ph.indexOf('buscar') !== -1 && (ph.indexOf('línea') !== -1 || ph.indexOf('linea') !== -1)) {
                                        var parent = inp.parentElement;
                                        if (parent) parent.style.setProperty('display', 'none', 'important');
                                        else inp.style.setProperty('display', 'none', 'important');
                                    }
                                });
                            } catch (e) {}
                        }

                        // Protección: nunca ocultar elementos que contengan "TRANSPUNTANO" o "2.0"
                        function isProtected(el) {
                            var t = (el.textContent || '').toUpperCase();
                            return t.indexOf('TRANSPUNTANO') !== -1 || t.indexOf('2.0') !== -1;
                        }

                        var running = false;
                        function applyUiFix() {
                            if (running) return;
                            running = true;
                            try {
                                removeBase44Badge();
                                hideHomeLineCards();
                                hideHomeTagline();
                                hideVerTodo();
                                hideHomeSearch();
                            } catch (e) {}
                            running = false;
                        }

                        applyUiFix();
                        setTimeout(applyUiFix, 500);
                        setTimeout(applyUiFix, 1500);
                        setTimeout(applyUiFix, 3000);
                        setTimeout(applyUiFix, 5000);

                        var timer = null;
                        var observer = new MutationObserver(function() {
                            if (timer) clearTimeout(timer);
                            timer = setTimeout(applyUiFix, 700);
                        });
                        observer.observe(document.documentElement, { childList: true, subtree: true });

                        var last = location.href;
                        setInterval(function() {
                            if (location.href !== last) {
                                last = location.href;
                                setTimeout(applyUiFix, 400);
                                setTimeout(applyUiFix, 1200);
                            }
                        }, 900);
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
