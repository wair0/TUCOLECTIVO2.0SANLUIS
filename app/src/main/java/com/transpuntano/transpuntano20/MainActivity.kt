package com.transpuntano.transpuntano20

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var offlineOverlay: View
    private lateinit var offlineTitle: TextView
    private lateinit var offlineMsg: TextView
    private lateinit var btnRetry: MaterialButton

    private val baseUrl = "https://cuandollega.smartmovepro.net/transpuntano"
    private var currentSection = "inicio"

    // CSS cyberpunk injection – dark tech + glass + neon accents
    private val cyberCss = """
        javascript:(function(){
            var s = document.createElement('style');
            s.innerHTML = `
                :root {
                    --bg: #080C14 !important;
                    --surface: #0E1420 !important;
                    --surface2: #121A29 !important;
                    --cyan: #00F2FF !important;
                    --magenta: #FF007F !important;
                    --green: #00FF88 !important;
                    --text: #FFFFFF !important;
                    --text-sec: #6C7D93 !important;
                }
                html, body {
                    background: var(--bg) !important;
                    color: var(--text) !important;
                }
                header, .header, .navbar, .toolbar, .top-bar, [class*="header"], [class*="nav"] {
                    background: linear-gradient(180deg, #0E1420 0%, #080C14 100%) !important;
                    border-bottom: 1px solid rgba(0,242,255,0.25) !important;
                    color: var(--cyan) !important;
                }
                a, button, .btn, [role="button"] {
                    color: var(--cyan) !important;
                }
                .card, .tile, .item, .list-item, [class*="card"], [class*="tile"] {
                    background: rgba(14,20,32,0.85) !important;
                    border: 1px solid rgba(0,242,255,0.2) !important;
                    border-radius: 12px !important;
                    backdrop-filter: blur(8px) !important;
                    box-shadow: 0 0 12px rgba(0,242,255,0.08) !important;
                }
                input, .search, [type="search"], [type="text"] {
                    background: var(--surface2) !important;
                    border: 1px solid rgba(0,242,255,0.35) !important;
                    color: var(--text) !important;
                    border-radius: 20px !important;
                }
                .progress, progress, [class*="progress"] {
                    accent-color: var(--cyan) !important;
                }
                /* Hide install / update banners if present */
                [class*="install"], [class*="update"], .banner-update {
                    display: none !important;
                }
            `;
            document.head.appendChild(s);
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        bottomNav = findViewById(R.id.bottomNav)
        offlineOverlay = findViewById(R.id.offlineOverlay)
        offlineTitle = findViewById(R.id.offlineTitle)
        offlineMsg = findViewById(R.id.offlineMsg)
        btnRetry = findViewById(R.id.btnRetry)

        setupWebView()
        setupBottomNav()
        setupBack()
        btnRetry.setOnClickListener { reloadCurrent() }

        // Start at home
        loadSection("inicio")
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            userAgentString = userAgentString + " Transpuntano20/2.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.isVisible = true
                offlineOverlay.isVisible = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.isVisible = false
                // Inject cyberpunk CSS on every page
                view?.evaluateJavascript(cyberCss, null)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    progressBar.isVisible = false
                    offlineTitle.text = getString(R.string.error_title)
                    offlineMsg.text = getString(R.string.error_msg)
                    offlineOverlay.isVisible = true
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Keep everything inside the WebView for hierarchy navigation
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.isVisible = newProgress < 100
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> loadSection("inicio")
                R.id.nav_lineas -> loadSection("lineas")
                R.id.nav_paradas -> loadSection("paradas")
                R.id.nav_favoritos -> loadSection("favoritos")
                R.id.nav_cercanas -> loadSection("cercanas")
            }
            true
        }
        bottomNav.selectedItemId = R.id.nav_inicio
    }

    private fun loadSection(section: String) {
        currentSection = section
        offlineOverlay.isVisible = false
        val url = when (section) {
            "inicio" -> baseUrl
            "lineas" -> "$baseUrl/lineas"
            "paradas" -> "$baseUrl/recorridos" // or paradas if available
            "favoritos" -> "$baseUrl/favoritos"
            "cercanas" -> "$baseUrl/paradascercanas"
            else -> baseUrl
        }
        webView.loadUrl(url)
    }

    private fun reloadCurrent() {
        offlineOverlay.isVisible = false
        webView.reload()
    }

    private fun setupBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
