package com.transpuntano.app

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashLayout: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splashLayout = findViewById(R.id.splashLayout)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val cyberpunkJs = """
                    (function() {
                        if (!document.getElementById('cyber-ring-keyframes')) {
                            var style = document.createElement('style');
                            style.id = 'cyber-ring-keyframes';
                            style.innerHTML = `
                                @keyframes cyberFill {
                                    0% { stroke-dashoffset: 195; }
                                    100% { stroke-dashoffset: 0; }
                                }
                                a[href*="base44"], [class*="base44"], div[style*="fixed"][style*="bottom"] {
                                    display: none !important;
                                    visibility: hidden !important;
                                    opacity: 0 !important;
                                }
                            `;
                            (document.head || document.documentElement).appendChild(style);
                        }

                        function applyCyberRings() {
                            // 1. Ocultar insignia flotante de Base 44
                            var baseElems = document.querySelectorAll('div, a, span, p, button');
                            baseElems.forEach(function(el) {
                                if (el.textContent && el.textContent.includes('Edit with Base 44')) {
                                    var target = el;
                                    while (target.parentElement && target.parentElement !== document.body) {
                                        var stylePos = window.getComputedStyle(target).position;
                                        if (stylePos === 'fixed' || stylePos === 'absolute') break;
                                        target = target.parentElement;
                                    }
                                    target.style.setProperty('display', 'none', 'important');
                                }
                            });

                            // 2. Buscar únicamente contenedores específicos con el formato "XX MIN"
                            var all = document.querySelectorAll('*');
                            all.forEach(function(el) {
                                var txt = (el.innerText || el.textContent || '').trim().replace(/\s+/g, ' ');
                                if (/^\d+\s*MIN$/i.test(txt)) {
                                    var hasMatchingChild = Array.from(el.children).some(function(child) {
                                        return /^\d+\s*MIN$/i.test((child.innerText || child.textContent || '').trim().replace(/\s+/g, ' '));
                                    });

                                    if (!hasMatchingChild) {
                                        if (el.parentElement) {
                                            el.parentElement.style.overflow = 'visible';
                                        }

                                        el.style.position = 'relative';
                                        el.style.display = 'inline-flex';
                                        el.style.flexDirection = 'column';
                                        el.style.alignItems = 'center';
                                        el.style.justifyContent = 'center';
                                        el.style.width = '68px';
                                        el.style.height = '68px';
                                        el.style.minWidth = '68px';
                                        el.style.minHeight = '68px';
                                        el.style.borderRadius = '50%';
                                        el.style.background = 'rgba(13, 14, 21, 0.85)';
                                        el.style.boxShadow = '0 0 12px rgba(0, 240, 255, 0.3)';
                                        el.style.margin = '0 0 0 auto';

                                        if (!el.querySelector('.cyber-svg-ring')) {
                                            var svgNS = "http://www.w3.org/2000/svg";
                                            var svg = document.createElementNS(svgNS, "svg");
                                            svg.setAttribute("class", "cyber-svg-ring");
                                            svg.setAttribute("viewBox", "0 0 70 70");
                                            svg.style.position = "absolute";
                                            svg.style.top = "0";
                                            svg.style.left = "0";
                                            svg.style.width = "100%";
                                            svg.style.height = "100%";
                                            svg.style.pointerEvents = "none";
                                            svg.style.transform = "rotate(-90deg)";

                                            var bgCircle = document.createElementNS(svgNS, "circle");
                                            bgCircle.setAttribute("cx", "35");
                                            bgCircle.setAttribute("cy", "35");
                                            bgCircle.setAttribute("r", "31");
                                            bgCircle.setAttribute("fill", "none");
                                            bgCircle.setAttribute("stroke", "rgba(0, 240, 255, 0.2)");
                                            bgCircle.setAttribute("stroke-width", "3.5");

                                            var fgCircle = document.createElementNS(svgNS, "circle");
                                            fgCircle.setAttribute("cx", "35");
                                            fgCircle.setAttribute("cy", "35");
                                            fgCircle.setAttribute("r", "31");
                                            fgCircle.setAttribute("fill", "none");
                                            fgCircle.setAttribute("stroke", "#00F0FF");
                                            fgCircle.setAttribute("stroke-width", "3.5");
                                            fgCircle.setAttribute("stroke-linecap", "round");
                                            fgCircle.setAttribute("stroke-dasharray", "195");
                                            fgCircle.setAttribute("stroke-dashoffset", "195");
                                            fgCircle.style.filter = "drop-shadow(0 0 6px #00F0FF)";
                                            fgCircle.style.animation = "cyberFill 60s linear infinite";

                                            svg.appendChild(bgCircle);
                                            svg.appendChild(fgCircle);
                                            el.appendChild(svg);
                                        }
                                    }
                                }
                            });
                        }

                        applyCyberRings();
                        setInterval(applyCyberRings, 300);

                        var observer = new MutationObserver(applyCyberRings);
                        if (document.body) {
                            observer.observe(document.body, { childList: true, subtree: true });
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(cyberpunkJs, null)

                if (splashLayout.visibility == View.VISIBLE) {
                    splashLayout.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction {
                            splashLayout.visibility = View.GONE
                            webView.visibility = View.VISIBLE
                        }
                }
            }
        }

        webView.loadUrl("https://trans-puntano-go.base44.app/")
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
