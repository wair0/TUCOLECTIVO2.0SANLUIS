package com.transpuntano.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashLayout: RelativeLayout
    private var isSplashHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splashLayout = findViewById(R.id.splashLayout)

        webView.visibility = View.VISIBLE

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
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideSplash()

                val failproofCyberJs = """
                    (function() {
                        try {
                            if (!document.getElementById('cyber-failproof-style')) {
                                var style = document.createElement('style');
                                style.id = 'cyber-failproof-style';
                                style.innerHTML = `
                                    @keyframes cyberFill {
                                        0% { stroke-dashoffset: 201; }
                                        100% { stroke-dashoffset: 0; }
                                    }

                                    /* Ocultar Base 44 via CSS puro */
                                    a[href*="base44"], [class*="base44"], [id*="base44"], iframe[src*="base44"] {
                                        display: none !important;
                                        visibility: hidden !important;
                                        opacity: 0 !important;
                                        pointer-events: none !important;
                                    }

                                    /* Estilos para anillo Neón en paradas */
                                    .cyber-ring-container {
                                        position: relative !important;
                                        display: inline-flex !important;
                                        flex-direction: column !important;
                                        align-items: center !important;
                                        justify-content: center !important;
                                        width: 72px !important;
                                        height: 72px !important;
                                        min-width: 72px !important;
                                        min-height: 72px !important;
                                        border-radius: 50% !important;
                                        background: rgba(13, 14, 21, 0.85) !important;
                                        box-shadow: 0 0 10px rgba(0, 240, 255, 0.25) !important;
                                        margin: 0 0 0 auto !important;
                                        box-sizing: border-box !important;
                                        padding: 2px !important;
                                    }
                                `;
                                (document.head || document.documentElement).appendChild(style);
                            }

                            function applyNonDestructiveCustomizations() {
                                try {
                                    // 1. Ocultar elemento flotante de Base 44 aisladamente
                                    var baseBadges = document.querySelectorAll('a, button, div');
                                    baseBadges.forEach(function(el) {
                                        if (el.children.length === 0 && el.textContent && el.textContent.includes('Base 44')) {
                                            var box = el.closest('div[style*="fixed"], div[style*="absolute"], a, button');
                                            if (box && box !== document.body && box !== document.documentElement) {
                                                box.style.setProperty('display', 'none', 'important');
                                            } else {
                                                el.style.setProperty('display', 'none', 'important');
                                            }
                                        }
                                    });

                                    // 2. Mayúsculas en subtítulo superior
                                    var subheaders = document.querySelectorAll('p, span, small, div');
                                    subheaders.forEach(function(el) {
                                        if (el.children.length === 0 && el.textContent) {
                                            var txt = el.textContent.trim().toLowerCase();
                                            if (txt.includes('transporte urbano') && txt.includes('tiempo real')) {
                                                if (el.textContent !== 'TRANSPORTE URBANO DE SAN LUIS EN TIEMPO REAL') {
                                                    el.textContent = 'TRANSPORTE URBANO DE SAN LUIS EN TIEMPO REAL';
                                                    el.style.textTransform = 'uppercase';
                                                    el.style.fontSize = '11px';
                                                    el.style.letterSpacing = '1px';
                                                    el.style.opacity = '0.85';
                                                }
                                            }
                                        }
                                    });

                                    // 3. Reestructurar accesos de la barra del medio en Grid 2x2 Cyberpunk
                                    var chips = document.querySelectorAll('button, a, div[role="button"]');
                                    chips.forEach(function(btn) {
                                        var t = (btn.innerText || btn.textContent || '').trim();
                                        if (['Líneas', 'Paradas', 'Mapa', 'Cercanas', 'Favoritos'].indexOf(t) !== -1) {
                                            if (!btn.closest('nav') && !btn.closest('footer')) {
                                                var parent = btn.parentElement;
                                                if (parent && parent.children.length >= 3) {
                                                    parent.style.setProperty('display', 'grid', 'important');
                                                    parent.style.setProperty('grid-template-columns', 'repeat(2, 1fr)', 'important');
                                                    parent.style.setProperty('gap', '12px', 'important');
                                                    parent.style.setProperty('padding', '12px 6px', 'important');
                                                    parent.style.setProperty('overflow', 'visible', 'important');
                                                }

                                                btn.style.setProperty('display', 'flex', 'important');
                                                btn.style.setProperty('flex-direction', 'column', 'important');
                                                btn.style.setProperty('align-items', 'center', 'important');
                                                btn.style.setProperty('justify-content', 'center', 'important');
                                                btn.style.setProperty('min-height', '68px', 'important');
                                                btn.style.setProperty('background', 'rgba(13, 14, 21, 0.95)', 'important');
                                                btn.style.setProperty('border', '1.5px solid #00F0FF', 'important');
                                                btn.style.setProperty('border-radius', '14px', 'important');
                                                btn.style.setProperty('color', '#00F0FF', 'important');
                                                btn.style.setProperty('font-weight', 'bold', 'important');
                                                btn.style.setProperty('box-shadow', '0 0 10px rgba(0, 240, 255, 0.25)', 'important');
                                                btn.style.setProperty('text-transform', 'uppercase', 'important');
                                                btn.style.setProperty('letter-spacing', '1px', 'important');
                                                btn.style.setProperty('font-size', '13px', 'important');
                                            }
                                        }
                                    });

                                    // 4. Anillos Neón en contadores de minutos
                                    var minRegex = new RegExp('^\\\\d+\\\\s*MIN', 'i');
                                    var allNodes = document.querySelectorAll('div, span, p');
                                    allNodes.forEach(function(el) {
                                        var txt = (el.innerText || el.textContent || '').trim().replace(/\s+/g, ' ');
                                        if (minRegex.test(txt)) {
                                            var hasMatchingChild = Array.from(el.children).some(function(child) {
                                                return minRegex.test((child.innerText || child.textContent || '').trim().replace(/\s+/g, ' '));
                                            });

                                            if (!hasMatchingChild) {
                                                if (el.parentElement) {
                                                    el.parentElement.style.overflow = 'visible';
                                                }

                                                if (!el.classList.contains('cyber-ring-container')) {
                                                    el.classList.add('cyber-ring-container');
                                                }

                                                var innerElems = el.querySelectorAll('*');
                                                innerElems.forEach(function(child) {
                                                    var cTxt = (child.innerText || child.textContent || '').trim();
                                                    if (/^\d+$/.test(cTxt)) {
                                                        child.style.setProperty('font-size', '18px', 'important');
                                                        child.style.setProperty('line-height', '1', 'important');
                                                        child.style.setProperty('font-weight', 'bold', 'important');
                                                    } else if (cTxt.toUpperCase() === 'MIN') {
                                                        child.style.setProperty('font-size', '9px', 'important');
                                                        child.style.setProperty('line-height', '1', 'important');
                                                        child.style.setProperty('margin-top', '2px', 'important');
                                                        child.style.setProperty('opacity', '0.85', 'important');
                                                    }
                                                });

                                                if (!el.querySelector('.cyber-svg-ring')) {
                                                    var svgNS = "http://www.w3.org/2000/svg";
                                                    var svg = document.createElementNS(svgNS, "svg");
                                                    svg.setAttribute("class", "cyber-svg-ring");
                                                    svg.setAttribute("viewBox", "0 0 76 76");
                                                    svg.style.position = "absolute";
                                                    svg.style.top = "0";
                                                    svg.style.left = "0";
                                                    svg.style.width = "100%";
                                                    svg.style.height = "100%";
                                                    svg.style.pointerEvents = "none";
                                                    svg.style.transform = "rotate(-90deg)";

                                                    var bgCircle = document.createElementNS(svgNS, "circle");
                                                    bgCircle.setAttribute("cx", "38");
                                                    bgCircle.setAttribute("cy", "38");
                                                    bgCircle.setAttribute("r", "32");
                                                    bgCircle.setAttribute("fill", "none");
                                                    bgCircle.setAttribute("stroke", "rgba(0, 240, 255, 0.18)");
                                                    bgCircle.setAttribute("stroke-width", "3.5");

                                                    var fgCircle = document.createElementNS(svgNS, "circle");
                                                    fgCircle.setAttribute("cx", "38");
                                                    fgCircle.setAttribute("cy", "38");
                                                    fgCircle.setAttribute("r", "32");
                                                    fgCircle.setAttribute("fill", "none");
                                                    fgCircle.setAttribute("stroke", "#00F0FF");
                                                    fgCircle.setAttribute("stroke-width", "3.5");
                                                    fgCircle.setAttribute("stroke-linecap", "round");
                                                    fgCircle.setAttribute("stroke-dasharray", "201");
                                                    fgCircle.setAttribute("stroke-dashoffset", "201");
                                                    fgCircle.style.filter = "drop-shadow(0 0 6px #00F0FF)";
                                                    fgCircle.style.animation = "cyberFill 60s linear infinite";

                                                    svg.appendChild(bgCircle);
                                                    svg.appendChild(fgCircle);
                                                    el.appendChild(svg);
                                                }
                                            }
                                        }
                                    });

                                } catch(err) {
                                    console.error('Cyber err:', err);
                                }
                            }

                            applyNonDestructiveCustomizations();
                            setInterval(applyNonDestructiveCustomizations, 400);

                        } catch(e) {
                            console.error('Cyber init err:', e);
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(failproofCyberJs, null)
            }
        }

        webView.loadUrl("https://trans-puntano-go.base44.app/")
    }

    private fun hideSplash() {
        if (!isSplashHidden) {
            isSplashHidden = true
            if (::webView.isInitialized) {
                webView.visibility = View.VISIBLE
            }
            if (::splashLayout.isInitialized) {
                splashLayout.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        splashLayout.visibility = View.GONE
                    }
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
