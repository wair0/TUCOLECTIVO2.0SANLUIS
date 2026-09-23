package com.transpuntano.app

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

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
                
                // Inyección agresiva y continua para eliminar la insignia de Base 44
                val hideBadgeJs = """
                    (function() {
                        // 1. Regla CSS global para ocultar contenedores flotantes sospechosos
                        var style = document.createElement('style');
                        style.innerHTML = `
                            a[href*="base44"],
                            [class*="base44"],
                            div[style*="fixed"][style*="bottom"] {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);

                        // 2. Función para rastrear y ocultar el elemento exacto por texto
                        function removeBase44Badge() {
                            var elements = document.querySelectorAll('div, a, span, p, button');
                            elements.forEach(function(el) {
                                if (el.textContent && el.textContent.includes('Edit with Base 44')) {
                                    var target = el;
                                    // Buscar el contenedor flotante principal
                                    while (target.parentElement && target.parentElement !== document.body) {
                                        var stylePos = window.getComputedStyle(target).position;
                                        if (stylePos === 'fixed' || stylePos === 'absolute') {
                                            break;
                                        }
                                        target = target.parentElement;
                                    }
                                    target.style.setProperty('display', 'none', 'important');
                                    target.style.setProperty('visibility', 'hidden', 'important');
                                }
                            });
                        }

                        // 3. Ejecución inicial, continua y mediante observador
                        removeBase44Badge();
                        setInterval(removeBase44Badge, 300);

                        var observer = new MutationObserver(removeBase44Badge);
                        if (document.body) {
                            observer.observe(document.body, { childList: true, subtree: true });
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(hideBadgeJs, null)
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
