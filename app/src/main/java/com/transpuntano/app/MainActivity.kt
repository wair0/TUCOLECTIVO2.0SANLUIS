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

                // Inyección segura: CSS puro para el anillo Neón sin destruir el DOM
                val safeCyberJs = """
                    (function() {
                        if (document.getElementById('cyber-safe-style')) return;

                        var style = document.createElement('style');
                        style.id = 'cyber-safe-style';
                        style.innerHTML = `
                            /* Ocultar insignia Base44 */
                            a[href*="base44"], [class*="base44"], div[style*="fixed"][style*="bottom"] {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }

                            /* Círculo Neón Cyberpunk sobre los contadores */
                            .cyber-neon-ring {
                                position: relative !important;
                                display: inline-flex !important;
                                flex-direction: column !important;
                                align-items: center !important;
                                justify-content: center !important;
                                width: 68px !important;
                                height: 68px !important;
                                min-width: 68px !important;
                                min-height: 68px !important;
                                border-radius: 50% !important;
                                background: rgba(13, 14, 21, 0.85) !important;
                                border: 2px solid rgba(0, 240, 255, 0.25) !important;
                                box-shadow: 0 0 12px rgba(0, 240, 255, 0.35), inset 0 0 8px rgba(0, 240, 255, 0.15) !important;
                                box-sizing: border-box !important;
                                margin: 4px auto !important;
                            }

                            /* Anillo Neón giratorio resplandeciente */
                            .cyber-neon-ring::before {
                                content: '' !important;
                                position: absolute !important;
                                top: -4px !important;
                                left: -4px !important;
                                right: -4px !important;
                                bottom: -4px !important;
                                border-radius: 50% !important;
                                border: 3px solid transparent !important;
                                border-top-color: #00F0FF !important;
                                border-right-color: #00F0FF !important;
                                filter: drop-shadow(0 0 6px #00F0FF) !important;
                                animation: cyberSpin 2s linear infinite !important;
                            }

                            @keyframes cyberSpin {
                                0% { transform: rotate(0deg); }
                                100% { transform: rotate(360deg); }
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);

                        // Asignación de clase sin modificar texto ni borrar componentes
                        function styleCounters() {
                            var elems = document.querySelectorAll('div, span, p');
                            elems.forEach(function(el) {
                                if (el.children.length === 0 && (el.textContent || '').trim() === 'MIN') {
                                    var parent = el.parentElement;
                                    if (parent && !parent.classList.contains('cyber-neon-ring')) {
                                        parent.classList.add('cyber-neon-ring');
                                    }
                                }
                            });
                        }

                        styleCounters();
                        setInterval(styleCounters, 400);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(safeCyberJs, null)

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
