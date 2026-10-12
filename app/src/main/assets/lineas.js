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
  var GO='<svg class="lc-go" viewBox="0 0 24 24" aria-hidden="true"><path d="M9,5 L16,12 L9,19"/></svg>';
  function isArriving(status,m){var s=norm(status);return /^(ARRIBANDO|LLEGANDO|EN PARADA|PROXIMO|PROXIMA)$/.test(s)||(typeof m==="number"&&isFinite(m)&&m<=1);}
  function arrivalDuration(m){return(typeof m==="number"&&isFinite(m))?Math.max(1.2,Math.min(18,m*0.35)):8;}
  function lineCardHtml(l,i){
    var r=l.routes,meta=(r==null)?"":'<span class="lc-meta">'+esc(r)+(Number(r)===1?" RUTA":" RUTAS")+'</span>';
    return '<button type="button" class="line-card tc-enter" data-line="'+esc(l.code)+'" style="--n:'+(i|0)+'"><span class="lc-badge"><span class="tc-hx"></span><i class="tc-hxf"></i><b>'+esc(l.code)+'</b></span><span class="lc-body"><span class="lc-name">'+esc(l.name)+'</span>'+meta+'</span>'+GO+'</button>';
  }
  function detailCardHtml(it,i,tag){
    return '<button type="button" class="data-card line-detail-card tc-enter" data-card="'+esc(it.code)+'" style="--n:'+(i|0)+'"><span class="ld-node"></span><span class="ld-name">'+esc(it.name)+'</span>'+(tag?'<span class="ld-tag">'+esc(tag)+'</span>':'')+GO+'</button>';
  }
  function arrivalCardHtml(a,i,selected){
    var m=(a.minutes==null||a.minutes==="")?null:Number(a.minutes),arr=isArriving(a.status,m),dest=a.destination&&String(a.destination).trim()?a.destination:"DESTINO NO INFORMADO";
    var time=arr?'<span class="arrival-time"><span class="arrival-arriving">ARRIBANDO</span></span>'
      :'<span class="arrival-time" style="--arrival-duration:'+arrivalDuration(m)+'s"><svg class="arrival-ring" viewBox="0 0 80 80" aria-hidden="true"><circle class="ar-base" cx="40" cy="40" r="35"/><g class="arrival-ring-orbit"><circle class="ar-arc" cx="40" cy="40" r="35"/><circle class="ar-dot" cx="38.8" cy="75" r="3"/></g></svg><span class="arrival-num"><b class="arrival-n"'+(m==null?"":' data-m="'+esc(m)+'"')+'>'+(m==null?"--":esc(m))+'</b><small>MIN</small></span></span>';
    return '<div class="arrival-card tc-enter'+(arr?" arriving":"")+(selected?" selected":"")+'" role="button" tabindex="0" aria-pressed="'+(selected?"true":"false")+'" data-dest="'+esc(dest)+'" style="--n:'+(i|0)+'"><div class="arrival-copy"><div class="arrival-row"><span class="arrival-k">LINEA</span><span class="arrival-v">'+esc(a.line)+'</span></div><div class="arrival-row"><span class="arrival-k">HACIA</span><span class="arrival-v">'+esc(dest)+'</span></div></div>'+time+'</div>';
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
  var SEL=".line-card,.lineas-grid>[data-line],.line-detail-card,.arrival-card,.tc-btn,.tc-back";
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
  window.TcLineas={bindPress:bindPress,lineCardHtml:lineCardHtml,detailCardHtml:detailCardHtml,arrivalCardHtml:arrivalCardHtml,stateHtml:stateHtml,animateNumbers:animateNumbers,arrivalDuration:arrivalDuration,isArriving:isArriving};
  /* Activar una sola vez los estados visuales de interacción para las tarjetas presentes y futuras de la sección Líneas. */
  if(document&&document.body)bindPress(document.body);
})();