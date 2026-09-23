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

                // Inyección de CSS y JS para convertir contadores en Anillos Neón Cyberpunk
                val cyberpunkJs = """
                    (function() {
                        // 1. Estilos CSS del Círculo Neón Animado
                        var style = document.createElement('style');
                        style.innerHTML = `
                            /* Ocultar insignia Base44 */
                            a[href*="base44"], [class*="base44"], div[style*="fixed"][style*="bottom"] {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                            }

                            /* Estructura del Círculo Neón */
                            .cyber-timer-box {
                                position: relative;
                                width: 64px;
                                height: 64px;
                                display: inline-flex;
                                align-items: center;
                                justify-content: center;
                                flex-shrink: 0;
                            }
                            .cyber-timer-svg {
                                width: 64px;
                                height: 64px;
                                transform: rotate(-90deg);
                            }
                            .cyber-ring-bg {
                                fill: none;
                                stroke: rgba(0, 240, 255, 0.12);
                                stroke-width: 4;
                            }
                            .cyber-ring-progress {
                                fill: none;
                                stroke: #00F0FF;
                                stroke-width: 4;
                                stroke-linecap: round;
                                stroke-dasharray: 157;
                                stroke-dashoffset: 157;
                                filter: drop-shadow(0 0 6px #00F0FF);
                                animation: cyberRingFill 60s linear infinite;
                            }
                            @keyframes cyberRingFill {
                                0% { stroke-dashoffset: 157; }
                                100% { stroke-dashoffset: 0; }
                            }
                            .cyber-timer-text {
                                position: absolute;
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                color: #00F0FF;
                                text-shadow: 0 0 8px rgba(0, 240, 255, 0.9);
                            }
                            .cyber-timer-num {
                                font-size: 20px;
                                font-weight: 900;
                                line-height: 1;
                                font-family: 'Courier New', monospace, sans-serif;
                            }
                            .cyber-timer-unit {
                                font-size: 8px;
                                font-weight: bold;
                                letter-spacing: 1px;
                                margin-top: 2px;
                                color: #80f8ff;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);

                        // 2. Función para rastrear y convertir números de minutos a Círculos Neón
                        function transformMinuteCounters() {
                            // Borrado de marca Base 44
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

                            // Transformar bloques con la palabra MIN
                            var allNodes = document.querySelectorAll('*');
                            allNodes.forEach(function(node) {
                                if (node.children.length === 0 && node.textContent.trim() === 'MIN') {
                                    var parent = node.parentElement;
                                    if (parent && !parent.classList.contains('cyber-converted')) {
                                        parent.classList.add('cyber-converted');

                                        var fullText = parent.innerText || parent.textContent;
                                        var matches = fullText.match(/\d+/);
                                        var numText = matches ? matches[0] : '0';

                                        var widget = document.createElement('div');
                                        widget.className = 'cyber-timer-box';
                                        widget.innerHTML = 
                                            '<svg class="cyber-timer-svg" viewBox="0 0 60 60">' +
                                                '<circle class="cyber-ring-bg" cx="30" cy="30" r="25"/>' +
                                                '<circle class="cyber-ring-progress" cx="30" cy="30" r="25"/>' +
                                            '</svg>' +
                                            '<div class="cyber-timer-text">' +
                                                '<span class="cyber-timer-num">' + numText + '</span>' +
                                                '<span class="cyber-timer-unit">MIN</span>' +
                                            '</div>';

                                        parent.innerHTML = '';
                                        parent.appendChild(widget);
                                    }
                                }
                            });
                        }

                        transformMinuteCounters();
                        setInterval(transformMinuteCounters, 400);

                        var observer = new MutationObserver(transformMinuteCounters);
                        if (document.body) {
                            observer.observe(document.body, { childList: true, subtree: true });
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(cyberpunkJs, null)

                // Transición de ocultado de Splash Screen
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
