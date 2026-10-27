/* TU COLECTIVO 2.0 - Seccion LINEAS High-Tech: ayudas visuales (vanilla JS, offline). NO contiene logica de negocio ni llamadas a SmartMove.
   window.TcLineas:
     bindPress(raiz)              feedback tactil/mouse (.hot / .press) por delegacion en tarjetas, botones y atras. Llamar una vez con document.body o la pantalla.
     lineCardHtml(l, i)           l = {code, name, routes}                 -> tarjeta de linea (.line-card[data-line])
     detailCardHtml(it, i, tag)   it = {code, name}; tag p. ej. "ARRIBOS" -> tarjeta .data-card.line-detail-card
     arrivalCardHtml(a, i, sel)   a = {line, destination, minutes, status} -> .arrival-card con circulo animado o estado ARRIBANDO
     stateHtml(tipo, texto)       tipo = loading | empty | error  (ACTUALIZANDO MINUTOS / SIN ARRIBOS / ERROR DE ARRIBOS)
     animateNumbers(raiz)         cuenta visual de 0 al valor en los minutos recien dibujados (solo animacion, no es un cronometro)
     arrivalDuration(m)           max(1.2, min(18, m*0.35)) s, o 8 s si m no es valido (misma regla que la app)
     isArriving(status, m)        ARRIBANDO | LLEGANDO | EN PARADA | PROXIMO/A (sin tildes) o m <= 1
   Los *Html devuelven el MARCADO de referencia que usan lineas.css; la app puede conservar su propio renderizado y solo agregar estas clases. */
(function(){
  function esc(s){return String(s==null?"":s).replace(/[&<>"']/g,function(c){return {"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c];});}
  function norm(s){return String(s||"").toUpperCase().normalize("NFD").replace(/[\u0300-\u036f]/g,"").trim();}
  var ICON={"inicio":"<path d=\"M5 22 24 6l19 16\"/><path d=\"M10 19v22h28V19\"/><path d=\"M20 41V29h8v12\"/><path class=\"a2\" d=\"M31 12V8h5v8.5\"/>","mapa":"<path d=\"M5 15 16 11v28L5 43z\"/><path d=\"M16 11l16 5v28L16 39z\"/><path d=\"M32 16l11-4v28L32 44z\"/><path d=\"M5 25l11-4M5 34l11-4\"/><path d=\"M16 31l16 5M16 36.5l16 5\"/><path d=\"M32 27l11-4M32 35l11-4\"/><path class=\"a2\" d=\"M24 27C24 27 15.5 19.5 15.5 13.5a8.5 8.5 0 0 1 17 0C32.5 19.5 24 27 24 27z\"/><circle class=\"a2\" cx=\"24\" cy=\"13.5\" r=\"3.2\"/>","lineas":"<path d=\"M5 17.5Q5 13 9.5 13H22v25.5H7.5Q5 38.5 5 36z\"/><path d=\"M8 16.5h11.5V26H8z\"/><circle cx=\"9.8\" cy=\"32\" r=\"1.5\"/><circle cx=\"17.7\" cy=\"32\" r=\"1.5\"/><path d=\"M7.5 35.2h12.5\"/><path d=\"M22 13l17.5 4.4Q42 18.1 42 21v15.2Q42 38.5 39.6 38.5h-1.5\"/><path d=\"M22 17.6l20 4.8\"/><path d=\"M22 28.8h20\"/><path d=\"M27 18.8V28.8M32 20V28.8M37 21.2V28.8\"/><path d=\"M22 38.5h4.5M32.5 38.5h2\"/><ellipse cx=\"13\" cy=\"40.4\" rx=\"3.2\" ry=\"3.8\"/><ellipse cx=\"29.6\" cy=\"40.4\" rx=\"3.2\" ry=\"3.8\"/>","paradas":"<rect class=\"a2\" x=\"4\" y=\"3.5\" width=\"25\" height=\"25\" rx=\"4\"/><path class=\"a2\" d=\"M10.2 10.2q0-1.6 1.6-1.6h9.4q1.6 0 1.6 1.6V22.8H10.2z\"/><path class=\"a2\" d=\"M12 11.8h9.9v5H12z\"/><circle class=\"a2\" cx=\"13.4\" cy=\"19.6\" r=\"1\"/><circle class=\"a2\" cx=\"20.6\" cy=\"19.6\" r=\"1\"/><path class=\"a2\" d=\"M8.6 13.6v4M24.6 13.6v4\"/><path d=\"M16.5 28.5v12\"/><path d=\"M12.8 40.5h7.4v2.8h-7.4z\"/><path d=\"M32 20.5q0-2.2 2.2-2.2h8.1q2.2 0 2.2 2.2V35.5H32z\"/><path d=\"M34.2 21.6h8.1v7h-8.1z\"/><circle cx=\"35.4\" cy=\"32\" r=\"1.1\"/><circle cx=\"41.1\" cy=\"32\" r=\"1.1\"/><path d=\"M34.4 35.5v3M42 35.5v3\"/><path d=\"M3 44h42\"/>","favoritos":"<path d=\"M24 38.5C24 38.5 10 26 10 16.5a14 14 0 0 1 28 0C38 26 24 38.5 24 38.5z\"/><circle cx=\"24\" cy=\"16.8\" r=\"9\"/><path class=\"a2\" d=\"M24 11.4 L25.47 15.28 L29.61 15.48 L26.38 18.07 L27.47 22.07 L24 19.8 L20.53 22.07 L21.62 18.07 L18.39 15.48 L22.53 15.28Z\"/><path d=\"M15.2 38.2Q8.5 39.3 8.5 41.6Q8.5 45 24 45Q39.5 45 39.5 41.6Q39.5 39.3 32.8 38.2\"/>","sync":"<path d=\"M17 31.2a5.2 5.2 0 0 1-.8-10.1 7.6 7.6 0 0 1 14.3-1.6 5.6 5.6 0 0 1 .9 11.7z\"/><path d=\"M20.8 29.2v-9M18.2 22.6l2.6-2.8 2.6 2.8\"/><path d=\"M27.2 20.4v9M24.6 26.9l2.6 2.8 2.6-2.8\"/><g class=\"ti-orbit\"><path d=\"M5.38 34.75 A21.5 21.5 0 0 1 21.01 2.71\"/><path d=\"M29.56 3.23 A21.5 21.5 0 0 1 43.49 33.09\"/><path d=\"M38.39 39.98 A21.5 21.5 0 0 1 10.76 40.94\"/><path class=\"th\" d=\"M7.56 18.01 A17.5 17.5 0 0 1 32.75 8.84\"/><path class=\"th\" d=\"M38.34 13.96 A17.5 17.5 0 0 1 29.99 40.44\"/><circle cx=\"24.75\" cy=\"2.51\" r=\"2.3\"/><circle cx=\"41.17\" cy=\"36.94\" r=\"2.3\"/><circle cx=\"8.02\" cy=\"38.39\" r=\"2.3\"/></g>","syncok":"<path d=\"M17 31.2a5.2 5.2 0 0 1-.8-10.1 7.6 7.6 0 0 1 14.3-1.6 5.6 5.6 0 0 1 .9 11.7z\"/><path d=\"M19.8 25.4l3.4 3.4 6-6.8\"/><g class=\"ti-orbit-s\"><path d=\"M5.38 34.75 A21.5 21.5 0 0 1 21.01 2.71\"/><path d=\"M29.56 3.23 A21.5 21.5 0 0 1 43.49 33.09\"/><path d=\"M38.39 39.98 A21.5 21.5 0 0 1 10.76 40.94\"/></g><circle cx=\"24.75\" cy=\"2.51\" r=\"2.3\"/><circle cx=\"41.17\" cy=\"36.94\" r=\"2.3\"/><circle cx=\"8.02\" cy=\"38.39\" r=\"2.3\"/>","menu":"<path d=\"M8 14h32\"/><path d=\"M8 24h20\"/><circle cx=\"34\" cy=\"24\" r=\"3\"/><path d=\"M8 34h32\"/>","search":"<circle cx=\"21\" cy=\"21\" r=\"12.5\"/><path d=\"M30.2 30.2 41 41\"/><path class=\"th\" d=\"M14.4 20.5a7 7 0 0 1 6-6\"/>","bell":"<path d=\"M24 6v3.4\"/><path d=\"M12.6 33.5V23a11.4 11.4 0 0 1 22.8 0v10.5l4 4H8.6z\"/><path d=\"M19.8 41.5a4.4 4.4 0 0 0 8.4 0\"/>","close":"<path d=\"M12 12l24 24M36 12 12 36\"/>","plus":"<path d=\"M24 11v26M11 24h26\"/>","minus":"<path d=\"M11 24h26\"/>","locate":"<circle cx=\"24\" cy=\"24\" r=\"10\"/><circle class=\"fl\" cx=\"24\" cy=\"24\" r=\"3\"/><path d=\"M24 5v8M24 35v8M5 24h8M35 24h8\"/>","refresh":"<path d=\"M8.5 24A15.5 15.5 0 0 1 36 14.3\"/><path d=\"M37 7v8.5h-8.5\"/><path d=\"M39.5 24A15.5 15.5 0 0 1 12 33.7\"/><path d=\"M11 41v-8.5h8.5\"/>","starO":"<path d=\"M24 8 L28.29 19.09 L40.17 19.75 L30.94 27.26 L33.99 38.75 L24 32.3 L14.01 38.75 L17.06 27.26 L7.83 19.75 L19.71 19.09Z\"/>","starF":"<path class=\"fl\" d=\"M24 8 L28.29 19.09 L40.17 19.75 L30.94 27.26 L33.99 38.75 L24 32.3 L14.01 38.75 L17.06 27.26 L7.83 19.75 L19.71 19.09Z\"/>","route":"<circle cx=\"11\" cy=\"37\" r=\"4.2\"/><circle cx=\"37\" cy=\"11\" r=\"4.2\"/><path d=\"M15.2 37H28a6 6 0 0 0 0-12h-8a6 6 0 0 1 0-12h12.8\"/>","chevR":"<path d=\"M18 10l14 14-14 14\"/>","chevL":"<path d=\"M30 10 16 24l14 14\"/>","auto":"<circle cx=\"24\" cy=\"24\" r=\"15\"/><path class=\"fl\" d=\"M24 9a15 15 0 0 1 0 30z\"/>","sun":"<circle cx=\"24\" cy=\"24\" r=\"8.5\"/><path d=\"M24 5v5M24 38v5M5 24h5M38 24h5M10.6 10.6l3.5 3.5M33.9 33.9l3.5 3.5M37.4 10.6l-3.5 3.5M14.1 33.9l-3.5 3.5\"/>","moon":"<path d=\"M38 28.5A15.5 15.5 0 0 1 19.5 10a15.5 15.5 0 1 0 18.5 18.5z\"/>","check":"<path d=\"M10 25l9.5 9.5L38 14\"/>"};
  /* ICONOS: un solo juego de línea fina (mapa plegable con pin, bus, parada con cartel, pin con estrella, nube de sincronización con órbita…).
     Reemplaza los iconos del HTML estático y de los textos de script.js (☆ ★ ↻ ⌖ ◎ × + −) sin tocar index.html ni script.js. Colores por CSS (claro/oscuro). */
  var GLYPH={"☆":"starO","★":"starF","↻":"refresh","⌖":"route","◎":"locate","×":"close","✕":"close","+":"plus","−":"minus"};
  function svgHtml(name,cls){return '<svg class="ti'+(cls?' '+cls:'')+'" data-ti="'+name+'" viewBox="0 0 48 48" aria-hidden="true" focusable="false">'+ICON[name]+'</svg>';}
  function setSvg(s,name){
    if(!s||!ICON[name]||s.getAttribute("data-ti")===name)return;
    s.setAttribute("viewBox","0 0 48 48");s.setAttribute("data-ti",name);s.removeAttribute("width");s.removeAttribute("height");
    s.classList.add("ti");s.innerHTML=ICON[name];
  }
  function each(sel,fn,root){[].forEach.call((root||document).querySelectorAll(sel),fn);}
  function glyphs(root){
    each("button,.map-refresh-icon,.locate-icon",function(el){
      var k=el.childNodes;if(k.length!==1||k[0].nodeType!==3)return;
      var m=k[0].nodeValue.match(/^\s*([☆★↻⌖◎×✕+−])\s*([\s\S]*)$/);if(!m||!GLYPH[m[1]])return;
      var rest=m[2].trim();
      el.innerHTML=svgHtml(GLYPH[m[1]],"tc-gl")+(rest?"<span>"+esc(rest)+"</span>":"");
    },root);
  }
  function scanIcons(root){
    root=root||document;
    each(".tc-hd-btn[data-action=menu] svg",function(s){setSvg(s,"menu");},root);
    each(".tc-hd-btn[data-action=search] svg",function(s){setSvg(s,"search");},root);
    each(".tc-hd-btn[data-action=notifications] svg",function(s){setSvg(s,"bell");},root);
    each(".tc-hd-field>svg",function(s){setSvg(s,"search");},root);
    each(".tc-hd-clear svg",function(s){setSvg(s,"close");},root);
    each(".tc-hd-item[data-section]",function(b){setSvg(b.querySelector(".tc-hd-item-ico svg"),b.getAttribute("data-section"));},root);
    each(".tc-hd-item-go,.tc-card-go,.lc-go",function(s){setSvg(s,"chevR");},root);
    each(".tc-hd-seg button[data-theme-mode]",function(b){setSvg(b.querySelector("svg"),{auto:"auto",light:"sun",dark:"moon"}[b.getAttribute("data-theme-mode")]);},root);
    each(".tc-nav-item[data-section]",function(b){setSvg(b.querySelector(".tc-nav-ico svg"),b.getAttribute("data-section"));},root);
    each(".tc-card[data-section]",function(b){setSvg(b.querySelector(".tc-card-ico svg"),b.getAttribute("data-section"));},root);
    each(".tc-sync .tc-card-ico svg.sy",function(s){setSvg(s,"sync");},root);
    each(".tc-sync .tc-card-ico svg.ck",function(s){setSvg(s,"syncok");},root);
    each(".tc-back svg",function(s){setSvg(s,"chevL");},root);
    glyphs(root);
  }
  /* Título del header ANIMADO: cada letra sube con rebote al abrir y luego una onda de luz las recorre una a una; la etiqueta "2.0" respira. */
  function splitTitle(){
    var h=document.querySelector(".tc-hd-title.tc-wm");if(!h||h.getAttribute("data-tcsplit"))return;
    var t="TU COLECTIVO",html="",k,ch;
    for(k=0;k<t.length;k++){ch=t.charAt(k);html+='<span class="tc-wl" style="--i:'+k+'">'+(ch===" "?"&nbsp;":ch)+"</span>";}
    h.innerHTML='<span class="tc-wtxt" aria-hidden="true">'+html+'</span><span class="tc-wver" aria-hidden="true">2.0</span>';
    h.setAttribute("data-tcsplit","1");
  }
  var ROUTE=svgHtml("lineas");
  /* PARCHE: script.js (arribos de PARADAS CERCANAS) usa esc(destination) fuera de su alcance y la pantalla quedaba en "CONSULTANDO ARRIBOS_" con un ReferenceError.
     Estos respaldos globales evitan los errores sin tocar script.js (la corrección definitiva está en el LEEME). */
  if(typeof window.esc!=="function")window.esc=function(v){return esc(v);};
  if(typeof window.destination==="undefined")window.destination="";
  if(typeof window.destinations==="undefined")window.destinations=[];
  var GO=svgHtml("chevR","lc-go");
  function isArriving(status,m){var s=norm(status);return /^(ARRIBANDO|LLEGANDO|EN PARADA|PROXIMO|PROXIMA)$/.test(s)||(typeof m==="number"&&isFinite(m)&&m<=1);}
  function arrivalDuration(m){return(typeof m==="number"&&isFinite(m))?Math.max(1.2,Math.min(18,m*0.35)):8;}
  function lineCardHtml(l,i){
    /* Muestra la identificación PÚBLICA (l.name, p. ej. A, BCG, 110). El código interno de SmartMove (l.code) solo viaja en data-line. */
    var label=String(l.name==null||l.name===""?l.code:l.name).trim(),r=l.routes;
    var meta=(r==null||r==="")?"":'<span class="lc-meta">'+esc(r)+(Number(r)===1?" RUTA":" RUTAS")+'</span>';
    return '<button type="button" class="line-card tc-enter" data-line="'+esc(l.code)+'" aria-label="LÍNEA '+esc(label)+'" style="--n:'+(i|0)+'"><span class="lc-badge"><span class="tc-hx"></span><i class="tc-hxf"></i>'+ROUTE+'</span><span class="lc-body"><span class="lc-k">LÍNEA</span><span class="lc-name'+(label.length>5?" lc-long":"")+'">'+esc(label)+'</span>'+meta+'</span>'+GO+'</button>';
  }
  function detailCardHtml(it,i,tag){
    return '<button type="button" class="data-card line-detail-card tc-enter" data-card="'+esc(it.code)+'" style="--n:'+(i|0)+'"><span class="ld-node"></span><span class="ld-name">'+esc(it.name)+'</span>'+(tag?'<span class="ld-tag">'+esc(tag)+'</span>':'')+GO+'</button>';
  }
  /* Contador de minutos AVANZADO: disco de cristal, 60 marcas de precisión (12 mayores), anillo orbital, arco con degradado y nodo luminoso que gira,
     anillo punteado que gira al revés y onda de pulso. Solo decorativo: ninguna animación calcula ni resta minutos. */
  var RID=0;
  function ringSvg(){
    var u="tcr"+(++RID);
    return '<svg class="arrival-ring" viewBox="0 0 100 100" aria-hidden="true" focusable="false"><defs>'
      +'<linearGradient id="'+u+'a" gradientUnits="userSpaceOnUse" x1="12" y1="88" x2="88" y2="12"><stop offset="0" class="rg-s0"/><stop offset="1" class="rg-s1"/></linearGradient>'
      +'<radialGradient id="'+u+'d" cx="50%" cy="50%" r="50%"><stop offset="0" class="rg-d0"/><stop offset="1" class="rg-d1"/></radialGradient></defs>'
      +'<circle class="rg-disc" cx="50" cy="50" r="40" fill="url(#'+u+'d)"/>'
      +'<circle class="rg-tick" cx="50" cy="50" r="46"/><circle class="rg-tick-major" cx="50" cy="50" r="46"/>'
      +'<circle class="rg-orbit" cx="50" cy="50" r="41.5"/>'
      +'<circle class="rg-dash" cx="50" cy="50" r="33"/>'
      +'<g class="arrival-ring-orbit"><circle class="rg-arc" cx="50" cy="50" r="37.5" stroke="url(#'+u+'a)"/><circle class="rg-head-halo" cx="87.5" cy="50" r="5.5"/><circle class="rg-head" cx="87.5" cy="50" r="2.6"/></g>'
      +'</svg>';
  }
  function arrivalCardHtml(a,i,selected){
    var m=(a.minutes==null||a.minutes==="")?null:Number(a.minutes),arr=isArriving(a.status,m),dest=a.destination&&String(a.destination).trim()?a.destination:"DESTINO NO INFORMADO";
    var time=arr?'<span class="arrival-time"><span class="arrival-arriving">ARRIBANDO</span></span>'
      :'<span class="arrival-time" style="--arrival-duration:'+arrivalDuration(m)+'s">'+ringSvg()+'<span class="arrival-num"><b class="arrival-n"'+(m==null?"":' data-m="'+esc(m)+'"')+'>'+(m==null?"--":esc(m))+'</b><small>MIN</small></span></span>';
    return '<div class="arrival-card tc-enter'+(arr?" arriving":"")+(selected?" selected":"")+'" role="button" tabindex="0" aria-pressed="'+(selected?"true":"false")+'" data-dest="'+esc(dest)+'" style="--n:'+(i|0)+'"><div class="arrival-copy"><div class="arrival-row"><span class="arrival-k">LÍNEA</span><span class="arrival-v">'+esc(a.line)+'</span></div><div class="arrival-row"><span class="arrival-k">HACIA</span><span class="arrival-v">'+esc(dest)+'</span></div></div>'+time+'</div>';
  }
  function stateHtml(kind,text){
    var cls=kind==="loading"?"is-loading":kind==="error"?"is-error":"is-empty";
    return '<div class="tc-state '+cls+'" role="status">'+(kind==="loading"?'<span class="tc-scan"></span>':'')+'<span>'+esc(text)+'</span></div>';
  }
  function animateNumbers(root){
    var reduce=window.matchMedia&&window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    [].forEach.call((root||document).querySelectorAll(".arrival-n[data-m]"),function(n){
      var to=Number(n.getAttribute("data-m"));if(!isFinite(to)||reduce||to<=0)return;
      var t0=performance.now(),D=650;
      (function f(now){var t=Math.min(1,(now-t0)/D);n.textContent=Math.round(to*(1-Math.pow(1-t,3)));if(t<1)requestAnimationFrame(f);else n.textContent=to;})(t0);
    });
  }
  var SEL=".line-card,.lineas-grid>[data-line],.line-detail-card,.arrival-card,.tc-btn,.tc-back,.nearby-stop,.favorite-item,.locate-stops,.map-refresh-card,.notification-toggle,.arrival-favorite-toggle,.map-popup-line,.map-route-action,.map-controls button,.favorite-remove,.map-popup-close";
  function bindPress(root){
    root=root||document.body;
    function el(t){var c=t&&t.closest?t.closest(SEL):null;return c&&root.contains(c)&&!c.disabled?c:null;}
    function cool(c,ms){setTimeout(function(){if(!c.matches(":focus-visible"))c.classList.remove("hot");},ms);}
    root.addEventListener("pointerdown",function(e){var c=el(e.target);if(c)c.classList.add("hot","press");});
    function up(e){var c=el(e.target);[].forEach.call(root.querySelectorAll(".press"),function(k){k.classList.remove("press");});if(c&&e.pointerType!=="mouse")cool(c,420);}
    root.addEventListener("pointerup",up);root.addEventListener("pointercancel",up);
    root.addEventListener("pointerover",function(e){if(e.pointerType!=="mouse")return;var c=el(e.target);if(c)c.classList.add("hot");});
    root.addEventListener("pointerout",function(e){if(e.pointerType!=="mouse")return;var c=el(e.target);if(c&&!c.contains(e.relatedTarget)&&!c.matches(":focus-visible"))c.classList.remove("hot");});
    root.addEventListener("focusin",function(e){var c=el(e.target);if(c&&c.matches(":focus-visible"))c.classList.add("hot");});
    root.addEventListener("focusout",function(e){var c=el(e.target);if(c)c.classList.remove("hot");});
  }
  /* Efecto de pulsación (onda táctil + vibración leve) en tarjetas, botones, pestañas y menús. */
  function ripple(root){
    var HOST=".tc-card,.line-card,.line-detail-card,.arrival-card,.tc-btn,.tc-back,.nearby-stop,.favorite-item,.locate-stops,.map-refresh-card,.notification-toggle,.arrival-favorite-toggle,.tc-hd-btn,.tc-hd-item,.map-controls button,.tc-hd-fchip,.favorite-remove,.map-popup-line,.tc-nav-item";
    root.addEventListener("pointerdown",function(e){
      var h=e.target&&e.target.closest?e.target.closest(HOST):null;if(!h||h.disabled)return;
      var box=h.classList.contains("tc-nav-item")?h.querySelector(".tc-nav-ico"):h;if(!box)return;
      var r=box.getBoundingClientRect(),d=Math.max(r.width,r.height)*2.2,s=document.createElement("span");
      s.className="tc-ripple";s.style.width=s.style.height=d+"px";s.style.left=(e.clientX-r.left-d/2)+"px";s.style.top=(e.clientY-r.top-d/2)+"px";
      box.appendChild(s);setTimeout(function(){if(s.parentNode)s.parentNode.removeChild(s);},700);
      try{if(navigator.vibrate)navigator.vibrate(8);}catch(_){}
    },{passive:true});
  }
  window.TcLineas={bindPress:bindPress,lineCardHtml:lineCardHtml,detailCardHtml:detailCardHtml,arrivalCardHtml:arrivalCardHtml,stateHtml:stateHtml,ringSvg:ringSvg,ripple:ripple,icons:{html:svgHtml,scan:scanIcons,set:ICON},animateNumbers:animateNumbers,arrivalDuration:arrivalDuration,isArriving:isArriving};
  /* Activar una sola vez los estados visuales de interacción para las tarjetas presentes y futuras de la sección Líneas. */
  if(document&&document.body){bindPress(document.body);if(!window.__tcRipple){window.__tcRipple=true;ripple(document.body);}splitTitle();if(!window.__tcIcons){window.__tcIcons=true;scanIcons(document);var pend=0;new MutationObserver(function(){if(pend)return;pend=requestAnimationFrame(function(){pend=0;scanIcons(document);});}).observe(document.body,{childList:true,subtree:true,characterData:true});}}
})();