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
  var ROUTE='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8,6 H15 A3,3 0 0 1 15,12 H9 A3,3 0 0 0 9,18 H16"/><circle cx="5" cy="6" r="1.6"/><circle cx="19" cy="18" r="1.6"/></svg>';
  /* PARCHE: script.js (arribos de PARADAS CERCANAS) usa esc(destination) fuera de su alcance y la pantalla quedaba en "CONSULTANDO ARRIBOS_" con un ReferenceError.
     Estos respaldos globales evitan los errores sin tocar script.js (la corrección definitiva está en el LEEME). */
  if(typeof window.esc!=="function")window.esc=function(v){return esc(v);};
  if(typeof window.destination==="undefined")window.destination="";
  if(typeof window.destinations==="undefined")window.destinations=[];
  var GO='<svg class="lc-go" viewBox="0 0 24 24" aria-hidden="true"><path d="M9,5 L16,12 L9,19"/></svg>';
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
  /* Anillo del contador (estilo corporativo): pista + arco de acento que gira suave. Solo decorativo: no resta ni calcula minutos. */
  function ringSvg(){
    return '<span class="arrival-hud" aria-hidden="true"><span class="arrival-hud-orbit"></span><span class="arrival-hud-grid"></span><span class="arrival-hud-core"></span></span>';
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
  window.TcLineas={bindPress:bindPress,lineCardHtml:lineCardHtml,detailCardHtml:detailCardHtml,arrivalCardHtml:arrivalCardHtml,stateHtml:stateHtml,ringSvg:ringSvg,animateNumbers:animateNumbers,arrivalDuration:arrivalDuration,isArriving:isArriving};
  /* Activar una sola vez los estados visuales de interacción para las tarjetas presentes y futuras de la sección Líneas. */
  if(document&&document.body)bindPress(document.body);
})();