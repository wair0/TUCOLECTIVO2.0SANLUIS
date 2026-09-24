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
                        if (window.__transpuntanoUiFixV8) return;
                        window.__transpuntanoUiFixV8 = true;

                        function isHome() {
                            try {
                                return new URL(location.href).pathname === '/';
                            } catch (e) {
                                return false;
                            }
                        }

                        function removeBase44Badge() {
                            try {
                                var badge = document.getElementById('base44-edit-badge');
                                if (badge) badge.remove();
                            } catch (e) {}
                        }

                        function hideHomeLineCards() {
                            if (!isHome()) return;
                            try {
                                var cards = document.querySelectorAll('a[href^="/lineas/"]');
                                for (var i = 0; i < cards.length; i++) {
                                    cards[i].style.setProperty('display', 'none', 'important');
                                }
                            } catch (e) {}
                        }

                        function hideHomeTagline() {
                            if (!isHome()) return;
                            try {
                                var candidates = document.querySelectorAll('p, span, small, h1, h2, h3, h4');
                                for (var i = 0; i < candidates.length; i++) {
                                    var el = candidates[i];
                                    if (el.children.length > 2) continue;
                                    var t = (el.textContent || '').toLowerCase()
                                        .replace(/[·•,.\-–—]/g, ' ')
                                        .replace(/\s+/g, ' ')
                                        .trim();
                                    if (t.indexOf('transporte urbano') !== -1 &&
                                        t.indexOf('san luis') !== -1 &&
                                        t.indexOf('tiempo real') !== -1) {
                                        el.style.setProperty('display', 'none', 'important');
                                        var p = el.parentElement;
                                        if (p && p !== document.body && p.children.length <= 3) {
                                            p.style.setProperty('display', 'none', 'important');
                                        }
                                        break;
                                    }
                                }
                            } catch (e) {}
                        }

                        function hideHomeLinesSection() {
                            if (!isHome()) return;
                            try {
                                // Ocultar "Ver todo >"
                                var links = document.querySelectorAll('a, span, button, div');
                                for (var i = 0; i < links.length; i++) {
                                    var el = links[i];
                                    var txt = (el.textContent || '').trim();
                                    if (/^ver todo\s*>?$/i.test(txt) && el.children.length === 0) {
                                        var box = el.closest('div') || el;
                                        if (box && !box.closest('nav')) {
                                            box.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                }

                                // Ocultar el título "LÍNEAS" de la sección del Inicio
                                var titles = document.querySelectorAll('h1, h2, h3, h4, span, div');
                                for (var j = 0; j < titles.length; j++) {
                                    var t = titles[j];
                                    var content = (t.textContent || '').trim();
                                    if (/^líneas$/i.test(content) && t.children.length <= 1) {
                                        if (t.closest('nav') || t.closest('[class*="nav"]') || t.closest('[class*="bottom"]')) continue;
                                        var section = t.closest('section, div') || t.parentElement;
                                        if (section && section !== document.body) {
                                            var sTxt = (section.textContent || '').toLowerCase();
                                            if (sTxt.length < 150) {
                                                section.style.setProperty('display', 'none', 'important');
                                            }
                                        }
                                    }
                                }
                            } catch (e) {}
                        }

                        function hideHomeSearch() {
                            if (!isHome()) return;
                            try {
                                var inputs = document.querySelectorAll('input');
                                for (var i = 0; i < inputs.length; i++) {
                                    var ph = (inputs[i].placeholder || '').toLowerCase();
                                    if (ph.indexOf('buscar') !== -1 && ph.indexOf('línea') !== -1) {
                                        var parent = inputs[i].parentElement;
                                        if (parent) parent.style.setProperty('display', 'none', 'important');
                                        else inputs[i].style.setProperty('display', 'none', 'important');
                                    }
                                }
                            } catch (e) {}
                        }

                        var running = false;
                        function applyUiFix() {
                            if (running) return;
                            running = true;
                            try {
                                removeBase44Badge();
                                hideHomeLineCards();
                                hideHomeTagline();
                                hideHomeLinesSection();
                                hideHomeSearch();
                            } catch (e) {}
                            running = false;
                        }

                        // Primera pasada inmediata + pocas más
                        applyUiFix();
                        setTimeout(applyUiFix, 400);
                        setTimeout(applyUiFix, 1200);
                        setTimeout(applyUiFix, 2500);
                        setTimeout(applyUiFix, 4500);

                        // Observer liviano con debounce
                        var timer = null;
                        var observer = new MutationObserver(function() {
                            if (timer) clearTimeout(timer);
                            timer = setTimeout(applyUiFix, 600);
                        });
                        observer.observe(document.documentElement, {
                            childList: true,
                            subtree: true
                        });

                        // Detectar cambio de ruta (SPA)
                        var last = location.href;
                        setInterval(function() {
                            if (location.href !== last) {
                                last = location.href;
                                setTimeout(applyUiFix, 300);
                                setTimeout(applyUiFix, 1000);
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
