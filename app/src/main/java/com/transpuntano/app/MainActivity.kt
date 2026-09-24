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
                        if (window.__transpuntanoUiFixV12) return;
                        window.__transpuntanoUiFixV12 = true;

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

                        function replaceHomeLogo() {
                            if (!isHome()) return;
                            try {
                                var nodes = document.querySelectorAll('h1, h2, h3, span, div, p');
                                for (var i = 0; i < nodes.length; i++) {
                                    var el = nodes[i];
                                    var t = (el.textContent || '').trim().toUpperCase();
                                    if (t.indexOf('TRANSPUNTANO') !== -1 && el.children.length <= 4) {
                                        // Reemplazar por TU COLECTIVO 2.0
                                        if (t.indexOf('2.0') !== -1 || t === 'TRANSPUNTANO') {
                                            el.innerHTML = '<div style="color:#00F0FF;font-weight:bold;letter-spacing:0.08em;font-size:1.1em;">TU COLECTIVO</div><div style="color:#FFFFFF;font-weight:bold;font-size:1.8em;line-height:1.1;">2.0</div>';
                                            break;
                                        }
                                    }
                                }
                            } catch (e) {}
                        }

                        function cleanHome() {
                            if (!isHome()) return;
                            try {
                                document.querySelectorAll('a[href^="/lineas/"]').forEach(function(el) {
                                    el.style.setProperty('display', 'none', 'important');
                                });

                                document.querySelectorAll('*').forEach(function(el) {
                                    var txt = (el.textContent || '').trim();
                                    if (/^ver todo\s*>?$/i.test(txt)) {
                                        if (el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]')) return;
                                        el.style.setProperty('display', 'none', 'important');
                                        var p = el.parentElement;
                                        if (p && p.children.length <= 3) {
                                            p.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                });

                                var quickLabels = ['líneas', 'lineas', 'paradas', 'mapa', 'cercanas', 'favoritos'];
                                document.querySelectorAll('a, button, div, span').forEach(function(el) {
                                    var txt = (el.textContent || '').trim().toLowerCase();
                                    if (quickLabels.indexOf(txt) !== -1) {
                                        if (el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]')) return;
                                        if ((el.textContent || '').toUpperCase().indexOf('TRANSPUNTANO') !== -1) return;
                                        if ((el.textContent || '').toUpperCase().indexOf('TU COLECTIVO') !== -1) return;
                                        el.style.setProperty('display', 'none', 'important');
                                        var p = el.parentElement;
                                        if (p && p.children.length <= 4 && !(p.textContent || '').toUpperCase().includes('TRANSPUNTANO') && !(p.textContent || '').toUpperCase().includes('TU COLECTIVO')) {
                                            p.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                });

                                document.querySelectorAll('h1, h2, h3, h4, span, div, p').forEach(function(el) {
                                    var txt = (el.textContent || '').trim();
                                    if (/^líneas$/i.test(txt) && el.children.length <= 1) {
                                        if (el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]')) return;
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });

                                document.querySelectorAll('p, span, small').forEach(function(el) {
                                    if (el.children.length > 0) return;
                                    var t = (el.textContent || '').toLowerCase()
                                        .replace(/[·•,.\-–—]/g, ' ')
                                        .replace(/\s+/g, ' ')
                                        .trim();
                                    if (t.indexOf('transporte urbano') !== -1 && t.indexOf('san luis') !== -1 && t.indexOf('tiempo real') !== -1) {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                });

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
                                replaceHomeLogo();
                                cleanHome();
                            } catch (e) {}
                            running = false;
                        }

                        applyUiFix();
                        setTimeout(applyUiFix, 400);
                        setTimeout(applyUiFix, 1000);
                        setTimeout(applyUiFix, 2000);
                        setTimeout(applyUiFix, 3500);
                        setTimeout(applyUiFix, 5500);

                        var timer = null;
                        var observer = new MutationObserver(function() {
                            if (timer) clearTimeout(timer);
                            timer = setTimeout(applyUiFix, 600);
                        });
                        observer.observe(document.documentElement, {
                            childList: true,
                            subtree: true
                        });

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
