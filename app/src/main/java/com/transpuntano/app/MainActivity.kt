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
        }, 2500)

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

                val cyberpunkJs = """
                    (function() {
                        try {
                            if (!document.getElementById('cyber-master-styles')) {
                                var style = document.createElement('style');
                                style.id = 'cyber-master-styles';
                                style.innerHTML = `
                                    @keyframes cyberFill {
                                        0% { stroke-dashoffset: 201; }
                                        100% { stroke-dashoffset: 0; }
                                    }

                                    /* Ocultar de forma absoluta el widget Base 44 */
                                    a[href*="base44"], [class*="base44"], [id*="base44"],
                                    iframe[src*="base44"],
                                    div[style*="fixed"][style*="bottom"],
                                    div[style*="position: fixed"] {
                                        display: none !important;
                                        visibility: hidden !important;
                                        opacity: 0 !important;
                                        pointer-events: none !important;
                                    }

                                    /* Cuadrícula Dashboard para pantalla de Inicio */
                                    .cyber-dashboard-grid {
                                        display: grid !important;
                                        grid-template-columns: repeat(2, 1fr) !important;
                                        gap: 14px !important;
                                        padding: 16px 10px !important;
                                        margin-top: 10px !important;
                                        width: 100% !important;
                                        box-sizing: border-box !important;
                                    }

                                    .cyber-dash-card {
                                        background: rgba(13, 14, 21, 0.95) !important;
                                        border: 1.5px solid #00F0FF !important;
                                        border-radius: 14px !important;
                                        padding: 18px 8px !important;
                                        display: flex !important;
                                        flex-direction: column !important;
                                        align-items: center !important;
                                        justify-content: center !important;
                                        box-shadow: 0 0 12px rgba(0, 240, 255, 0.25), inset 0 0 8px rgba(0, 240, 255, 0.1) !important;
                                        cursor: pointer !important;
                                        transition: transform 0.12s ease !important;
                                    }

                                    .cyber-dash-card:active {
                                        transform: scale(0.95) !important;
                                        border-color: #FF007F !important;
                                        box-shadow: 0 0 16px rgba(255, 0, 127, 0.4) !important;
                                    }

                                    .cyber-dash-icon {
                                        font-size: 26px !important;
                                        margin-bottom: 6px !important;
                                        filter: drop-shadow(0 0 6px #00F0FF) !important;
                                    }

                                    .cyber-dash-title {
                                        color: #00F0FF !important;
                                        font-size: 12px !important;
                                        font-weight: 800 !important;
                                        letter-spacing: 1px !important;
                                        text-transform: uppercase !important;
                                        text-shadow: 0 0 6px rgba(0, 240, 255, 0.6) !important;
                                    }

                                    /* Contenedor Anillo Neón para contadores */
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

                            function applyMasterTransformations() {
                                try {
                                    // 1. Remoción de la marca Base 44 via JS dinámico
                                    var allNodes = document.querySelectorAll('body *');
                                    allNodes.forEach(function(el) {
                                        if (el.children.length === 0 && el.textContent && el.textContent.includes('Edit with Base 44')) {
                                            var target = el;
                                            while (target && target !== document.body) {
                                                var stylePos = window.getComputedStyle(target).position;
                                                if (stylePos === 'fixed' || stylePos === 'absolute') {
                                                    target.style.setProperty('display', 'none', 'important');
                                                    break;
                                                }
                                                target = target.parentElement;
                                            }
                                        }
                                    });

                                    // 2. Mayúsculas en el subtítulo superior
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

                                    // 3. Reorganización de la Pantalla de Inicio
                                    var isInicio = Array.from(document.querySelectorAll('div, h1, span')).some(function(el) {
                                        return (el.textContent || '').trim().toUpperCase() === 'TRANSPUNTANO';
                                    });

                                    if (isInicio) {
                                        // 3a. Ocultar la barra horizontal de accesos pequeños duplicados bajo el buscador
                                        var quickNavRow = Array.from(document.querySelectorAll('div')).find(function(d) {
                                            if (d.children.length >= 3 && !d.closest('nav') && !d.closest('footer')) {
                                                var t = (d.innerText || d.textContent || '');
                                                return t.includes('Líneas') && t.includes('Paradas') && t.includes('Mapa');
                                            }
                                            return false;
                                        });
                                        if (quickNavRow) {
                                            quickNavRow.style.setProperty('display', 'none', 'important');
                                        }

                                        // 3b. Ocultar la sección duplicada de líneas en Inicio
                                        var linesSection = Array.from(document.querySelectorAll('div, section')).find(function(d) {
                                            if (!d.closest('nav') && !d.closest('footer')) {
                                                var t = (d.innerText || d.textContent || '').trim();
                                                return t.startsWith('LÍNEAS') && t.includes('Ver todo') && t.includes('LINEA');
                                            }
                                            return false;
                                        });
                                        if (linesSection) {
                                            linesSection.style.setProperty('display', 'none', 'important');
                                        }

                                        // 3c. Inyectar Grid de Accesos Grandes (Dashboard)
                                        var searchInput = document.querySelector('input[placeholder*="Buscar"], input');
                                        if (searchInput) {
                                            var searchContainer = searchInput.closest('div');
                                            while (searchContainer && searchContainer.parentElement && searchContainer.parentElement.children.length === 1) {
                                                searchContainer = searchContainer.parentElement;
                                            }

                                            if (searchContainer && !document.getElementById('cyber-dashboard-grid')) {
                                                var grid = document.createElement('div');
                                                grid.id = 'cyber-dashboard-grid';
                                                grid.className = 'cyber-dashboard-grid';

                                                var cardsData = [
                                                    { title: 'LÍNEAS', icon: '🚌', target: 'Líneas' },
                                                    { title: 'PARADAS', icon: '🚏', target: 'Paradas' },
                                                    { title: 'MAPA', icon: '🗺️', target: 'Mapa' },
                                                    { title: 'FAVORITOS', icon: '⭐', target: 'Favoritos' }
                                                ];

                                                cardsData.forEach(function(item) {
                                                    var card = document.createElement('div');
                                                    card.className = 'cyber-dash-card';
                                                    card.innerHTML = '<span class="cyber-dash-icon">' + item.icon + '</span><span class="cyber-dash-title">' + item.title + '</span>';
                                                    
                                                    card.onclick = function() {
                                                        var navItems = document.querySelectorAll('nav *, footer *, [class*="bottom"] *');
                                                        navItems.forEach(function(navEl) {
                                                            var txt = (navEl.innerText || navEl.textContent || '').trim();
                                                            if (txt.toLowerCase() === item.target.toLowerCase()) {
                                                                navEl.click();
                                                            }
                                                        });
                                                    };

                                                    grid.appendChild(card);
                                                });

                                                searchContainer.parentElement.insertBefore(grid, searchContainer.nextSibling);
                                            }
                                        }
                                    }

                                    // 4. Anillos Neón en los contadores de minutos
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
                                    console.error('Cyber error:', err);
                                }
                            }

                            applyMasterTransformations();
                            setInterval(applyMasterTransformations, 300);

                        } catch(e) {
                            console.error('Init error:', e);
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(cyberpunkJs, null)
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
