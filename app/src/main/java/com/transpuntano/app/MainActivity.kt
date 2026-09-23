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
                        // 1. Inyectar CSS Cyberpunk Neón
                        if (!document.getElementById('cyber-style')) {
                            var style = document.createElement('style');
                            style.id = 'cyber-style';
                            style.innerHTML = `
                                /* Ocultar marca Base44 */
                                a[href*="base44"], [class*="base44"], div[style*="fixed"][style*="bottom"] {
                                    display: none !important;
                                    visibility: hidden !important;
                                    opacity: 0 !important;
                                }

                                /* Contenedor del Anillo Neón */
                                .cyber-timer-box {
                                    position: relative;
                                    width: 68px;
                                    height: 68px;
                                    display: inline-flex;
                                    align-items: center;
                                    justify-content: center;
                                    margin: 0 auto;
                                }
                                .cyber-timer-svg {
                                    width: 68px;
                                    height: 68px;
                                    transform: rotate(-90deg);
                                    overflow: visible;
                                }
                                .cyber-ring-bg {
                                    fill: none;
                                    stroke: rgba(0, 240, 255, 0.15);
                                    stroke-width: 4;
                                }
                                .cyber-ring-progress {
                                    fill: none;
                                    stroke: #00F0FF;
                                    stroke-width: 4;
                                    stroke-linecap: round;
                                    stroke-dasharray: 170;
                                    stroke-dashoffset: 170;
                                    filter: drop-shadow(0 0 8px #00F0FF);
                                    animation: cyberRingFill 60s linear infinite;
                                }
                                @keyframes cyberRingFill {
                                    0% { stroke-dashoffset: 170; }
                                    100% { stroke-dashoffset: 0; }
                                }
                                .cyber-timer-text {
                                    position: absolute;
                                    top: 0; left: 0; right: 0; bottom: 0;
                                    display: flex;
                                    flex-direction: column;
                                    align-items: center;
                                    justify-content: center;
                                    color: #00F0FF;
                                    text-shadow: 0 0 10px rgba(0, 240, 255, 0.9);
                                    pointer-events: none;
                                }
                                .cyber-timer-num {
                                    font-size: 22px;
                                    font-weight: 900;
                                    line-height: 1;
                                    font-family: monospace, sans-serif;
                                }
                                .cyber-timer-unit {
                                    font-size: 9px;
                                    font-weight: bold;
                                    letter-spacing: 1px;
                                    margin-top: 2px;
                                    color: #80f8ff;
                                }
                            `;
                            (document.head || document.documentElement).appendChild(style);
                        }

                        // 2. Escanear y transformar bloques de tiempo (ej. "6 MIN", "25 MIN")
                        function transformArrivalTimes() {
                            // Borrar aviso Base44 si reaparece
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

                            // Transformación directa
                            var allNodes = document.querySelectorAll('div, span, p');
                            allNodes.forEach(function(el) {
                                if (el.dataset.cyberTransformed) return;

                                var text = (el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim();

                                // Coincide con texto que contenga solo números y la palabra MIN (ej: "6 MIN", "25 MIN")
                                if (/^\d+\s*MIN$/i.test(text)) {
                                    var match = text.match(/\d+/);
                                    if (match) {
                                        var num = match[0];
                                        el.dataset.cyberTransformed = "true";
                                        el.style.background = "transparent";
                                        el.style.border = "none";
                                        el.style.display = "inline-flex";
                                        el.style.justifyContent = "center";
                                        el.style.alignItems = "center";

                                        el.innerHTML = `
                                            <div class="cyber-timer-box">
                                                <svg class="cyber-timer-svg" viewBox="0 0 70 70">
                                                    <circle class="cyber-ring-bg" cx="35" cy="35" r="27"/>
                                                    <circle class="cyber-ring-progress" cx="35" cy="35" r="27"/>
                                                </svg>
                                                <div class="cyber-timer-text">
                                                    <span class="cyber-timer-num">\${num}</span>
                                                    <span class="cyber-timer-unit">MIN</span>
                                                </div>
                                            </div>
                                        `;
                                    }
                                }
                            });
                        }

                        transformArrivalTimes();
                        setInterval(transformArrivalTimes, 300);

                        var observer = new MutationObserver(transformArrivalTimes);
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
