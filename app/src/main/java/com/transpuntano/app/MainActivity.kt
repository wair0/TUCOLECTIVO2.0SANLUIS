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

        // Temporizador de respaldo infalible: Oculta el Splash en 2 segundos sí o sí
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

                val bulletproofCyberJs = """
                    (function() {
                        try {
                            if (!document.getElementById('cyber-bulletproof-style')) {
                                var style = document.createElement('style');
                                style.id = 'cyber-bulletproof-style';
                                style.innerHTML = `
                                    @keyframes cyberFill {
                                        0% { stroke-dashoffset: 201; }
                                        100% { stroke-dashoffset: 0; }
                                    }

                                    /* 1. Ocultar marca Base 44 */
                                    a[href*="base44"], [class*="base44"], [id*="base44"] {
                                        display: none !important;
                                        visibility: hidden !important;
                                        opacity: 0 !important;
                                    }

                                    /* 2. Ocultar específicamente el listado duplicado de líneas en el inicio 
                                       buscando tarjetas que contengan la estructura de código de línea y recorrido */
                                    div:has(> div > span), div:has(> small) {
                                        /* Protegemos los elementos normales, solo filtramos contenedores huérfanos de inicio si es necesario */
                                    }

                                    /* Estilo de Anillo Neón para contadores en paradas */
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

                            function runSafeTransformations() {
                                try {
                                    // 1. Limpiar marca Base 44 de forma aislada
                                    var baseElems = document.querySelectorAll('a, button, div');
                                    baseElems.forEach(function(el) {
                                        if (el.children.length === 0 && el.textContent && el.textContent.includes('Edit with Base 44')) {
                                            var box = el.closest('div[style*="fixed"], div[style*="absolute"], a, button');
                                            if (box && box !== document.body) {
                                                box.style.setProperty('display', 'none', 'important');
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

                                    // 3. Ocultar de forma segura la lista de líneas en el Inicio sin afectar la sección "Líneas" del menú inferior
                                    var allCards = document.querySelectorAll('div');
                                    allCards.forEach(function(card) {
                                        // Verificamos si es una tarjeta de línea individual en la pantalla principal
                                        var txt = card.innerText || '';
                                        if (txt.includes('Recorrido') && txt.includes('codLinea') && txt.includes('Ver calles')) {
                                            // Asegurarnos de que no estemos dentro de la sección dedicada de líneas
                                            var isDedicatedLinesPage = false;
                                            var parentCheck = card.parentElement;
                                            while(parentCheck) {
                                                var pText = (parentCheck.innerText || '').toUpperCase();
                                                if (pText.startsWith('LÍNEAS') && !pText.includes('TRANSPUNTANO')) {
                                                    // Si el contenedor principal es la vista dedicada de líneas, no la tocamos
                                                    // Pero si está en la pantalla principal (Inicio), la ocultamos
                                                }
                                                parentCheck = parentCheck.parentElement;
                                            }
                                            
                                            // Filtro seguro por texto exacto de cabecera de inicio
                                            if (document.body.innerText.includes('TRANSPUNTANO') && document.body.innerText.includes('Buscar línea')) {
                                                // Estamos en la pantalla de inicio, ocultar tarjetas de líneas sueltas
                                                var lineCardContainer = card.closest('div[class*="rounded"], div[style*="border"], div');
                                                if (lineCardContainer && lineCardContainer.children.length > 0 && !lineCardContainer.closest('nav')) {
                                                    // Comprobamos que sea una tarjeta individual de línea
                                                    if (txt.indexOf('LINEA') !== -1 || txt.indexOf('LÍNEA') !== -1) {
                                                        card.style.setProperty('display', 'none', 'important');
                                                    }
                                                }
                                            }
                                        }
                                    });

                                    // 4. Anillo Neón en contadores de tiempo de paradas
                                    var allNodes = document.querySelectorAll('div, span, p');
                                    allNodes.forEach(function(el) {
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

                                } catch(e) {
                                    console.error('Safe transformation error:', e);
                                }
                            }

                            runSafeTransformations();
                            setInterval(runSafeTransformations, 400);

                        } catch(err) {
                            console.error('Bulletproof init error:', err);
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(bulletproofCyberJs, null)
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
