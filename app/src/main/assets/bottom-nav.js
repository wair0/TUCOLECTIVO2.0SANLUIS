/* TU COLECTIVO 2.0 - Barra inferior: logica visual minima (vanilla JS, offline).
   - Al tocar una pestaña: mueve el haz, activa el hexagono y llama a TcBottomNav.onChange(seccion) (si existe)
     y dispara el evento "tc:navchange" (detail.section) sobre #tc-nav.
   - La app puede sincronizar el estado: TcBottomNav.setActive("mapa")  (no dispara onChange).
   - Secciones: inicio | lineas | mapa | paradas | favoritos */
(function(){
  var nav=document.getElementById("tc-nav");if(!nav)return;
  var items=[].slice.call(nav.querySelectorAll(".tc-nav-item"));
  function setActive(name,notify){
    var idx=-1;
    items.forEach(function(b,i){var on=b.getAttribute("data-section")===name;if(on)idx=i;
      b.classList.toggle("active",on);b.setAttribute("aria-selected",on?"true":"false");b.tabIndex=on?0:-1;});
    if(idx<0)return;
    nav.style.setProperty("--i",idx);nav.setAttribute("data-active",name);
    if(notify){
      if(typeof api.onChange==="function")api.onChange(name);
      nav.dispatchEvent(new CustomEvent("tc:navchange",{bubbles:true,detail:{section:name}}));
    }
  }
  var api=window.TcBottomNav={onChange:null,setActive:function(n){setActive(n,false)},getActive:function(){return nav.getAttribute("data-active")}};
  nav.addEventListener("click",function(e){var b=e.target.closest(".tc-nav-item");if(b&&b.getAttribute("data-section")!==api.getActive())setActive(b.getAttribute("data-section"),true);});
  nav.addEventListener("keydown",function(e){
    var i=items.indexOf(document.activeElement);if(i<0)return;
    if(e.key==="ArrowRight")i=(i+1)%items.length;else if(e.key==="ArrowLeft")i=(i-1+items.length)%items.length;else return;
    items[i].focus();setActive(items[i].getAttribute("data-section"),true);e.preventDefault();
  });
})();