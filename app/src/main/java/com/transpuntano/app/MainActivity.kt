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
                        if (!document.getElementById('cyber-ring-style')) {
                            var style = document.createElement('style');
                            style.id = 'cyber-ring-style';
                            style.innerHTML = `
                                @keyframes cyberFill {
                                    0% { stroke-dashoffset: 201; }
                                    100% { stroke-dashoffset: 0; }
                                }
                                a[href*="base44"], [class*="base44"], div[style*="fixed"][style*="bottom"] {
                                    display: none !important;
                                    visibility: hidden !important;
                                    opacity: 0 !important;
                                }
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

                        function adjustAndApplyRings() {
                            var baseElems = document.querySelectorAll('a[href*="base44"], [class*="base44"], div[style*="fixed"][style*="bottom"]');
                            baseElems.forEach(function(el) {
                                el.style.setProperty('display', 'none', 'important');
                            });

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

                                        // Redimensionar tipografía interna para que no sobresalga del anillo
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
                        }

                        adjustAndApplyRings();
                        setInterval(adjustAndApplyRings, 300);

                        var observer = new MutationObserver(adjustAndApplyRings);
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
