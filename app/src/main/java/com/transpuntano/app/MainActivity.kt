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
                // Remueve automáticamente la insignia de Base 44 al cargar
                val hideBadgeJs = """
                    (function() {
                        function removeBadge() {
                            var elems = document.querySelectorAll('*');
                            for (var i = 0; i < elems.length; i++) {
                                var el = elems[i];
                                if (el.innerText && el.innerText.includes('Edit with Base 44')) {
                                    el.style.setProperty('display', 'none', 'important');
                                }
                            }
                        }
                        removeBadge();
                        setTimeout(removeBadge, 500);
                        setTimeout(removeBadge, 1500);
                        setTimeout(removeBadge, 3000);
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
