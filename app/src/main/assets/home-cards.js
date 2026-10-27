/* TU COLECTIVO 2.0 - Tarjetas de inicio + SINCRONIZAR LINEAS: logica visual minima (vanilla JS, offline).
   Tarjetas de navegacion (data-section): mapa | lineas | paradas | favoritos   (mismos nombres que TcBottomNav / TcHeader)
   API (window.TcHomeCards):
     onSelect = function(seccion){...}     se llama al tocar una tarjeta de navegacion (tras ~140 ms para que se vea el efecto)
     setBadge("favoritos", 3)               contador opcional en la tarjeta (0 / null lo oculta)
     replay()                               repite la animacion de entrada (util al volver a INICIO)
   SINCRONIZAR LINEAS (tarjeta #tc-sync): cada pulsacion lleva la barra de 0 a 100 %.
     onSync = function(){...}               se llama al iniciar: aqui lanza la sincronizacion real (p. ej. loadLines())
     modo AUTOMATICO (por defecto): la barra corre sola de 0 a 100 % en ~3,2 s.
     modo REAL: pon TcHomeCards.syncManual = true y reporta el avance verdadero desde nativo:
        TcHomeCards.sync.progress(0..100) | TcHomeCards.sync.done() | TcHomeCards.sync.fail()
     TcHomeCards.sync.start() tambien inicia por codigo. Mientras corre, nuevas pulsaciones se ignoran.
   Eventos: "tc:cardselect" (detail.section) y "tc:sync" (detail.action = start | done | fail) sobre #tc-cards. */
(function(){
  var root=document.getElementById("tc-cards");if(!root)return;
  var cards=[].slice.call(root.querySelectorAll(".tc-card"));
  var reduce=!!(window.matchMedia&&window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  var timers=new WeakMap();
  function find(sec){for(var i=0;i<cards.length;i++)if(cards[i].getAttribute("data-section")===sec)return cards[i];return null;}
  function cardOf(t){var c=t&&t.closest?t.closest(".tc-card"):null;return c&&root.contains(c)?c:null;}
  function busy(c){return c.classList.contains("run");}
  function hot(c,on){if(!c)return;clearTimeout(timers.get(c));if(!on&&busy(c))return;c.classList.toggle("hot",on);}
  function cool(c,ms){if(!c)return;clearTimeout(timers.get(c));timers.set(c,setTimeout(function(){if(!c.matches(":focus-visible")&&!busy(c))c.classList.remove("hot");},ms));}
  var sync=document.getElementById("tc-sync");
  var pctEl=sync&&sync.querySelector(".tc-sync-pct"),trk=sync&&sync.querySelector(".tc-sync-track");
  var S={state:"idle",p:0,raf:0,t0:0,timer:0,manual:false},DUR=3200;
  var api=window.TcHomeCards={
    onSelect:null,onSync:null,syncManual:false,
    setBadge:function(sec,n){var c=find(sec);if(!c)return;var b=c.querySelector(".tc-card-count");if(!b)return;n=parseInt(n,10)||0;b.textContent=n>99?"99+":String(n);b.hidden=n<=0;},
    replay:function(){root.classList.remove("in");void root.offsetWidth;root.classList.add("in");}
  };
  function emit(a){root.dispatchEvent(new CustomEvent("tc:sync",{bubbles:true,detail:{action:a}}));}
  function setP(v){S.p=Math.max(0,Math.min(100,v));sync.style.setProperty("--p",S.p.toFixed(1));var r=Math.round(S.p);pctEl.textContent=r+"%";trk.setAttribute("aria-valuenow",r);}
  function stop(){cancelAnimationFrame(S.raf);clearTimeout(S.timer);}
  function tick(now){if(S.state!=="run"||S.manual)return;var t=Math.min(1,(now-S.t0)/DUR);setP(100*t*t*(3-2*t));if(t>=1)end(true);else S.raf=requestAnimationFrame(tick);}
  function start(){
    if(!sync||S.state!=="idle")return false;
    stop();sync.classList.remove("done","fail","drain");sync.classList.add("run","hot");
    S.state="run";S.manual=!!api.syncManual;setP(0);S.t0=performance.now();
    if(!S.manual)S.raf=requestAnimationFrame(tick);
    emit("start");if(typeof api.onSync==="function")api.onSync();
    return true;
  }
  function end(ok){
    if(S.state!=="run")return;stop();S.state=ok?"done":"fail";
    if(ok)setP(100);else pctEl.textContent="ERROR";
    sync.classList.remove("run");sync.classList.add(ok?"done":"fail");emit(ok?"done":"fail");
    S.timer=setTimeout(reset,ok?1500:2000);
  }
  function reset(){
    sync.classList.add("drain");sync.classList.remove("done","fail","hot");setP(0);
    S.timer=setTimeout(function(){sync.classList.remove("drain");S.state="idle";},380);
  }
  api.sync={start:start,
    progress:function(n){if(S.state!=="run")return;S.manual=true;cancelAnimationFrame(S.raf);n=parseFloat(n)||0;setP(n);if(n>=100)end(true);},
    done:function(){if(S.state==="run"){setP(100);end(true);}},
    fail:function(){end(false);}};
  function fire(c){
    if(c===sync){start();return;}
    c.classList.remove("fire");void c.offsetWidth;c.classList.add("fire");
    setTimeout(function(){c.classList.remove("fire");},800);
    var sec=c.getAttribute("data-section");
    setTimeout(function(){
      if(typeof api.onSelect==="function")api.onSelect(sec);
      root.dispatchEvent(new CustomEvent("tc:cardselect",{bubbles:true,detail:{section:sec}}));
    },reduce?0:140);
  }
  root.addEventListener("pointerdown",function(e){var c=cardOf(e.target);if(!c||c===sync)return;hot(c,true);c.classList.add("press");});
  function release(e){var c=cardOf(e.target);cards.forEach(function(k){k.classList.remove("press");});if(c&&c!==sync&&e.pointerType!=="mouse")cool(c,450);}
  root.addEventListener("pointerup",release);root.addEventListener("pointercancel",release);
  root.addEventListener("pointerover",function(e){if(e.pointerType!=="mouse")return;var c=cardOf(e.target);if(c&&c!==sync)hot(c,true);});
  root.addEventListener("pointerout",function(e){if(e.pointerType!=="mouse")return;var c=cardOf(e.target);if(c&&c!==sync&&!c.contains(e.relatedTarget)&&!c.matches(":focus-visible"))hot(c,false);});
  root.addEventListener("focusin",function(e){var c=cardOf(e.target);if(c&&c!==sync&&c.matches(":focus-visible"))hot(c,true);});
  root.addEventListener("focusout",function(e){var c=cardOf(e.target);if(c&&c!==sync)hot(c,false);});
  root.addEventListener("click",function(e){var c=cardOf(e.target);if(c)fire(c);});
  root.addEventListener("keydown",function(e){
    var i=cards.indexOf(document.activeElement);if(i<0)return;
    var n=i;if(e.key==="ArrowRight")n=Math.min(i+1,cards.length-1);else if(e.key==="ArrowLeft")n=Math.max(i-1,0);
    else if(e.key==="ArrowDown")n=Math.min(i+2,cards.length-1);else if(e.key==="ArrowUp")n=Math.max(i-2,0);else return;
    cards[n].focus();e.preventDefault();
  });
  api.replay();
})();
