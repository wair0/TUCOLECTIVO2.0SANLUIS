/* TU COLECTIVO 2.0 - Header + menus contextuales: logica visual minima (vanilla JS, offline).
   API (window.TcHeader):
     setSection("inicio|lineas|mapa|paradas|favoritos")  mueve el haz, marca el item actual del menu y actualiza el chip "SECCIÓN: X"
     setStatus("TEXTO")          cambia el texto del chip con animacion
     setAlerts(n)                fuerza el numero de alertas (el punto de la campana se oculta con 0)
     setNotifications([{id,title,text,time,unread}])   carga la lista de la campana (el punto se calcula con las no leidas)
     closeAll()                  cierra menus y desactiva botones (sin disparar callbacks)
     setTheme("auto|light|dark", persistir=true)   aplica el tema (atributo data-theme en <html>) y actualiza el selector del menu. No dispara onThemeChange.
     getTheme()                  devuelve {mode:"auto|light|dark", theme:"light|dark"}
   Callbacks (o eventos en #tc-hd, bubbles):
     onThemeChange(modo, tema)        evento tc:themechange    {mode, theme}  (al tocar el selector o si el sistema cambia estando en AUTO)
     onAction(accion, abierto)        evento tc:headeraction   accion: "menu" | "search" | "notifications"
     onMenuSelect(seccion)            evento tc:menuselect     {section}
     onSearchInput(consulta, filtro)  evento tc:searchinput    {query, filter}  (al escribir o cambiar filtro)
     onSearch(consulta, filtro)       evento tc:search         {query, filter}  (al presionar Enter; cierra el panel)
     onNotification(id)               evento tc:notification   {id}
     onMarkAllRead()                  evento tc:marknotifications
   Filtros: todo | lineas | calles | paradas | favoritos */
(function(){
  var hd=document.getElementById("tc-hd");if(!hd)return;
  var scrim=document.getElementById("tc-hd-scrim"),st=document.getElementById("tc-hd-status");
  var btns=[].slice.call(hd.querySelectorAll(".tc-hd-btn")),panels={},menuItems=[].slice.call(hd.querySelectorAll(".tc-hd-item"));
  [].forEach.call(hd.querySelectorAll(".tc-hd-panel"),function(p){panels[p.getAttribute("data-for")]=p;});
  var input=document.getElementById("tc-hd-input"),clr=document.getElementById("tc-hd-clear"),chipsEl=document.getElementById("tc-hd-chips"),
      notesEl=document.getElementById("tc-hd-notes"),markBtn=document.getElementById("tc-hd-markread");
  var SEC={inicio:"INICIO",lineas:"LÍNEAS",mapa:"MAPA",paradas:"PARADAS",favoritos:"FAVORITOS"},KEYS=Object.keys(SEC);
  var filter="todo",notes=[],openName=null;
  function emit(n,d){hd.dispatchEvent(new CustomEvent(n,{bubbles:true,detail:d}));}
  function call(fn){if(typeof api[fn]==="function"){api[fn].apply(api,[].slice.call(arguments,1));}}
  function setStatus(t){st.classList.remove("swap");void st.offsetWidth;st.textContent=t;st.classList.add("swap");}
  function closeAll(){
    btns.forEach(function(b){b.classList.remove("active");b.setAttribute("aria-expanded","false");});
    Object.keys(panels).forEach(function(k){panels[k].classList.remove("open");});
    if(scrim)scrim.classList.remove("open");
    openName=null;
  }
  function fireAction(a,open){call("onAction",a,open);emit("tc:headeraction",{action:a,open:open});}
  function openPanel(name){
    var b=hd.querySelector('.tc-hd-btn[data-action="'+name+'"]');
    closeAll();openName=name;
    if(b){b.classList.add("active");b.setAttribute("aria-expanded","true");}
    if(panels[name])panels[name].classList.add("open");
    if(scrim)scrim.classList.add("open");
    if(name==="search"&&input)setTimeout(function(){if(openName==="search")input.focus();},200);
  }
  function dismiss(returnFocus){
    var was=openName;if(!was)return;
    var b=hd.querySelector('.tc-hd-btn[data-action="'+was+'"]');
    closeAll();fireAction(was,false);
    if(returnFocus&&b)b.focus();
  }
  /* ---- notificaciones ---- */
  function unreadCount(){return notes.filter(function(n){return n.unread;}).length;}
  function renderNotes(){
    notesEl.textContent="";
    if(!notes.length){var e=document.createElement("div");e.className="tc-hd-empty";e.textContent="SIN NOTIFICACIONES";notesEl.appendChild(e);}
    notes.forEach(function(n,i){
      var b=document.createElement("button");b.type="button";b.className="tc-hd-note"+(n.unread?" unread":"");b.setAttribute("data-id",n.id);b.style.setProperty("--n",i);
      var dot=document.createElement("i");dot.className="tc-hd-ndot";
      var body=document.createElement("div");body.className="tc-hd-nbody";
      var head=document.createElement("div");head.className="tc-hd-nhead";
      var t=document.createElement("span");t.className="tc-hd-ntitle";t.textContent=n.title;
      var tm=document.createElement("span");tm.className="tc-hd-ntime";tm.textContent=n.time;
      head.appendChild(t);head.appendChild(tm);
      var tx=document.createElement("p");tx.className="tc-hd-ntext";tx.textContent=n.text;
      body.appendChild(head);body.appendChild(tx);b.appendChild(dot);b.appendChild(body);notesEl.appendChild(b);
    });
    var u=unreadCount();hd.setAttribute("data-alerts",String(u));markBtn.disabled=u===0;
  }
  var api=window.TcHeader={
    onAction:null,onMenuSelect:null,onSearchInput:null,onSearch:null,onNotification:null,onMarkAllRead:null,
    setSection:function(n){var i=KEYS.indexOf(n);if(i<0)return;hd.style.setProperty("--i",i);hd.setAttribute("data-section",n);
      menuItems.forEach(function(m){m.classList.toggle("current",m.getAttribute("data-section")===n);});setStatus("SECCIÓN: "+SEC[n]);},
    setStatus:setStatus,
    setAlerts:function(n){hd.setAttribute("data-alerts",String(Math.max(0,n|0)));},
    setNotifications:function(list){notes=(list||[]).map(function(n,i){return{id:String(n.id!=null?n.id:i),title:String(n.title||""),text:String(n.text||""),time:String(n.time||""),unread:!!n.unread};});renderNotes();},
    closeAll:closeAll,
    onThemeChange:null,
    setTheme:function(m,persist){setTheme(m,false,persist!==false);},
    getTheme:function(){return{mode:mode,theme:resolved()};}
  };
  /* ---- botones del header ---- */
  hd.addEventListener("click",function(e){
    var b=e.target.closest(".tc-hd-btn");if(!b)return;
    var a=b.getAttribute("data-action");
    if(b.classList.contains("active")){dismiss(false);}else{openPanel(a);fireAction(a,true);}
  });
  if(scrim)scrim.addEventListener("click",function(){dismiss(false);});
  document.addEventListener("keydown",function(e){if(e.key==="Escape"&&openName)dismiss(true);});
  document.addEventListener("pointerdown",function(e){if(openName&&!hd.contains(e.target))dismiss(false);},true);
  /* ---- menu hamburguesa ---- */
  panels.menu.addEventListener("click",function(e){
    var it=e.target.closest(".tc-hd-item");if(!it)return;
    var s=it.getAttribute("data-section");closeAll();api.setSection(s);
    call("onMenuSelect",s);emit("tc:menuselect",{section:s});fireAction("menu",false);
  });
  /* ---- lupa ---- */
  function syncClear(){clr.classList.toggle("show",input.value.length>0);}
  function searchInput(){syncClear();call("onSearchInput",input.value,filter);emit("tc:searchinput",{query:input.value,filter:filter});}
  input.addEventListener("input",searchInput);
  input.addEventListener("keydown",function(e){
    if(e.key==="Enter"){e.preventDefault();var q=input.value.trim();input.blur();closeAll();
      call("onSearch",q,filter);emit("tc:search",{query:q,filter:filter});fireAction("search",false);}
  });
  clr.addEventListener("click",function(){input.value="";searchInput();input.focus();});
  chipsEl.addEventListener("click",function(e){
    var c=e.target.closest(".tc-hd-fchip");if(!c)return;filter=c.getAttribute("data-filter");
    [].forEach.call(chipsEl.children,function(x){var on=x===c;x.classList.toggle("on",on);x.setAttribute("aria-pressed",on?"true":"false");});
    searchInput();
  });
  /* ---- campana ---- */
  notesEl.addEventListener("click",function(e){
    var b=e.target.closest(".tc-hd-note");if(!b)return;var id=b.getAttribute("data-id");
    notes.forEach(function(n){if(n.id===id)n.unread=false;});renderNotes();closeAll();
    call("onNotification",id);emit("tc:notification",{id:id});fireAction("notifications",false);
  });
  markBtn.addEventListener("click",function(){
    notes.forEach(function(n){n.unread=false;});renderNotes();
    call("onMarkAllRead");emit("tc:marknotifications",{});
  });
  /* ---- tema claro / oscuro (selector del menu hamburguesa) ---- */
  var THEME_KEY="tc-theme",MODES=["auto","light","dark"],root=document.documentElement,mode="auto",
      mqD=window.matchMedia?window.matchMedia("(prefers-color-scheme: dark)"):null,
      themeEl=document.getElementById("tc-hd-theme"),nowEl=document.getElementById("tc-hd-theme-now"),
      segBtns=[].slice.call(themeEl.querySelectorAll("[data-theme-mode]"));
  function resolved(){return mode==="auto"?(mqD&&mqD.matches?"dark":"light"):mode;}
  function paintTheme(){
    themeEl.style.setProperty("--t",MODES.indexOf(mode));
    segBtns.forEach(function(b){var on=b.getAttribute("data-theme-mode")===mode;b.classList.toggle("on",on);b.setAttribute("aria-checked",on?"true":"false");b.tabIndex=on?0:-1;});
    nowEl.textContent=(mode==="auto"?"AUTO \u00B7 ":"")+(resolved()==="dark"?"OSCURO":"CLARO");
  }
  function setTheme(m,notify,persist){
    if(MODES.indexOf(m)<0)return;
    var changed=m!==mode;mode=m;
    root.setAttribute("data-theme",resolved());
    paintTheme();
    if(persist){try{localStorage.setItem(THEME_KEY,m);}catch(e){}}
    if(notify&&changed){call("onThemeChange",m,resolved());emit("tc:themechange",{mode:m,theme:resolved()});}
  }
  (function(){
    var saved=null;try{saved=localStorage.getItem(THEME_KEY);}catch(e){}
    var attr=root.getAttribute("data-theme");
    mode=MODES.indexOf(saved)>=0?saved:(attr==="light"||attr==="dark"?attr:"auto");
    root.setAttribute("data-theme",resolved());
    paintTheme();
  })();
  themeEl.addEventListener("click",function(e){var b=e.target.closest("[data-theme-mode]");if(b)setTheme(b.getAttribute("data-theme-mode"),true,true);});
  themeEl.addEventListener("keydown",function(e){
    var i=segBtns.indexOf(document.activeElement);if(i<0)return;
    if(e.key==="ArrowRight")i=(i+1)%3;else if(e.key==="ArrowLeft")i=(i+2)%3;else return;
    segBtns[i].focus();setTheme(segBtns[i].getAttribute("data-theme-mode"),true,true);e.preventDefault();
  });
  if(mqD){var onSys=function(){if(mode==="auto"){root.setAttribute("data-theme",resolved());paintTheme();call("onThemeChange","auto",resolved());emit("tc:themechange",{mode:"auto",theme:resolved()});}};
    if(mqD.addEventListener)mqD.addEventListener("change",onSys);else if(mqD.addListener)mqD.addListener(onSys);}
  renderNotes();
})();