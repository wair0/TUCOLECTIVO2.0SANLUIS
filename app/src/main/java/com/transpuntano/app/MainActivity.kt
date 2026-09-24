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
        }, 2000)

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
                        if (window.__transpuntanoUiFixV7) return;
                        window.__transpuntanoUiFixV7 = true;

                        function isHome() {
                            try {
                                return new URL(location.href).pathname === '/';
                            } catch (e) {
                                return false;
                            }
                        }

                        function removeBase44Badge() {
                            var badge = document.getElementById('base44-edit-badge');
                            if (badge) badge.remove();

                            var base44Nodes = document.querySelectorAll('[id*="base44"], [class*="base44"]');
                            for (var i = 0; i < base44Nodes.length; i++) {
                                var node = base44Nodes[i];
                                if (node.id === 'base44-edit-badge' ||
                                    (node.textContent || '').toLowerCase().indexOf('edit with') !== -1) {
                                    node.remove();
                                }
                            }
                        }

                        function hideHomeLineCards() {
                            if (!isHome()) return;
                            var cards = document.querySelectorAll('a[href^="/lineas/"]');
                            for (var i = 0; i < cards.length; i++) {
                                cards[i].style.setProperty('display', 'none', 'important');
                            }
                        }

                        function hideHomeTagline() {
                            if (!isHome()) return;
                            var nodes = document.querySelectorAll('p, span, small, div, h1, h2, h3, h4');
                            for (var i = 0; i < nodes.length; i++) {
                                var node = nodes[i];
                                if (node.children.length > 3) continue;
                                var text = (node.textContent || '').trim().toLowerCase()
                                    .replace(/[·•,.\-–—]/g, ' ')
                                    .replace(/\s+/g, ' ');
                                if (text.indexOf('transporte urbano') !== -1 &&
                                    text.indexOf('san luis') !== -1 &&
                                    text.indexOf('tiempo real') !== -1) {
                                    node.style.setProperty('display', 'none', 'important');
                                    var parent = node.parentElement;
                                    if (parent && parent !== document.body) {
                                        var pText = (parent.textContent || '').trim().toLowerCase()
                                            .replace(/[·•,.\-–—]/g, ' ')
                                            .replace(/\s+/g, ' ');
                                        if (pText.indexOf('transporte urbano') !== -1 &&
                                            pText.indexOf('tiempo real') !== -1 &&
                                            parent.children.length <= 4) {
                                            parent.style.setProperty('display', 'none', 'important');
                                        }
                                    }
                                }
                            }
                        }

                        function hideHomeLinesSection() {
                            if (!isHome()) return;
                            var all = document.querySelectorAll('div, section, header, h2, h3, span, a, button');
                            for (var i = 0; i < all.length; i++) {
                                var el = all[i];
                                var t = (el.textContent || '').trim();

                                if (/^ver todo\s*>?$/i.test(t) && el.children.length === 0) {
                                    var container = el.closest('div') || el.parentElement;
                                    if (container && !container.closest('nav')) {
                                        container.style.setProperty('display', 'none', 'important');
                                    } else {
                                        el.style.setProperty('display', 'none', 'important');
                                    }
                                }

                                if (/^líneas$/i.test(t) && el.children.length <= 1) {
                                    if (el.closest('nav') || el.closest('[class*="nav"]') || el.closest('[class*="bottom"]')) continue;
                                    var parent = el.parentElement;
                                    if (parent && !parent.closest('nav')) {
                                        var section = parent.closest('section, div') || parent;
                                        if (section && section !== document.body) {
                                            var sText = (section.textContent || '').toLowerCase();
                                            if (sText.indexOf('líneas') !== -1 && sText.length < 120) {
                                                section.style.setProperty('display', 'none', 'important');
                                            } else {
                                                el.style.setProperty('display', 'none', 'important');
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        function hideHomeSearch() {
                            if (!isHome()) return;
                            var inputs = document.querySelectorAll('input, textarea, [contenteditable="true"]');
                            for (var i = 0; i < inputs.length; i++) {
                                var info = (
                                    (inputs[i].placeholder || '') + ' ' +
                                    (inputs[i].getAttribute('aria-label') || '') + ' ' +
                                    (inputs[i].getAttribute('name') || '')
                                ).toLowerCase();
                                if (info.indexOf('buscar línea') !== -1 || info.indexOf('buscar linea') !== -1) {
                                    var parent = inputs[i].parentElement;
                                    if (parent) {
                                        parent.style.setProperty('display', 'none', 'important');
                                    } else {
                                        inputs[i].style.setProperty('display', 'none', 'important');
                                    }
                                }
                            }
                        }

                        function applyUiFix() {
                            try {
                                removeBase44Badge();
                                hideHomeLineCards();
                                hideHomeTagline();
                                hideHomeLinesSection();
                                hideHomeSearch();
                            } catch (e) {
                                console.error('Transpuntano UI fix:', e);
                            }
                        }

                        function startObserver() {
                            if (!document.documentElement) return;
                            if (window.__transpuntanoUiObserver) return;
                            window.__transpuntanoUiObserver = new MutationObserver(function() {
                                applyUiFix();
                            });
                            window.__transpuntanoUiObserver.observe(document.documentElement, {
                                childList: true,
                                subtree: true
                            });
                        }

                        applyUiFix();
                        startObserver();
                        setTimeout(applyUiFix, 100);
                        setTimeout(applyUiFix, 300);
                        setTimeout(applyUiFix, 700);
                        setTimeout(applyUiFix, 1500);
                        setTimeout(applyUiFix, 3000);

                        var lastUrl = location.href;
                        setInterval(function() {
                            if (location.href !== lastUrl) {
                                lastUrl = location.href;
                                setTimeout(applyUiFix, 100);
                                setTimeout(applyUiFix, 500);
                                setTimeout(applyUiFix, 1200);
                            }
                        }, 500);
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
