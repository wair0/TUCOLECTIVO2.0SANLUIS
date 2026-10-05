/* TU COLECTIVO 2.0 — Fase 2: navegación + Header cyberpunk */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.prototype.slice.call(r.querySelectorAll(s));
  const emit = (name, detail) => document.dispatchEvent(new CustomEvent(name, { detail }));
  const triggers = $$('[data-menu]');
  const panels = $$('.panel');
  const status = $('#status');
  const q = $('#q');
  const screens = $$('.screen');
  const navButtons = $$('.nav-btn');
  let current = null;
  let filter = 'todo';
  let currentScreen = 'inicio';

  function closeAll() {
    const wasOpen = current !== null;
    panels.forEach(p => p.classList.remove('open'));
    triggers.forEach(b => { b.classList.remove('on'); b.setAttribute('aria-expanded', 'false'); });
    current = null;
    return wasOpen;
  }

  function toggle(btn) {
    const id = btn.dataset.menu;
    const open = current !== id;
    closeAll();
    if (!open) return;
    $('#' + id).classList.add('open');
    btn.classList.add('on');
    btn.setAttribute('aria-expanded', 'true');
    current = id;
    if (id === 'm-bell') {
      const d = $('.dot', btn); if (d) d.remove();
      if (window.TuColectivoNative?.getArrivalNotificationState) window.TuColectivoNative.getArrivalNotificationState();
    }
    if (id === 'm-search') setTimeout(() => q.focus(), 220);
  }

  function setStatus(text, state) {
    status.textContent = text;
    status.dataset.state = state || 'ok';
    status.classList.remove('chg');
    void status.offsetWidth;
    status.classList.add('chg');
  }

  function go(name) {
    if (!screens.some(s => s.dataset.screen === name)) return;
    currentScreen = name;
    screens.forEach(s => s.classList.toggle('active', s.dataset.screen === name));
    navButtons.forEach(b => b.classList.toggle('active', b.dataset.go === name));
    closeAll();
    if (name !== 'inicio') {
      setStatus('SECCIÓN: ' + name.toUpperCase(), 'ok');
    } else {
      setStatus('SISTEMA LISTO', 'ok');
    }
    emit('app:navigate', { go: name });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  document.addEventListener('pointerdown', e => {
    const b = e.target.closest('.btn,.item,.chip,.card,.home-card,.line-card,.data-card,.arrival-card,.favorite-item,.locate-stops,.sub-back,.favorite-toggle,.favorite-remove,.nav-btn,.sync-btn');
    if (b) { b.classList.remove('hack'); void b.offsetWidth; b.classList.add('hack'); }
    if (current && !e.target.closest('.panel,[data-menu]')) closeAll();
  });

  document.addEventListener('animationend', e => e.target.classList.remove('hack'));
  document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAll(); });

  document.addEventListener('click', e => {
    const t = e.target.closest('[data-menu]');
    if (t) return toggle(t);
    const target = e.target.closest('[data-go]');
    if (target) { go(target.dataset.go); return; }
  });

  const search = () => emit('app:search', { q: q.value.trim(), filter });
  q.addEventListener('input', search);
  q.addEventListener('keydown', e => { if (e.key === 'Enter') { q.blur(); search(); } });
  $$('.chip').forEach(c => c.addEventListener('click', () => {
    $$('.chip').forEach(o => o.setAttribute('aria-pressed', String(o === c)));
    filter = c.dataset.f;
    search();
  }));

  let lineSyncPending = false;
  let lineSyncTimer = null;
  let lineSyncResultTimer = null;
  let lineSyncStartedAt = 0;
  const LINE_SYNC_ANIMATION_MS = 2200;

  function finishLineSync(success, count, message) {
    if (!lineSyncPending) return;

    // SmartMove puede responder antes de que termine la animación CSS.
    // En ese caso esperamos el tiempo restante para que la barra nunca salte
    // de 0 a 100% instantáneamente.
    if (success) {
      const elapsed = performance.now() - lineSyncStartedAt;
      const remaining = Math.max(0, LINE_SYNC_ANIMATION_MS - elapsed);
      if (remaining > 0) {
        if (lineSyncResultTimer !== null) clearTimeout(lineSyncResultTimer);
        lineSyncResultTimer = setTimeout(() => {
          lineSyncResultTimer = null;
          finishLineSync(success, count, message);
        }, remaining);
        return;
      }
    }

    lineSyncPending = false;
    if (lineSyncTimer !== null) clearTimeout(lineSyncTimer);
    lineSyncTimer = null;
    if (lineSyncResultTimer !== null) clearTimeout(lineSyncResultTimer);
    lineSyncResultTimer = null;

    const b = $('#sync');
    const label = b.querySelector('b');
    const detail = b.querySelector('small');
    b.classList.remove('run', 'done');
    if (success) {
      b.classList.add('done');
      label.textContent = 'LÍNEAS ACTUALIZADAS';
      detail.textContent = count + (count === 1 ? ' LÍNEA CONFIRMADA' : ' LÍNEAS CONFIRMADAS');
      setStatus(detail.textContent, 'ok');
      emit('app:status', { text: detail.textContent, state: 'ok' });
    } else {
      label.textContent = 'ERROR AL SINCRONIZAR';
      detail.textContent = message || 'NO SE PUDIERON CARGAR LAS LÍNEAS';
      setStatus('ERROR DE SINCRONIZACIÓN', 'error');
      emit('app:status', { text: detail.textContent, state: 'error' });
    }
  }
  window.TuColectivoLineSyncResult = finishLineSync;

  $('#sync').addEventListener('click', () => {
    if (lineSyncPending) return;
    const b = $('#sync');
    const label = b.querySelector('b');
    const detail = b.querySelector('small');
    if (!window.TuColectivoNative || typeof window.TuColectivoNative.loadLines !== 'function') {
      label.textContent = 'SERVICIO NO DISPONIBLE';
      detail.textContent = 'NO HAY CONEXIÓN CON SMARTMOVE';
      setStatus('SIN CONEXIÓN CON SMARTMOVE', 'error');
      return;
    }
    lineSyncPending = true;
    lineSyncStartedAt = performance.now();
    if (lineSyncResultTimer !== null) {
      clearTimeout(lineSyncResultTimer);
      lineSyncResultTimer = null;
    }
    b.classList.remove('done');
    b.classList.add('run');
    label.textContent = 'SINCRONIZANDO...';
    detail.textContent = 'CONSULTANDO SMARTMOVE';
    setStatus('SINCRONIZANDO LÍNEAS...', 'busy');
    lineSyncTimer = setTimeout(() => finishLineSync(false, 0, 'SMARTMOVE NO RESPONDIÓ A TIEMPO'), 35000);
    try {
      window.TuColectivoNative.loadLines();
    } catch (_) {
      finishLineSync(false, 0, 'NO SE PUDO INICIAR LA CONSULTA');
    }
  });

  document.addEventListener('app:status', e => setStatus(e.detail.text, e.detail.state));
  window.setSystemStatus = setStatus;
  function handleBack(){
    if (current !== null) { closeAll(); return true; }
    if (currentScreen === 'lineas' && typeof window.TuColectivoLineBack === 'function') {
      return !!window.TuColectivoLineBack();
    }
    if (currentScreen !== 'inicio') { go('inicio'); return true; }
    return false;
  }
  window.TuColectivoLineLabels = window.TuColectivoLineLabels || {};
  window.TuColectivoPublicLineLabel = (code, labels) => {
    const source = labels || window.TuColectivoLineLabels || {};
    const raw = String(source[String(code)] ?? '').trim();
    const clean = raw.replace(/^l[ií]nea\s*/i, '').replace(/\s+/g, ' ').trim();
    return clean || String(code);
  };
  window.TuColectivo = { setStatus, closeMenus: closeAll, navigate: go, handleBack };
})();


/* FASE 6 — MAPA */
(() => {
  'use strict';
  const canvas = document.getElementById('mapCanvas');
  if (!canvas) return;
  const wrap = document.getElementById('map-wrap');
  const state = document.getElementById('mapState');
  const zoomLabel = document.getElementById('mapZoom');
  const locate = document.getElementById('mapLocate');
  const plus = document.getElementById('mapPlus');
  const minus = document.getElementById('mapMinus');
  const refreshLive = document.getElementById('mapRefreshLive');
  const refreshLiveMeta = document.getElementById('mapRefreshLiveMeta');
  const refreshLiveState = document.getElementById('mapRefreshLiveState');
  const ctx = canvas.getContext('2d');
  const TILE = 256;
  const FALLBACK = {lat:-33.3017,lng:-66.3378};
  const map = {lat:FALLBACK.lat,lng:FALLBACK.lng,zoom:14,drag:false,pointers:new Map(),last:null,baseDistance:0,baseZoom:14,userLocation:null,routePoints:[],stops:[],vehicles:[],routeLineCode:0,availableLines:[],availableLineLabels:{},lineLabels:{},availableLinesLoading:false,pointerStart:null,dragged:false,manualRouteRefreshViewport:null};
  const tiles = new Map();

  function worldSize(z){ return TILE * Math.pow(2,z); }
  function project(lat,lng,z){
    const s=worldSize(z), x=(lng+180)/360*s;
    const r=lat*Math.PI/180, y=(1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2*s;
    return {x,y};
  }
  function unproject(x,y,z){
    const s=worldSize(z), lng=x/s*360-180;
    const n=Math.PI-2*Math.PI*y/s;
    return {lat:180/Math.PI*Math.atan(Math.sinh(n)),lng};
  }
  function tileImage(x,y,z){
    const n=Math.pow(2,z);
    x=((x%n)+n)%n;
    if(y<0||y>=n)return null;
    const key=z+'/'+x+'/'+y;
    if(tiles.has(key))return tiles.get(key);
    const img=new Image();
    img.decoding='async';
    img.src='https://tile.openstreetmap.org/'+z+'/'+x+'/'+y+'.png';
    tiles.set(key,img);
    img.onload=draw;
    img.onerror=()=>{state.textContent='MAPA SIN CONEXIÓN';draw();};
    return img;
  }
  function resize(){
    const dpr=Math.min(window.devicePixelRatio||1,2);
    const r=wrap.getBoundingClientRect();
    canvas.width=Math.max(1,Math.floor(r.width*dpr));
    canvas.height=Math.max(1,Math.floor(r.height*dpr));
    canvas.style.width=r.width+'px';canvas.style.height=r.height+'px';
    ctx.setTransform(dpr,0,0,dpr,0,0);
    draw();
  }
  function draw(){
    const r=wrap.getBoundingClientRect();
    const w=r.width,h=r.height;
    ctx.clearRect(0,0,w,h);
    ctx.fillStyle='#07101c';ctx.fillRect(0,0,w,h);
    const center=project(map.lat,map.lng,map.zoom);
    const minX=Math.floor((center.x-w/2)/TILE)-1;
    const maxX=Math.floor((center.x+w/2)/TILE)+1;
    const minY=Math.floor((center.y-h/2)/TILE)-1;
    const maxY=Math.floor((center.y+h/2)/TILE)+1;
    let loaded=false;
    for(let tx=minX;tx<=maxX;tx++) for(let ty=minY;ty<=maxY;ty++){
      const img=tileImage(tx,ty,map.zoom);
      const px=tx*TILE-center.x+w/2, py=ty*TILE-center.y+h/2;
      if(img&&img.complete&&img.naturalWidth){ctx.drawImage(img,px,py,TILE,TILE);loaded=true;}
    }
    ctx.fillStyle='rgba(2,5,15,.28)';ctx.fillRect(0,0,w,h);
    drawGrid(w,h);
    drawRoute(w,h,center);
    drawStops(w,h,center);
    const animateVehicles=drawVehicles(w,h,center);
    if(animateVehicles) requestAnimationFrame(draw);
    if(map.userLocation){const p=screenPoint(map.userLocation.lat,map.userLocation.lng,center,w,h);drawMarker(p.x,p.y);}
    zoomLabel.textContent='ZOOM '+map.zoom;
    if(loaded) state.textContent='MAPA EN LÍNEA';
  }
  function drawGrid(w,h){
    ctx.save();
    ctx.strokeStyle='rgba(0,240,255,.08)';ctx.lineWidth=1;
    const step=48;
    for(let x=w/2%step;x<w;x+=step){ctx.beginPath();ctx.moveTo(x,0);ctx.lineTo(x,h);ctx.stroke();}
    for(let y=h/2%step;y<h;y+=step){ctx.beginPath();ctx.moveTo(0,y);ctx.lineTo(w,y);ctx.stroke();}
    ctx.restore();
  }
  function screenPoint(lat,lng,center,w,h){
    const p=project(lat,lng,map.zoom),world=worldSize(map.zoom);
    let dx=p.x-center.x;
    if(dx>world/2)dx-=world;else if(dx< -world/2)dx+=world;
    return {x:dx+w/2,y:p.y-center.y+h/2};
  }
  function drawRoute(w,h,center){
    if(!Array.isArray(map.routePoints)||map.routePoints.length<2)return;
    ctx.save();ctx.lineCap='round';ctx.lineJoin='round';
    ctx.beginPath();
    map.routePoints.forEach((point,i)=>{const p=screenPoint(point.lat,point.lng,center,w,h);if(i===0)ctx.moveTo(p.x,p.y);else ctx.lineTo(p.x,p.y);});
    ctx.strokeStyle='rgba(255,0,255,.28)';ctx.lineWidth=9;ctx.shadowColor='#ff00ff';ctx.shadowBlur=14;ctx.stroke();
    ctx.strokeStyle='#ff4dff';ctx.lineWidth=3.5;ctx.shadowBlur=5;ctx.stroke();ctx.restore();
  }
  function drawStops(w,h,center){
    ctx.save();
    for(const stop of map.stops){
      if(!Number.isFinite(stop.lat)||!Number.isFinite(stop.lng))continue;
      const p=screenPoint(stop.lat,stop.lng,center,w,h);
      if(p.x< -20||p.x>w+20||p.y< -20||p.y>h+20)continue;
      ctx.beginPath();ctx.arc(p.x,p.y,6,0,Math.PI*2);ctx.fillStyle='rgba(0,240,255,.22)';ctx.fill();
      ctx.lineWidth=2;ctx.strokeStyle='#00f0ff';ctx.shadowColor='#00f0ff';ctx.shadowBlur=8;ctx.stroke();ctx.shadowBlur=0;
      // El código interno de la parada no se dibuja para mantener el mapa limpio.
    }
    ctx.restore();
  }
  function drawVehicles(w,h,center){
    const now=performance.now();
    let needsAnimation=false;
    ctx.save();
    for(const vehicle of map.vehicles){
      if(!Number.isFinite(vehicle.lat)||!Number.isFinite(vehicle.lng))continue;
      const motion=map.vehicleMotion?.get(vehicle.id);
      let lat=vehicle.lat,lng=vehicle.lng;
      if(motion){
        const progress=Math.min(1,(now-motion.startedAt)/motion.duration);
        const eased=progress<.5?2*progress*progress:1-Math.pow(-2*progress+2,2)/2;
        lat=motion.fromLat+(motion.toLat-motion.fromLat)*eased;
        lng=motion.fromLng+(motion.toLng-motion.fromLng)*eased;
        if(progress<1)needsAnimation=true;
      }
      const p=screenPoint(lat,lng,center,w,h);
      if(p.x< -28||p.x>w+28||p.y< -28||p.y>h+28)continue;
      const pulse=.5+.5*Math.sin(now/420);
      const radius=8.5+pulse*2;
      ctx.beginPath();ctx.arc(p.x,p.y,radius+4,0,Math.PI*2);ctx.fillStyle='rgba(255,0,255,'+(0.05+pulse*.08)+')';ctx.fill();
      ctx.beginPath();ctx.arc(p.x,p.y,9,0,Math.PI*2);ctx.fillStyle='rgba(255,0,255,.22)';ctx.fill();
      ctx.strokeStyle='#ff4dff';ctx.lineWidth=2;ctx.shadowColor='#ff00ff';ctx.shadowBlur=12+pulse*8;ctx.stroke();ctx.shadowBlur=0;
      const vehicleLabel=publicLineLabel(vehicle.lineCode||vehicle.line,window.TuColectivoLineLabels);
      ctx.fillStyle='#fff';ctx.font='bold 9px Arial';ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText(vehicleLabel||'?',p.x,p.y+.5);
      needsAnimation=true;
    }
    ctx.restore();
    return needsAnimation;
  }
  function fitPoints(points){
    const valid=(points||[]).filter(p=>Number.isFinite(p.lat)&&Number.isFinite(p.lng));
    if(!valid.length)return;
    const minLat=Math.min(...valid.map(p=>p.lat)),maxLat=Math.max(...valid.map(p=>p.lat));
    const minLng=Math.min(...valid.map(p=>p.lng)),maxLng=Math.max(...valid.map(p=>p.lng));
    map.lat=(minLat+maxLat)/2;map.lng=(minLng+maxLng)/2;
    const r=wrap.getBoundingClientRect(),pad=54;
    let chosen=11;
    for(let z=18;z>=11;z--){
      const a=project(maxLat,minLng,z),b=project(minLat,maxLng,z);
      if(Math.abs(b.x-a.x)<=Math.max(1,r.width-pad*2)&&Math.abs(b.y-a.y)<=Math.max(1,r.height-pad*2)){chosen=z;break;}
    }
    map.zoom=chosen;draw();
  }
  function mapEscape(value){return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function mapArrivalDestination(value){
    const raw=String(value||'').replace(/[.·•‧∙⋅。．]+/g,' ').replace(/[\u200B-\u200D\uFEFF]/g,' ').replace(/\s+/g,' ').trim();
    if(!raw||/^(?:A{1,2}|N\/?A|S\/?D|DESTINO|SERVICIO)$/i.test(raw))return 'DESTINO NO INFORMADO';
    return 'HACIA '+raw.toUpperCase();
  }
  function publicLineLabel(code, labels){
    const source=labels||map.lineLabels||map.availableLineLabels||window.TuColectivoLineLabels||{};
    return window.TuColectivoPublicLineLabel
      ? window.TuColectivoPublicLineLabel(code, source)
      : String(code);
  }
  function openStopPopup(stop){
    const popup=document.getElementById('mapStopPopup');if(!popup)return;
    map.pendingStop=stop;
    document.getElementById('mapPopupName').textContent=stop.name||('PARADA '+stop.code);
    const cleanLocation=value=>String(value||'').replace(/\s*(?:,|-)?\s*SAN LUIS\s*$/i,'').trim();
    const locations=[stop.street,stop.intersection].map(cleanLocation).filter(Boolean);
    const uniqueLocations=locations.filter((value,index,array)=>array.findIndex(other=>other.toLocaleLowerCase('es-AR')===value.toLocaleLowerCase('es-AR'))===index);
    document.getElementById('mapPopupLocation').textContent=uniqueLocations.join(' · ')||'UBICACIÓN DE PARADA';
    const lines=document.getElementById('mapPopupLines');
    const availableLines=[...new Set((stop.lines||[]).map(Number).filter(n=>Number.isFinite(n)&&n>0))].sort((a,b)=>a-b);
    const renderLineChoices=choices=>{lines.innerHTML=choices.map(n=>'<button class="map-popup-line" data-map-stop-line="'+n+'" type="button">CONSULTAR LÍNEA '+mapEscape(publicLineLabel(n))+'</button>').join('')||'<small>NO SE ENCONTRARON LÍNEAS PARA CONSULTAR</small>';};
    if(availableLines.length)renderLineChoices(availableLines);
    else{
      lines.innerHTML='<small>BUSCANDO LÍNEAS QUE PASAN POR ESTA PARADA...</small>';
    }
    document.getElementById('mapPopupArrivals').innerHTML='';
    popup.hidden=false;
    if(!availableLines.length&&window.TuColectivoNative?.resolveNearbyStopLines){
      window.TuColectivoNative.resolveNearbyStopLines(stop.identifier,stop.lat,stop.lng);
    }
  }
  function hitTestMap(x,y){
    const r=wrap.getBoundingClientRect(),center=project(map.lat,map.lng,map.zoom),w=r.width,h=r.height;
    let best=null,bestD=20;
    for(const stop of map.stops){const p=screenPoint(stop.lat,stop.lng,center,w,h),d=Math.hypot(p.x-x,p.y-y);if(d<bestD){best=stop;bestD=d;}}
    if(best)openStopPopup(best);
  }
  function drawMarker(x,y){
    ctx.save();
    ctx.strokeStyle='#00f0ff';ctx.fillStyle='rgba(0,240,255,.18)';
    ctx.shadowColor='#00f0ff';ctx.shadowBlur=16;
    ctx.lineWidth=2;
    ctx.beginPath();ctx.arc(x,y,10,0,Math.PI*2);ctx.fill();ctx.stroke();
    ctx.beginPath();ctx.arc(x,y,3,0,Math.PI*2);ctx.fillStyle='#fff';ctx.fill();
    ctx.restore();
  }
  function setCenter(lat,lng){map.lat=Math.max(-85,Math.min(85,lat));map.lng=lng;draw();}
  function setZoom(z){
    map.zoom=Math.max(11,Math.min(18,Math.round(z)));
    zoomLabel.textContent='ZOOM '+map.zoom;draw();
  }
  function zoomAt(delta){
    setZoom(map.zoom+delta);
    if(navigator.vibrate) navigator.vibrate(12);
  }
  function locateUser(){
    state.textContent='LOCALIZANDO...';
    if(window.TuColectivoNative&&typeof window.TuColectivoNative.requestMapLocation==='function')window.TuColectivoNative.requestMapLocation(map.routeLineCode||0);
    else state.textContent='UBICACIÓN NATIVA NO DISPONIBLE';
  }
  function distance(a,b){return Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);}
  canvas.addEventListener('pointerdown',e=>{
    canvas.setPointerCapture(e.pointerId);
    map.pointers.set(e.pointerId,e);
    if(map.pointers.size===1){map.drag=true;map.last=e;map.pointerStart={x:e.clientX,y:e.clientY};map.dragged=false;}
    else if(map.pointers.size===2){map.drag=false;const p=[...map.pointers.values()];map.baseDistance=distance(p[0],p[1]);map.baseZoom=map.zoom;}
  });
  canvas.addEventListener('pointermove',e=>{
    if(!map.pointers.has(e.pointerId))return;
    map.pointers.set(e.pointerId,e);
    if(map.pointers.size===1&&map.drag&&map.last){
      const dx=e.clientX-map.last.clientX,dy=e.clientY-map.last.clientY;
      if(map.pointerStart&&Math.hypot(e.clientX-map.pointerStart.x,e.clientY-map.pointerStart.y)>6)map.dragged=true;
      const c=project(map.lat,map.lng,map.zoom);
      const next=unproject(c.x-dx,c.y-dy,map.zoom);
      setCenter(next.lat,next.lng);map.last=e;
    }else if(map.pointers.size===2){
      const p=[...map.pointers.values()];
      const d=distance(p[0],p[1]);
      if(map.baseDistance>0){const delta=Math.max(-2,Math.min(2,Math.round(Math.log2(d/map.baseDistance)*2)));setZoom(map.baseZoom+delta);}
    }
  });
  ['pointerup','pointercancel','pointerleave'].forEach(type=>canvas.addEventListener(type,e=>{
    if(type==='pointerup'&&!map.dragged&&map.pointerStart){const r=canvas.getBoundingClientRect();hitTestMap(e.clientX-r.left,e.clientY-r.top);}
    map.pointers.delete(e.pointerId);if(map.pointers.size===0){map.drag=false;map.last=null;map.pointerStart=null;map.dragged=false;}
  }));
  canvas.addEventListener('wheel',e=>{e.preventDefault();zoomAt(e.deltaY<0?1:-1);},{passive:false});
  plus.addEventListener('click',()=>zoomAt(1));
  minus.addEventListener('click',()=>zoomAt(-1));
  locate.addEventListener('click',locateUser);
  window.addEventListener('resize',resize);
  const mapPopup=document.getElementById('mapStopPopup');
  if(mapPopup)mapPopup.addEventListener('click',e=>{
    if(e.target.closest('.map-popup-close')){mapPopup.hidden=true;return;}
    const choice=e.target.closest('[data-map-stop-line]');
    if(choice&&map.pendingStop&&window.TuColectivoNative&&typeof window.TuColectivoNative.loadArrivals==='function'){
      const line=Number(choice.dataset.mapStopLine);map.pendingMapArrival={stop:map.pendingStop,line};
      document.getElementById('mapPopupArrivals').innerHTML='<small>CONSULTANDO ARRIBOS EN TIEMPO REAL...</small>';
      window.TuColectivoNative.loadArrivals(map.pendingStop.identifier,line);
    }
  });
  window.onNativeMapRoute=payload=>{
    try{map.routePoints=(typeof payload==='string'?JSON.parse(payload):payload||[]).map(p=>({lat:Number(p.lat),lng:Number(p.lng)})).filter(p=>Number.isFinite(p.lat)&&Number.isFinite(p.lng));
      state.textContent=map.routePoints.length>1?'RECORRIDO CARGADO · LÍNEA '+publicLineLabel(map.routeLineCode):'SIN GEOMETRÍA DE RECORRIDO';
      if(refreshLive&&map.routeLineCode>0){refreshLive.disabled=false;refreshLiveState.textContent='GPS';refreshLiveMeta.textContent='LÍNEA '+publicLineLabel(map.routeLineCode)+' · RECORRIDO EN TIEMPO REAL';}
      if(map.manualRouteRefreshViewport){
        const viewport=map.manualRouteRefreshViewport;
        map.lat=viewport.lat;map.lng=viewport.lng;map.zoom=viewport.zoom;
        map.manualRouteRefreshViewport=null;
        draw();
      }else if(map.routePoints.length)fitPoints(map.routePoints.concat(map.stops));else draw();
    }catch(_){state.textContent='ERROR LEYENDO RECORRIDO';}
  };
  window.onNativeMapRouteError=payload=>{map.manualRouteRefreshViewport=null;let msg='NO SE PUDO CARGAR EL RECORRIDO';try{msg=(typeof payload==='string'?JSON.parse(payload):payload).message||msg;}catch(_){}state.textContent=msg;};
  window.onNativeContinuousLocation=function(payload){
    let r={};try{r=typeof payload==='string'?JSON.parse(payload):payload||{};}catch(_){}
    const lat=Number(r.latitude),lng=Number(r.longitude);
    if(!Number.isFinite(lat)||!Number.isFinite(lng))return;
    if(document.querySelector('[data-screen="mapa"]')?.classList.contains('active')){
      map.userLocation={lat,lng};
      draw();
    }
    if(document.querySelector('[data-screen="paradas"]')?.classList.contains('active')){
      if(nearbyState)nearbyState.textContent='';
    }
  };

  window.onNativeLocation=function(payload){
    let r={};try{r=typeof payload==='string'?JSON.parse(payload):payload||{};}catch(_){}
    if(r.purpose==='map'){
      map.userLocation={lat:Number(r.latitude),lng:Number(r.longitude)};
      setCenter(map.userLocation.lat,map.userLocation.lng);
      state.textContent='';
      if(map.loadStopsAfterLocation&&window.TuColectivoNative?.loadMapDataAtLocation){
        map.loadStopsAfterLocation=false;
        window.TuColectivoNative.loadMapDataAtLocation(map.userLocation.lat,map.userLocation.lng,map.routeLineCode||0);
      }
    }
    if(r.purpose==='nearby')setNearbyState('');
  };
  window.onNativeLocationError=function(payload){
    let r={};try{r=typeof payload==='string'?JSON.parse(payload):payload||{};}catch(_){}
    if(r.purpose==='map')state.textContent=r.message||'GPS NO DISPONIBLE';
    if(r.purpose==='nearby'){locateBtn.disabled=false;setNearbyState('');}
  };
  window.onNativeMapStops=payload=>{
    try{const raw=typeof payload==='string'?JSON.parse(payload):payload||[];raw.forEach(s=>{if(s?.lineLabels&&typeof s.lineLabels==='object')Object.assign(map.lineLabels,s.lineLabels);});
      map.stops=raw.map(s=>({id:String(s.identifier||s.code),code:Number(s.code)||0,identifier:String(s.identifier||s.code||''),name:String(s.description||('PARADA '+s.code)),lat:Number(s.latitude),lng:Number(s.longitude),street:String(s.street||''),intersection:String(s.intersection||''),lines:(()=>{const found=[...(Array.isArray(s.lineCodes)?s.lineCodes:[]).map(Number),Number(s.lineCode)];return [...new Set(found.filter(n=>Number.isFinite(n)&&n>0))].sort((a,b)=>a-b);})()})).filter(s=>Number.isFinite(s.lat)&&Number.isFinite(s.lng)&&(s.lat!==0||s.lng!==0));
      state.textContent='';draw();
    }catch(_){state.textContent='ERROR LEYENDO PARADAS';}
  };
  window.onNativeMapStopsError=payload=>{let msg='NO SE PUDIERON CARGAR LAS PARADAS';try{msg=(typeof payload==='string'?JSON.parse(payload):payload).message||msg;}catch(_){}state.textContent=msg;};
  window.onNativeMapAvailableLines=payload=>{
    try{
      const raw=typeof payload==='string'?JSON.parse(payload):payload||[];
      map.availableLines=[...new Set(raw.map(item=>Number(item.code)).filter(n=>Number.isFinite(n)&&n>0))].sort((a,b)=>a-b);
      map.availableLineLabels={};
      raw.forEach(item=>{const code=Number(item.code);if(Number.isFinite(code)&&code>0)map.availableLineLabels[String(code)]=String(item.name||'').replace(/^l[ií]nea\s*/i,'').trim()||String(code);});
      Object.assign(map.lineLabels,map.availableLineLabels);
      map.availableLinesLoading=false;
      const popup=document.getElementById('mapStopPopup'), lines=document.getElementById('mapPopupLines');
      if(popup&&!popup.hidden&&map.pendingStop&&!(map.pendingStop.lines||[]).length&&lines){
        lines.innerHTML='<small>NO HAY LÍNEAS CONFIRMADAS PARA ESTA PARADA.</small>';
      }
    }catch(_){map.availableLinesLoading=false;}
  };
  window.onNativeMapAvailableLinesError=payload=>{
    map.availableLinesLoading=false;
    const lines=document.getElementById('mapPopupLines');
    if(lines&&!map.availableLines.length)lines.innerHTML='<small>NO SE PUDIERON CARGAR LAS LÍNEAS. VOLVÉ A ABRIR LA PARADA PARA REINTENTAR.</small>';
  };
  window.onNativeMapVehicles=payload=>{try{
    const raw=typeof payload==='string'?JSON.parse(payload):payload||[];
    map.vehicleMotion=map.vehicleMotion||new Map();
    const now=performance.now();
    const next=raw.map(v=>{
      const id=String(v.id||v.vehicleId||'');
      const lat=Number(v.lat??v.latitude),lng=Number(v.lng??v.longitude);
      const lineCode=Number(v.lineCode||v.code||map.routeLineCode||0);
      const previous=map.vehicles.find(item=>item.id===id);
      if(previous&&Number.isFinite(previous.lat)&&Number.isFinite(previous.lng)&&
         (Math.abs(previous.lat-lat)>0.0000001||Math.abs(previous.lng-lng)>0.0000001)){
        map.vehicleMotion.set(id,{fromLat:previous.lat,fromLng:previous.lng,toLat:lat,toLng:lng,startedAt:now,duration:9000});
      }else if(!previous){
        map.vehicleMotion.delete(id);
      }
      return {id,lineCode,line:publicLineLabel(lineCode||v.line,window.TuColectivoLineLabels),destination:String(v.destination||''),lat,lng,gpsTimestamp:String(v.gpsTimestamp||'')};
    }).filter(v=>Number.isFinite(v.lat)&&Number.isFinite(v.lng)&&(v.lat!==0||v.lng!==0));
    const activeIds=new Set(next.map(v=>v.id));
    [...map.vehicleMotion.keys()].forEach(id=>{if(!activeIds.has(id))map.vehicleMotion.delete(id);});
    map.vehicles=next;
  }catch(_){
    if(refreshLive&&map.manualVehicleRefresh){map.manualVehicleRefresh=false;refreshLive.classList.remove('is-refreshing');refreshLive.disabled=false;refreshLiveState.textContent='ERROR';refreshLiveMeta.textContent='NO SE PUDO ACTUALIZAR EL GPS';}
    state.textContent='ERROR LEYENDO GPS DE COLECTIVOS';}};
  window.TuColectivoMap={
    showRoute(lineCode){map.routeLineCode=Number(lineCode)||0;map.routePoints=[];map.vehicles=[];map.manualVehicleRefresh=false;if(refreshLive){refreshLive.disabled=map.routeLineCode<=0;refreshLive.classList.remove('is-refreshing');refreshLiveState.textContent='GPS';refreshLiveMeta.textContent=map.routeLineCode>0?'LÍNEA '+publicLineLabel(map.routeLineCode)+' · RECORRIDO EN TIEMPO REAL':'SELECCIONÁ UNA LÍNEA DESDE ARRIBOS';}map.loadStopsAfterLocation=true;state.textContent='CARGANDO RECORRIDO...';document.getElementById('mapStopPopup')?.setAttribute('hidden','');if(window.TuColectivoNative&&map.routeLineCode>0)window.TuColectivoNative.loadMapRoute(map.routeLineCode);window.TuColectivo.navigate('mapa');draw();},
    handleStopLines(result){
      const stop=map.pendingStop;
      if(!stop||String(stop.identifier)!==String(result?.identifier))return false;
      const lines=[...new Set((result?.lines||[]).map(Number).filter(n=>Number.isFinite(n)&&n>0))].sort((a,b)=>a-b);
      if(result?.lineLabels&&typeof result.lineLabels==='object')Object.assign(map.lineLabels,result.lineLabels);
      stop.lines=lines;
      stop.lineLabels=result?.lineLabels&&typeof result.lineLabels==='object'?result.lineLabels:(stop.lineLabels||{});
      const box=document.getElementById('mapPopupLines');
      if(!box)return true;
      if(!lines.length){box.innerHTML='<small>NO SE PUDIERON CONFIRMAR LÍNEAS PARA ESTA PARADA.</small>';return true;}
      box.innerHTML='<small>LÍNEAS QUE PASAN POR ESTA PARADA</small>'+lines.map(line=>'<button class="map-popup-line" data-map-stop-line="'+line+'" type="button">CONSULTAR LÍNEA '+mapEscape(publicLineLabel(line,result?.lineLabels))+'</button>').join('');
      return true;
    },
    handleArrivals(payload){if(!map.pendingMapArrival)return false;let items=[];try{items=typeof payload==='string'?JSON.parse(payload):payload||[];}catch(_){}const target=document.getElementById('mapPopupArrivals');if(target)target.innerHTML=items.length?items.map(a=>'<div class="map-popup-arrival"><b>'+mapEscape(a.line||'COLECTIVO')+'</b><span>'+mapEscape(mapArrivalDestination(a.destination))+'</span><strong>'+mapEscape(a.minutes==null?'--':a.minutes)+' MIN</strong></div>').join(''):'<small>SMARTMOVE NO DEVOLVIÓ ARRIBOS PARA ESTA LÍNEA</small>';map.pendingMapArrival=null;return true;},
    handleArrivalsError(payload){if(!map.pendingMapArrival)return false;let msg='NO SE PUDIERON CARGAR LOS ARRIBOS';try{msg=(typeof payload==='string'?JSON.parse(payload):payload).message||msg;}catch(_){}const target=document.getElementById('mapPopupArrivals');if(target)target.textContent=msg;map.pendingMapArrival=null;return true;}
  };
  if(refreshLive)refreshLive.addEventListener('click',()=>{
    if(!map.routeLineCode||!window.TuColectivoNative)return;
    map.manualVehicleRefresh=true;
    map.manualRouteRefreshViewport={lat:map.lat,lng:map.lng,zoom:map.zoom};
    refreshLive.disabled=true;
    refreshLive.classList.add('is-refreshing');
    refreshLiveState.textContent='ACTUALIZANDO';
    refreshLiveMeta.textContent='LÍNEA '+publicLineLabel(map.routeLineCode)+' · CONSULTANDO GPS...';
    state.textContent='ACTUALIZANDO RECORRIDO EN TIEMPO REAL...';
    if(typeof window.TuColectivoNative.loadMapRoute==='function')window.TuColectivoNative.loadMapRoute(map.routeLineCode);
    if(typeof window.TuColectivoNative.refreshMapVehiclesManual==='function')window.TuColectivoNative.refreshMapVehiclesManual(map.routeLineCode);
    else if(typeof window.TuColectivoNative.refreshMapVehicles==='function')window.TuColectivoNative.refreshMapVehicles(map.routeLineCode);
  });
  window.onNativeMapManualRefreshComplete=payload=>{
    if(!refreshLive)return;
    map.manualVehicleRefresh=false;
    refreshLive.classList.remove('is-refreshing');
    refreshLive.disabled=false;
    refreshLiveState.textContent='GPS';
    const stamp=new Date().toLocaleTimeString([], {hour:'2-digit',minute:'2-digit',second:'2-digit'});
    refreshLiveMeta.textContent='LÍNEA '+publicLineLabel(map.routeLineCode)+' · GPS ACTUALIZADO '+stamp;
    state.textContent='RECORRIDO ACTUALIZADO · GPS '+stamp;
  };
  window.onNativeMapManualRefreshError=payload=>{
    if(!refreshLive)return;
    let msg='NO SE PUDO ACTUALIZAR EL GPS';
    try{msg=(typeof payload==='string'?JSON.parse(payload):payload).message||msg;}catch(_){}
    map.manualVehicleRefresh=false;
    map.manualRouteRefreshViewport=null;
    refreshLive.classList.remove('is-refreshing');
    refreshLive.disabled=false;
    refreshLiveState.textContent='ERROR';
    refreshLiveMeta.textContent=msg;
    state.textContent=msg;
  };
  window.setInterval(()=>{const screen=document.querySelector('[data-screen="mapa"]');if(screen&&screen.classList.contains('active')&&window.TuColectivoNative&&typeof window.TuColectivoNative.refreshMapVehicles==='function')window.TuColectivoNative.refreshMapVehicles(map.routeLineCode||0);},10000);
  document.addEventListener('app:navigate',e=>{
    if(e.detail.go==='mapa'){
      map.loadStopsAfterLocation=true;
      setTimeout(resize,60);
      setTimeout(()=>{if(typeof locateUser==='function')locateUser();},120);
    }
  });
  resize();
})();

/* FASE 7 — PARADAS CERCANAS + ARRIBOS */
(() => {
  'use strict';
  const locateBtn=document.getElementById('locateStops');
  const nearbyList=document.getElementById('nearbyList');
  const nearbyState=document.getElementById('nearbyState');
  const arrivalList=document.getElementById('arrivalList');
  const arrivalStop=document.getElementById('arrivalStop');
  const arrivalsMeta=document.getElementById('arrivalsMeta');
  const arrivalsBack=document.getElementById('arrivalsBack');
  const setNearbyState=text=>{if(nearbyState)nearbyState.textContent=text||''};
  const setArrivalsMeta=text=>{if(arrivalsMeta)arrivalsMeta.textContent=text||''};
  let pendingStopSelectionId=null;
  if(!locateBtn||!nearbyList)return;

  function meters(a,b){
    const R=6371000,rad=Math.PI/180;
    const dLat=(b.lat-a.lat)*rad,dLng=(b.lng-a.lng)*rad;
    const x=Math.sin(dLat/2)**2+Math.cos(a.lat*rad)*Math.cos(b.lat*rad)*Math.sin(dLng/2)**2;
    return Math.round(R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x)));
  }
  function formatDistance(m){return m<1000?Math.max(1,m)+' M':(m/1000).toFixed(1).replace('.',',')+' KM';}
  function lineMarkup(lines,labels){
    const source=labels||window.TuColectivoLineLabels||{};
    return '<div class="line-badges">'+lines.map(n=>'<span class="line-badge-mini">LÍNEA '+escapeHtml(publicNearbyLineLabel(n,source))+'</span>').join('')+'</div>';
  }
  function publicNearbyLineLabel(code,labels){
    return window.TuColectivoPublicLineLabel
      ? window.TuColectivoPublicLineLabel(code,labels)
      : String(code);
  }

  function normalizeStop(stop){
    const labels=(stop&&stop.lineLabels&&typeof stop.lineLabels==='object')?stop.lineLabels:{};
    Object.assign(window.TuColectivoLineLabels||{},labels);
    const lines=Array.isArray(stop.lineCodes)?stop.lineCodes.map(Number).filter(Number.isFinite):[];
    const lineCode=Number(stop.lineCode);
    if(lineCode>0&&!lines.includes(lineCode))lines.push(lineCode);
    return {
      id:String(stop.identifier||stop.code),
      code:Number(stop.code)||0,
      identifier:String(stop.identifier||stop.code||''),
      name:String(stop.description||('PARADA '+stop.code)),
      lat:Number(stop.latitude),
      lng:Number(stop.longitude),
      street:String(stop.street||''),
      intersection:String(stop.intersection||''),
      lines:lines.sort((a,b)=>a-b),
      lineLabels:labels
    };
  }

  function renderNearby(rawStops,position){
    const here={lat:position.coords.latitude,lng:position.coords.longitude};
    window.TuColectivoLineLabels=window.TuColectivoLineLabels||{};
    const stops=rawStops.map(normalizeStop).filter(stop=>Number.isFinite(stop.lat)&&Number.isFinite(stop.lng));
    window.TuColectivoStops=stops;
    const ranked=stops.map(stop=>({...stop,distance:meters(here,stop)})).sort((a,b)=>a.distance-b.distance).slice(0,5);
    if(!ranked.length){
      setNearbyState('');
      nearbyList.innerHTML='<div class="nearby-empty"><strong>NO HAY PARADAS CERCANAS_</strong><span>SMARTMOVE NO DEVOLVIÓ PARADAS PARA ESTA UBICACIÓN</span></div>';
      return;
    }
    nearbyList.innerHTML=ranked.map(stop=>'<div class="data-card nearby-stop" data-stop-id="'+stop.id+'" role="button" tabindex="0"><b>'+escapeHtml(stop.name)+'</b><span>'+(stop.lines.length?stop.lines.length+' '+(stop.lines.length===1?'LÍNEA':'LÍNEAS')+' CONFIRMADAS':'BUSCANDO LÍNEAS...')+'</span>'+lineMarkup(stop.lines,stop.lineLabels)+'<em>'+formatDistance(stop.distance)+'</em></div>').join('');
    setNearbyState('');
    ranked.filter(stop=>!stop.lines.length).forEach(stop=>{
      window.TuColectivoNative?.resolveNearbyStopLines?.(stop.identifier,stop.lat,stop.lng);
    });
  }

  window.onNativeNearbyStops=function(payload){
    try{
      const data=typeof payload==='string'?JSON.parse(payload):payload;
      const stops=Array.isArray(data)?data:(Array.isArray(data?.stops)?data.stops:[]);
      if(data&&Number.isFinite(Number(data.latitude))&&Number.isFinite(Number(data.longitude))){
        window.TuColectivoPendingPosition={coords:{latitude:Number(data.latitude),longitude:Number(data.longitude)}};
      }
      if(window.TuColectivoPendingPosition) renderNearby(stops,window.TuColectivoPendingPosition);
      else {
        setNearbyState('');
        nearbyList.innerHTML='<div class="nearby-empty"><strong>UBICACIÓN NO DISPONIBLE_</strong><span>VOLVÉ A LOCALIZAR LAS PARADAS</span></div>';
      }
    }catch(error){
      setNearbyState('');
      nearbyList.innerHTML='<div class="nearby-empty"><strong>ERROR DE DATOS_</strong><span>NO SE PUDIERON LEER LAS PARADAS DE SMARTMOVE</span></div>';
    }finally{
      locateBtn.disabled=false;
      window.TuColectivoPendingPosition=null;
    }
  };

  window.onNativeNearbyStopsError=function(payload){
    let message='NO SE PUDIERON LOCALIZAR PARADAS';
    try{const data=typeof payload==='string'?JSON.parse(payload):payload;if(data&&data.message)message=data.message;}catch(_){}
    locateBtn.disabled=false;
    window.TuColectivoPendingPosition=null;
    setNearbyState('');
    nearbyList.innerHTML='<div class="nearby-empty"><strong>NO SE PUDIERON LOCALIZAR PARADAS_</strong><span>'+message+'</span></div>';
  };

  function showArrival(stop,line){
    window.TuColectivoCurrentNearbyArrival={stop,line:Number(line)};
    arrivalStop.innerHTML='<strong>'+escapeHtml(stop.name)+'</strong><span>LÍNEA '+escapeHtml(publicNearbyLineLabel(Number(line),stop.lineLabels))+' · PARADA SELECCIONADA</span><button class="arrival-favorite-toggle" type="button" aria-pressed="false">☆ AGREGAR ARRIBO A FAVORITOS</button>';
    arrivalList.innerHTML='<div class="nearby-empty"><strong>CONSULTANDO ARRIBOS_</strong><span>OBTENIENDO DATOS REALES DE SMARTMOVE</span></div>';
    setArrivalsMeta('LÍNEA '+publicNearbyLineLabel(Number(line),stop.lineLabels));
    window.TuColectivo.navigate('arribos');
    syncNearbyArrivalFavorite();
    if(window.TuColectivoNative&&typeof window.TuColectivoNative.loadArrivals==='function'){
      window.TuColectivoNative.loadArrivals(stop.identifier,Number(line));
    }else{
      arrivalList.innerHTML='<div class="nearby-empty"><strong>SERVICIO NO DISPONIBLE</strong><span>NO SE PUDO CONECTAR CON SMARTMOVE</span></div>';
    }
  }
  function escapeHtml(value){return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  function syncNearbyArrivalFavorite(){
    const current=window.TuColectivoCurrentNearbyArrival;
    const button=arrivalStop.querySelector('.arrival-favorite-toggle');
    if(!current||!button)return;
    const saved=window.TuColectivoFavorites?.isArrivalSaved(current.stop,current.line)||false;
    button.textContent=saved?'★ ARRIBO GUARDADO':'☆ AGREGAR ARRIBO A FAVORITOS';
    button.setAttribute('aria-pressed',String(saved));
  }
  window.TuColectivoNearbySyncArrivalFavorite=syncNearbyArrivalFavorite;
  window.onNativeNearbyArrivals=function(payload){
    let items=[];
    try{items=typeof payload==='string'?JSON.parse(payload):payload||[];}catch(_){}
    if(!arrivalList)return;
    arrivalList.innerHTML=items.length?items.map(item=>{
      const m=Number(item.minutes),duration=Number.isFinite(m)?Math.max(1.2,Math.min(18,m*.35)):8;
      const rawDestination=String(item.destination||'').replace(/[.·•‧∙⋅。．]+/g,' ').replace(/[\u200B-\u200D\uFEFF]/g,' ').replace(/\s+/g,' ').trim().replace(/[.·•‧∙⋅。．]+$/,'').trim();
      const unknownDestination=!rawDestination||/^(?:A{1,2}|N\/?A|S\/?D|DESTINO|SERVICIO)$/i.test(rawDestination);
      const destinationLabel=unknownDestination?'DESTINO NO INFORMADO':'HACIA '+rawDestination.toUpperCase();
      return '<div class="arrival-card"><div class="arrival-copy"><b>'+escapeHtml(item.line||('LÍNEA '+(window.TuColectivoCurrentNearbyArrival?.line||'')))+'</b><span>'+escapeHtml(destinationLabel)+'</span></div><em class="arrival-time" style="--arrival-duration:'+duration+'s"><strong>'+escapeHtml(item.minutes==null?'--':item.minutes)+'</strong><small>MIN</small></em></div>';
    }).join(''):'<div class="nearby-empty"><strong>SIN ARRIBOS</strong><span>SMARTMOVE NO DEVOLVIÓ SERVICIOS PARA ESTA PARADA</span></div>';
  };
  window.onNativeNearbyArrivalsError=function(payload){
    let message='NO SE PUDIERON CARGAR LOS ARRIBOS';
    try{const data=typeof payload==='string'?JSON.parse(payload):payload;if(data?.message)message=data.message;}catch(_){}
    if(arrivalList)arrivalList.innerHTML='<div class="nearby-empty"><strong>ERROR DE ARRIBOS</strong><span>'+escapeHtml(message)+'</span></div>';
  };

  window.onNativeNearbyStopLines=function(payload){
    let result={};try{result=typeof payload==='string'?JSON.parse(payload):payload||{};}catch(_){}
    const lines=[...new Set((result.lines||[]).map(Number).filter(n=>Number.isFinite(n)&&n>0))].sort((a,b)=>a-b);
    if(window.TuColectivoMap?.handleStopLines)window.TuColectivoMap.handleStopLines(result);
    const stop=(window.TuColectivoStops||[]).find(s=>String(s.identifier)===String(result.identifier));
    if(!stop)return;
    stop.lines=lines;
    const card=nearbyList.querySelector('[data-stop-id="'+CSS.escape(String(stop.id))+'"]');
    if(card){
      const summary=card.querySelector('span');
      if(summary)summary.textContent=lines.length?lines.length+' '+(lines.length===1?'LÍNEA':'LÍNEAS')+' CONFIRMADAS':'LÍNEAS NO CONFIRMADAS';
      const oldMarkup=card.querySelector('.line-badges');
      if(oldMarkup)oldMarkup.remove();
      const markup=lineMarkup(lines,result.lineLabels||stop.lineLabels||window.TuColectivoLineLabels);
      if(markup)card.insertAdjacentHTML('beforeend',markup);
    }
    if(pendingStopSelectionId===String(result.identifier)){
      pendingStopSelectionId=null;
      if(lines.length){
        chooseStop(stop);
      }else{
        nearbyList.innerHTML='<div class="nearby-empty"><strong>'+escapeHtml(stop.name)+'</strong><span>SMARTMOVE NO CONFIRMÓ LÍNEAS PARA ESTA PARADA. VOLVÉ A LOCALIZAR LAS PARADAS PARA REINTENTAR.</span></div>';
      }
    }
  };

  function chooseStop(stop){
    if(stop.lines.length===1){pendingStopSelectionId=null;showArrival(stop,stop.lines[0]);return;}
    if(!stop.lines.length){
      pendingStopSelectionId=String(stop.identifier);
      nearbyList.innerHTML='<div class="nearby-empty"><strong>'+escapeHtml(stop.name)+'</strong><span>BUSCANDO LÍNEAS QUE PASAN POR ESTA PARADA...</span></div>';
      if(window.TuColectivoNative?.resolveNearbyStopLines){
        window.TuColectivoNative.resolveNearbyStopLines(stop.identifier,stop.lat,stop.lng);
      }else{
        pendingStopSelectionId=null;
        nearbyList.innerHTML='<div class="nearby-empty"><strong>'+escapeHtml(stop.name)+'</strong><span>NO SE PUDIERON CONSULTAR LAS LÍNEAS DE ESTA PARADA.</span></div>';
      }
      return;
    }
    if(!stop.lines.length){
      nearbyList.innerHTML='<div class="nearby-empty"><strong>'+escapeHtml(stop.name)+'</strong><span>SMARTMOVE NO PROPORCIONÓ LÍNEAS CONFIRMADAS PARA ESTA PARADA. NO SE MOSTRARÁN LÍNEAS INVENTADAS.</span></div>';
      return;
    }
    nearbyList.innerHTML='<div class="nearby-empty"><strong>'+escapeHtml(stop.name)+'</strong><span>SELECCIONÁ UNA LÍNEA CONFIRMADA PARA VER ARRIBOS</span>'+lineMarkup(stop.lines,stop.lineLabels)+'</div>'+stop.lines.map(line=>'<button class="data-card stop-line-choice" data-stop-id="'+stop.id+'" data-line="'+line+'" type="button"><b>LÍNEA '+escapeHtml(publicNearbyLineLabel(line,stop.lineLabels))+'</b><span>VER ARRIBOS DE ESTA PARADA</span><em>›</em></button>').join('');
  }

  locateBtn.addEventListener('click',()=>{
    setNearbyState(''); locateBtn.disabled=true;
    if(window.TuColectivoNative&&typeof window.TuColectivoNative.requestNearbyLocation==='function')window.TuColectivoNative.requestNearbyLocation();
    else { locateBtn.disabled=false; setNearbyState(''); }
  });

  nearbyList.addEventListener('click',e=>{
    if(e.target.closest('.favorite-toggle'))return;
    const choice=e.target.closest('.stop-line-choice');
    if(choice){
      const stop=(window.TuColectivoStops||[]).find(s=>String(s.id)===String(choice.dataset.stopId));
      if(stop)showArrival(stop,Number(choice.dataset.line));
      return;
    }
    const card=e.target.closest('.nearby-stop');
    if(card){
      const stop=(window.TuColectivoStops||[]).find(s=>String(s.id)===String(card.dataset.stopId));
      if(stop)chooseStop(stop);
    }
  });

})();

/* FASE 8 — FAVORITOS */
(() => {
  'use strict';
  const list=document.getElementById('favoritesList');
  const STORAGE='tucolectivo:favorites:v1';
  if(!list)return;

  const empty='<div class="favorites-empty"><strong>SIN FAVORITOS_</strong><span>GUARDÁ LÍNEAS O PARADAS PARA VERLAS AQUÍ</span></div>';
  let favorites=load();
  let notificationsEnabled=false;
  const SCHEDULE_STORAGE='tucolectivo:notification-schedule:v1';
  function loadNotificationSchedule(){
    try{
      const value=JSON.parse(localStorage.getItem(SCHEDULE_STORAGE)||'{}');
      return {start:/^([01]\d|2[0-3]):[0-5]\d$/.test(value.start)?value.start:'18:00',end:/^([01]\d|2[0-3]):[0-5]\d$/.test(value.end)?value.end:'19:30'};
    }catch(_){return {start:'18:00',end:'19:30'};}
  }
  let notificationSchedule=loadNotificationSchedule();
  function readNotificationSchedule(){
    return {start:document.getElementById('arrivalNotificationStart')?.value||notificationSchedule.start,end:document.getElementById('arrivalNotificationEnd')?.value||notificationSchedule.end};
  }
  function saveNotificationSchedule(schedule){
    notificationSchedule=schedule;
    try{localStorage.setItem(SCHEDULE_STORAGE,JSON.stringify(schedule));}catch(_){}
  }
  function syncArrivalNotificationConfig(){
    if(!notificationsEnabled)return;
    if(!favorites.arrivals.length){window.TuColectivoNative?.stopArrivalNotifications?.();return;}
    window.TuColectivoNative?.setArrivalNotifications?.(JSON.stringify(favorites.arrivals),notificationSchedule.start,notificationSchedule.end);
  }
  function escapeNotificationText(value){
    return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }
  function renderNotificationHistory(history){
    const host=document.getElementById('recentAlertsList');
    if(!host)return;
    if(!Array.isArray(history)||!history.length){
      host.innerHTML='<li><b>SIN ALERTAS RECIENTES</b><span>Las alertas aparecerán cuando se aproximen los arribos guardados en Favoritos.</span></li>';
      return;
    }
    host.innerHTML=history.slice(0,20).map(item=>{
      const age=Math.max(0,Math.floor((Date.now()-Number(item.timestamp||Date.now()))/60000));
      const when=age<1?'ahora':age+' min';
      return '<li><b>'+escapeNotificationText('LÍNEA '+item.line+' · '+item.message)+'</b><span>'+escapeNotificationText([item.stop,item.destination].filter(Boolean).join(' · '))+'</span><time>'+when+'</time></li>';
    }).join('');
  }
  function syncNotificationButton(){
    const button=document.getElementById('arrivalNotificationsToggle');
    if(!button)return;
    button.textContent=notificationsEnabled?'DESACTIVAR ALERTAS':'ACTIVAR ALERTAS EN ESTE HORARIO';
    button.setAttribute('aria-pressed',String(notificationsEnabled));
    button.classList.toggle('enabled',notificationsEnabled);
  }
  window.onNativeArrivalNotificationsState=function(payload){
    let state={enabled:false,history:[],message:''};
    try{state=typeof payload==='string'?JSON.parse(payload):payload||state;}catch(_){}
    notificationsEnabled=!!state.enabled;
    if(state.startTime&&state.endTime&&state.hasSchedule){
      notificationSchedule={start:String(state.startTime),end:String(state.endTime)};
      const start=document.getElementById('arrivalNotificationStart'),end=document.getElementById('arrivalNotificationEnd');
      if(start)start.value=notificationSchedule.start;
      if(end)end.value=notificationSchedule.end;
      try{localStorage.setItem(SCHEDULE_STORAGE,JSON.stringify(notificationSchedule));}catch(_){}
    }
    syncNotificationButton();
    renderNotificationHistory(state.history);
    if(state.message&&window.setSystemStatus)window.setSystemStatus(state.message,notificationsEnabled?'ok':'warn');
  };
  function load(){
    try{
      const raw=localStorage.getItem(STORAGE);
      const parsed=raw?JSON.parse(raw):{lines:[],stops:[]};
      return {
        lines:Array.isArray(parsed.lines)?parsed.lines.map(Number).filter(n=>n>=1&&n<=14):[],
        stops:Array.isArray(parsed.stops)?parsed.stops.filter(Boolean):[],
        arrivals:Array.isArray(parsed.arrivals)?parsed.arrivals.filter(Boolean):[]
      };
    }catch(_){return {lines:[],stops:[],arrivals:[]};}
  }
  function save(){try{localStorage.setItem(STORAGE,JSON.stringify(favorites));}catch(_){}}
  function has(type,id){return favorites[type].some(v=>String(v)===String(id));}
  function arrivalKey(stop,line){return String(stop.identifier||stop.id||stop.code)+'::'+Number(line);}
  function isArrivalSaved(stop,line){const key=arrivalKey(stop,line);return favorites.arrivals.some(v=>v.key===key);}
  function toggleArrival(stop,line){
    const key=arrivalKey(stop,line);
    if(isArrivalSaved(stop,line))favorites.arrivals=favorites.arrivals.filter(v=>v.key!==key);
    else favorites.arrivals.push({key,line:Number(line),stop:{id:String(stop.id||stop.identifier||stop.code),identifier:String(stop.identifier||stop.id||stop.code),code:Number(stop.code)||0,name:String(stop.name||stop.description||('PARADA '+stop.code)),street:String(stop.street||''),intersection:String(stop.intersection||''),lat:Number(stop.lat??stop.latitude),lng:Number(stop.lng??stop.longitude),lines:Array.isArray(stop.lines)?stop.lines.map(Number):[Number(line)]}});
    save();syncButtons();render();window.TuColectivoNearbySyncArrivalFavorite?.();syncLineArrivalFavorite();syncArrivalNotificationConfig();
  }
  function syncLineArrivalFavorite(){const current=window.TuColectivoCurrentLineArrival;const button=document.querySelector('[data-line-arrival-favorite]');if(!current||!button)return;const on=isArrivalSaved(current.stop,current.line);button.textContent=on?'★ ARRIBO GUARDADO':'☆ AGREGAR ARRIBO A FAVORITOS';button.setAttribute('aria-pressed',String(on));}
  function toggle(type,id){
    const key=String(id);
    favorites[type]=has(type,id)?favorites[type].filter(v=>String(v)!==key):[...favorites[type],type==='lines'?Number(id):key];
    save();syncButtons();render();
  }
  function syncButtons(){
    document.querySelectorAll('.line-card[data-line]').forEach(card=>{
      const line=Number(card.dataset.line), on=has('lines',line), b=card.querySelector('.favorite-toggle');
      if(!b)return;
      b.textContent=on?'★':'☆';b.setAttribute('aria-pressed',String(on));
      b.setAttribute('aria-label',(on?'Quitar LÍNEA ':'Agregar LÍNEA ')+line+' a favoritos');
    });
    document.querySelectorAll('.nearby-stop[data-stop-id],.favorite-stop[data-stop-id]').forEach(card=>{
      const b=card.querySelector('.favorite-toggle'); if(!b)return;
      const on=has('stops',card.dataset.stopId);
      b.textContent=on?'★':'☆';b.setAttribute('aria-pressed',String(on));
    });
  }
  function stopInfo(id){
    const stop=window.TuColectivoStops?.find(s=>s.id===id);
    return stop||null;
  }
  function render(){
    const lines=favorites.lines.slice().sort((a,b)=>a-b);
    const stops=favorites.stops.map(stopInfo).filter(Boolean);
    if(!lines.length&&!stops.length&&!favorites.arrivals.length){list.innerHTML=empty;return;}
    let html='';
    if(lines.length){
      html+='<div class="favorite-group">LÍNEAS FAVORITAS</div>';
      html+=lines.map(n=>'<div class="favorite-item" data-fav-type="lines" data-fav-id="'+n+'"><div><b>LÍNEA '+n+'</b><small>RECORRIDO ACTIVO · ACCESO RÁPIDO</small></div><button class="favorite-remove" type="button" aria-label="Quitar LÍNEA '+n+' de favoritos">×</button></div>').join('');
    }
    if(stops.length){
      html+='<div class="favorite-group">PARADAS FAVORITAS</div>';
      html+=stops.map(s=>'<div class="favorite-item" data-fav-type="stops" data-fav-id="'+s.id+'"><div><b>'+s.name+'</b><small>'+s.lines.length+' '+(s.lines.length===1?'LÍNEA':'LÍNEAS')+' · ACCESO RÁPIDO</small></div><button class="favorite-remove" type="button" aria-label="Quitar parada de favoritos">×</button></div>').join('');
    }
    if(favorites.arrivals.length){
      html+='<div class="favorite-group">ARRIBOS FAVORITOS</div>';
      html+=favorites.arrivals.map(v=>'<div class="favorite-item" data-fav-type="arrivals" data-fav-id="'+v.key+'"><div><b>'+String(v.stop?.name||'PARADA').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))+'</b><small>LÍNEA '+(window.TuColectivoPublicLineLabel?window.TuColectivoPublicLineLabel(Number(v.line)):Number(v.line))+' · ARRIBOS EN TIEMPO REAL</small></div><button class="favorite-remove" type="button" aria-label="Quitar arribo de favoritos">×</button></div>').join('');
    }
    list.innerHTML=html;
  }

  document.addEventListener('click',e=>{
    const notificationToggle=e.target.closest('#arrivalNotificationsToggle');
    if(notificationToggle){
      e.preventDefault();
      if(notificationsEnabled){
        window.TuColectivoNative?.stopArrivalNotifications?.();
      }else if(!favorites.arrivals.length){
        if(window.setSystemStatus)window.setSystemStatus('GUARDÁ AL MENOS UN ARRIBO EN FAVORITOS','warn');
      }else if(window.TuColectivoNative?.setArrivalNotifications){
        const schedule=readNotificationSchedule();
        if(!schedule.start||!schedule.end||schedule.end<=schedule.start){
          if(window.setSystemStatus)window.setSystemStatus('LA HORA FINAL DEBE SER POSTERIOR A LA INICIAL','warn');
          return;
        }
        saveNotificationSchedule(schedule);
        notificationToggle.textContent='CONFIGURANDO HORARIO...';
        window.TuColectivoNative.setArrivalNotifications(JSON.stringify(favorites.arrivals),schedule.start,schedule.end);
      }else if(window.setSystemStatus){
        window.setSystemStatus('PUENTE NATIVO NO DISPONIBLE','error');
      }
      return;
    }
    const arrivalBtn=e.target.closest('.arrival-favorite-toggle');
    if(arrivalBtn){
      e.preventDefault();e.stopPropagation();
      const current=arrivalBtn.hasAttribute('data-line-arrival-favorite')?window.TuColectivoCurrentLineArrival:window.TuColectivoCurrentNearbyArrival;
      if(current)toggleArrival(current.stop,current.line);
      return;
    }
    const toggleBtn=e.target.closest('.favorite-toggle');
    if(toggleBtn){
      e.preventDefault();e.stopPropagation();
      const card=toggleBtn.closest('[data-line],[data-stop-id]');
      if(!card)return;
      if(card.dataset.line)toggle('lines',card.dataset.line);
      else if(card.dataset.stopId)toggle('stops',card.dataset.stopId);
      return;
    }
    const remove=e.target.closest('.favorite-remove');
    if(remove){
      const item=remove.closest('.favorite-item');
      if(item){
        if(item.dataset.favType==='arrivals'){
          favorites.arrivals=favorites.arrivals.filter(v=>v.key!==item.dataset.favId);
          save();render();syncArrivalNotificationConfig();
        }else toggle(item.dataset.favType,item.dataset.favId);
      }
      return;
    }
  });
  document.addEventListener('keydown',e=>{
    const b=e.target.closest('.favorite-toggle');
    if(b&&(e.key==='Enter'||e.key===' ')){e.preventDefault();b.click();return;}
    const stop=e.target.closest('.nearby-stop');
    if(stop&&(e.key==='Enter'||e.key===' ')){
      e.preventDefault();
      const item=window.TuColectivoStops?.find(s=>s.id===stop.dataset.stopId);
      if(item){
        const hasMultiple=item.lines.length>1;
        if(hasMultiple){
          stop.click();
        }else{
          stop.click();
        }
      }
    }
  });
  list.addEventListener('click',e=>{
    if(e.target.closest('.favorite-remove'))return;
    const item=e.target.closest('.favorite-item');
    if(!item)return;
    const type=item.dataset.favType;
    const id=item.dataset.favId;
    if(type==='lines'){
      window.TuColectivo.navigate('lineas');
      setTimeout(()=>document.querySelector('.line-card[data-line="'+id+'"]')?.scrollIntoView({behavior:'smooth',block:'center'}),80);
    }else if(type==='arrivals'){
      const saved=favorites.arrivals.find(v=>v.key===id);
      if(saved){window.TuColectivoCurrentNearbyArrival={stop:{...saved.stop,lines:saved.stop.lines||[saved.line]},line:Number(saved.line)};window.TuColectivo.navigate('arribos');const current=window.TuColectivoCurrentNearbyArrival;const box=document.getElementById('arrivalStop'),target=document.getElementById('arrivalList');box.innerHTML='<strong>'+String(current.stop.name).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))+'</strong><span>LÍNEA '+(window.TuColectivoPublicLineLabel?window.TuColectivoPublicLineLabel(Number(current.line)):current.line)+' · PARADA FAVORITA</span><button class="arrival-favorite-toggle" type="button" aria-pressed="true">★ ARRIBO GUARDADO</button>';target.innerHTML='<div class="nearby-empty"><strong>CONSULTANDO ARRIBOS_</strong><span>OBTENIENDO DATOS REALES DE SMARTMOVE</span></div>';window.TuColectivoNative?.loadArrivals(current.stop.identifier,current.line);}
    }else if(type==='stops'){
      window.TuColectivo.navigate('paradas');
      setTimeout(()=>{
        const stop=window.TuColectivoStops?.find(s=>s.id===id);
        if(stop){
          const nearby=document.getElementById('nearbyList');
          nearby.innerHTML='<div class="nearby-empty"><strong>'+stop.name+'</strong><span>PARADA FAVORITA · SELECCIONÁ UNA LÍNEA PARA VER ARRIBOS</span>'+lineMarkup(stop.lines)+'</div>'+stop.lines.map(line=>'<button class="data-card stop-line-choice" data-stop-id="'+stop.id+'" data-line="'+line+'" type="button"><b>LÍNEA '+line+'</b><span>VER ARRIBOS DE ESTA PARADA</span><em>›</em></button>').join('');
        }
      },80);
    }
  });
  document.addEventListener('app:navigate',e=>{if(e.detail.go==='favoritos'){render();window.TuColectivoNative?.getArrivalNotificationState?.();}});
  window.TuColectivoFavorites={toggle,render,toggleArrival,isArrivalSaved,syncLineArrivalFavorite};
  const scheduleStart=document.getElementById('arrivalNotificationStart'),scheduleEnd=document.getElementById('arrivalNotificationEnd');
  if(scheduleStart)scheduleStart.value=notificationSchedule.start;
  if(scheduleEnd)scheduleEnd.value=notificationSchedule.end;
  [scheduleStart,scheduleEnd].forEach(input=>input?.addEventListener('change',()=>saveNotificationSchedule(readNotificationSchedule())));
  render();syncButtons();syncNotificationButton();
  window.TuColectivoNative?.getArrivalNotificationState?.();
  window.setInterval(()=>{
    if(document.querySelector('[data-screen="favoritos"]')?.classList.contains('active'))window.TuColectivoNative?.getArrivalNotificationState?.();
  },30000);
})();

/* FASE 11B — LÍNEAS REALES + SUBSECCIONES DESDE SMARTMOVE NATIVO */
(() => {
  'use strict';
  const lineScreen=document.querySelector('[data-screen="lineas"]');
  if(!lineScreen)return;
  const lineGrid=lineScreen.querySelector('.lineas-grid');
  const lineHead=lineScreen.querySelector('.screen-head small');
  const setLineHead=text=>{if(lineHead)lineHead.textContent=text;};
  const nativeApi=window.TuColectivoNative;
  if(!lineGrid)return;
  let lines=[],currentLine=null,currentStreet=null,currentIntersection=null,currentStop=null;
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const loading=t=>lineGrid.innerHTML='<div class="nearby-empty"><strong>'+esc(t)+'</strong><span>CONSULTANDO SERVICIO SMARTMOVE_</span></div>';
  const renderLines=items=>{
    lines=Array.isArray(items)?items:[];
    setLineHead(lines.length+' RUTAS');
    lineGrid.innerHTML=lines.map(l=>{const publicName=window.TuColectivoPublicLineLabel?window.TuColectivoPublicLineLabel(l.code,window.TuColectivoLineLabels):String(l.name||l.code);const displayName='LINEA '+publicName;return '<button class="line-card" data-line="'+l.code+'"><span class="line-badge">'+esc(displayName)+'</span><div><b>'+esc(displayName)+'</b></div><em>›</em></button>';}).join('');
    if(!lines.length)loading('SIN LÍNEAS');
    if(window.TuColectivoFavorites?.render)window.TuColectivoFavorites.render();
  };
  const showList=(title,subtitle,items,empty,onClick,itemLabel='')=>{
    const context=String(subtitle??'').trim();
    lineGrid.innerHTML='<div class="line-sub-head"><strong>'+esc(title)+'</strong>'+(context?'<small class="line-sub-context">'+esc(context)+'</small>':'')+'</div>';
    if(!items.length){lineGrid.insertAdjacentHTML('beforeend','<div class="nearby-empty"><strong>'+esc(empty)+'</strong><span>NO HAY DATOS PARA ESTA SELECCIÓN_</span></div>');return;}
    items.forEach(item=>{const b=document.createElement('button');b.type='button';b.className='data-card line-detail-card';const title=item.name||itemLabel||'';const secondary=item.name&&itemLabel?itemLabel:'';b.innerHTML='<b class="line-detail-title">'+esc(title)+'</b>'+(secondary?'<span>'+esc(secondary)+'</span>':'')+'<em>›</em>';b.addEventListener('click',()=>onClick(item));lineGrid.appendChild(b);});
  };
  const showError=(title,msg)=>showList(title,'',[],msg,()=>{});
  window.onNativeLines=p=>{
    try{
      const items=JSON.parse(p);
      window.TuColectivoLineLabels=window.TuColectivoLineLabels||{};
      items.forEach(item=>{
        const code=Number(item.code);
        if(Number.isFinite(code)&&code>0) window.TuColectivoLineLabels[String(code)]=String(item.name||'').trim();
      });
      window.TuColectivoLineSyncResult?.(true, items.length);
      renderLines(items.map(item=>({
        ...item,
        name:window.TuColectivoPublicLineLabel
          ? window.TuColectivoPublicLineLabel(item.code,window.TuColectivoLineLabels)
          : item.name
      })));
    }catch(_){showError('LÍNEAS','RESPUESTA INVÁLIDA');}
  };
  window.onNativeLinesError=p=>{
    let message='ERROR SMARTMOVE';
    try{message=JSON.parse(p).message||message;}catch(_){}
    window.TuColectivoLineSyncResult?.(false, 0, message);
    showError('LÍNEAS',message);
  };
  const cleanStreetName=name=>String(name??'').replace(/\s*(?:,|-)?\s*SAN LUIS\s*$/i,'').trim();
  window.onNativeStreets=p=>{
    const a=JSON.parse(p).map(x=>({...x,name:cleanStreetName(x.name)}));
    showList('CALLES','',a,'SIN CALLES',x=>{currentStreet=x;nativeApi.loadIntersections(currentLine.code,x.code);},'');
  };
  window.onNativeStreetsError=p=>showError('CALLES PRINCIPALES',JSON.parse(p).message);
  window.onNativeIntersections=p=>{const a=JSON.parse(p).map(x=>({...x,name:cleanStreetName(x.name)}));showList('INTERSECCIONES','CALLE: '+(currentStreet?.name||''),a,'SIN INTERSECCIONES',x=>{currentIntersection=x;nativeApi.loadStops(currentLine.code,currentStreet.code,x.code);},'');};
  window.onNativeIntersectionsError=p=>showError('INTERSECCIONES',JSON.parse(p).message);
  window.onNativeStops=p=>{const a=JSON.parse(p);currentStop=null;showList('PARADAS','INTERSECCIÓN: '+(currentIntersection?.name||''),a,'SIN PARADAS',showArrivals,'ARRIBOS');};
  window.onNativeStopsError=p=>showError('PARADAS',JSON.parse(p).message);
  const ARRIVING_DELAY_MS=35000;
  const arrivingTimers=new Map();

  // El backend devuelve minutos enteros y puede enviar varias veces el mismo
  // arribo. El estado de "1 MIN" debe sobrevivir a los repaints del listado:
  // no usamos el índice del array porque puede cambiar entre respuestas.
  const arrivalIdentity=(x)=>{
    const vehicle=String(x.vehicleId??x.tripId??x.id??'').trim();
    if(vehicle)return 'vehicle:'+vehicle;
    const line=String(x.line??currentLine?.code??'').trim().toUpperCase();
    const destination=String(x.destination??'').trim().toUpperCase();
    const stop=String(currentStop?.identifier??currentStop?.code??'').trim();
    return 'route:'+line+'|'+destination+'|'+stop;
  };

  const renderLineArrivals=(items,list)=>{
    const a=Array.isArray(items)?items:[];
    if(!list)return;
    list.innerHTML=a.length?a.map(x=>{
      // SmartMove puede entregar los minutos como número o como texto (por ejemplo "1 MIN").
      // Normalizamos el valor y corregimos el caso en que el texto llegue con formato "1 MIN".
      const rawMinutes=x.minutes;
      const minuteMatch=typeof rawMinutes==='number'
        ? rawMinutes
        : Number.parseFloat(String(rawMinutes??'').replace(',', '.').match(/-?\d+(?:\.\d+)?/)?.[0]||'');
      const m=Number.isFinite(minuteMatch)?minuteMatch:NaN;
      const arrivalKey=arrivalIdentity(x);
      const previous=arrivingTimers.get(arrivalKey);
      // SmartMove también devuelve el estado textual original en "status".
      // Lo usamos como frontera real del estado: una vez que mostramos
      // ARRIBANDO no volvemos a dibujar minutos por un repaint/respuesta
      // duplicada que conserve el mismo estado del backend.
      const backendStatus=String(x.status??'').trim().toUpperCase();
      const statusChanged=previous?.arrivingStatus!=null && backendStatus!==previous.arrivingStatus;
      let arriving=Number.isFinite(m)&&m<=0;

      if(previous?.forcedArriving){
        if(statusChanged){
          if(previous.timer)clearTimeout(previous.timer);
          arrivingTimers.delete(arrivalKey);
        }else{
          // Mantener ARRIBANDO mientras SmartMove siga reportando el mismo
          // estado, aunque el valor numérico vuelva a ser 1, 2, etc.
          arriving=true;
        }
      }
      if(Number.isFinite(m)&&m===1 && !arriving){
        if(previous?.forcedArriving && !statusChanged){
          arriving=true;
        }else if(!previous || statusChanged){
          const state={forcedArriving:false,timer:null,firstOneAt:Date.now(),arrivingStatus:null};
          state.timer=setTimeout(()=>{
            const current=arrivingTimers.get(arrivalKey);
            if(!current||current!==state)return;
            current.forcedArriving=true;
            current.arrivingStatus=backendStatus;
            current.timer=null;
            arrivingTimers.set(arrivalKey,current);
            if(list.isConnected)renderLineArrivals(a,list);
          },ARRIVING_DELAY_MS);
          arrivingTimers.set(arrivalKey,state);
        }
      }
      const d=Number.isFinite(m)?Math.max(1.2,Math.min(18,m*.35)):8;
      const rawDestination=String(x.destination||'').replace(/[.·•‧∙⋅。．]+/g,' ').replace(/[\u200B-\u200D\uFEFF]/g,' ').replace(/\s+/g,' ').trim().replace(/[.·•‧∙⋅。．]+$/,'').trim();
      const unknownDestination=!rawDestination||/^(?:A{1,2}|N\/?A|S\/?D|DESTINO|SERVICIO)$/i.test(rawDestination);
      const destination=unknownDestination?'':rawDestination;
      const publicLine=window.TuColectivoPublicLineLabel?window.TuColectivoPublicLineLabel(x.line||currentLine?.code||currentLine?.name,window.TuColectivoLineLabels):String(x.line||currentLine?.name||'');
      const arrivalLineTitle='LINEA '+publicLine;
      const arrivalDestination=destination?'HACIA '+destination.toUpperCase():'DESTINO NO INFORMADO';
      if(arriving){
        return '<div class="arrival-card arrival-arriving"><div class="arrival-copy"><b>'+esc(arrivalLineTitle)+'</b><span>'+esc(arrivalDestination)+'</span></div><em class="arrival-time arrival-time-arriving"><strong>ARRIBANDO</strong></em></div>';
      }
      return '<div class="arrival-card"><div class="arrival-copy"><b>'+esc(arrivalLineTitle)+'</b><span>'+esc(arrivalDestination)+'</span></div><em class="arrival-time" style="--arrival-duration:'+d+'s"><svg class="arrival-ring" viewBox="0 0 80 80" aria-hidden="true"><circle class="arrival-ring-base" cx="40" cy="40" r="35"></circle><g class="arrival-ring-orbit"><circle class="arrival-ring-arc" cx="40" cy="40" r="35"></circle><circle class="arrival-ring-dot" cx="38.78" cy="74.98" r="3.2"></circle></g></svg><strong>'+esc(x.minutes==null?'--':x.minutes)+'</strong><small>MIN</small></em></div>';
    }).join(''):'<div class="nearby-empty"><strong>SIN ARRIBOS</strong><span>SMARTMOVE NO DEVOLVIÓ SERVICIOS PARA ESTA PARADA</span></div>';
  };
  window.onNativeArrivals=p=>{
    if(window.TuColectivoMap?.handleArrivals(p))return;
    if(window.TuColectivoCurrentNearbyArrival&&document.querySelector('[data-screen="arribos"]')?.classList.contains('active')){window.onNativeNearbyArrivals?.(p);return;}
    const a=JSON.parse(p),list=lineGrid.querySelector('.line-arrivals');
    renderLineArrivals(a,list);
  };
  window.onNativeArrivalsError=p=>{if(window.TuColectivoMap?.handleArrivalsError(p))return;if(window.TuColectivoCurrentNearbyArrival&&document.querySelector('[data-screen="arribos"]')?.classList.contains('active')){window.onNativeNearbyArrivalsError?.(p);return;}const list=lineGrid.querySelector('.line-arrivals');if(list)list.innerHTML='<div class="nearby-empty"><strong>ERROR DE ARRIBOS</strong><span>'+esc(JSON.parse(p).message)+'</span></div>';};
  function showArrivals(stop){
    currentStop=stop;
    setLineHead('ARRIBOS');
    const favKey=String(stop.identifier||stop.code)+'::'+Number(currentLine.code);
    window.TuColectivoCurrentLineArrival={stop:{id:String(stop.identifier||stop.code),identifier:String(stop.identifier||stop.code),code:Number(stop.code)||0,name:stop.description,street:stop.street||'',intersection:stop.intersection||'',lat:Number(stop.latitude),lng:Number(stop.longitude),lines:[Number(currentLine.code)]},line:Number(currentLine.code)};
    const stopContext=[stop.street,stop.intersection].map(v=>String(v||'').trim()).filter(Boolean).join(' · ');lineGrid.innerHTML='<div class="line-sub-head"><strong>'+esc(stop.description)+'</strong>'+(stopContext?'<small class="line-sub-context">'+esc(stopContext)+'</small>':'')+'</div><button class="arrival-favorite-toggle" data-line-arrival-favorite="true" type="button" aria-pressed="false">☆ AGREGAR ARRIBO A FAVORITOS</button><button class="arrival-refresh" type="button" data-arrival-refresh="true">↻ ACTUALIZAR MINUTOS</button><div class="arrival-list line-arrivals"><div class="nearby-empty"><strong>CONSULTANDO ARRIBOS_</strong><span>ESPERÁ LA RESPUESTA DE SMARTMOVE</span></div></div><button class="map-route-action" data-map-route-line="'+Number(currentLine.code)+'" type="button">⌖ VER RECORRIDO EN EL MAPA</button>';
    window.TuColectivoFavorites?.syncLineArrivalFavorite?.();
    nativeApi.loadArrivals(stop.identifier,currentLine.code);
  }
  function loadLines(){if(nativeApi){loading('CARGANDO LÍNEAS');nativeApi.loadLines();}else showError('LÍNEAS','PUENTE ANDROID NO DISPONIBLE');}
  function openLine(l){currentLine=l;setLineHead(l.name.toUpperCase());showList(l.name,'CALLES PRINCIPALES',[], 'CARGANDO CALLES',()=>{});nativeApi.loadStreets(l.code);}
  function back(){
    if(currentStop){currentStop=null;setLineHead('PARADAS');nativeApi.loadStops(currentLine.code,currentStreet.code,currentIntersection.code);return true;}
    if(currentIntersection){currentIntersection=null;setLineHead(currentStreet.name);nativeApi.loadIntersections(currentLine.code,currentStreet.code);return true;}
    if(currentStreet){currentStreet=null;setLineHead(currentLine.name);nativeApi.loadStreets(currentLine.code);return true;}
    if(currentLine){currentLine=null;renderLines(lines);return true;}
    window.TuColectivo.navigate('inicio');
    return true;
  }
  lineGrid.addEventListener('click',e=>{
    const refresh=e.target.closest('[data-arrival-refresh]');
    if(refresh&&currentStop&&currentLine){
      refresh.disabled=true;
      refresh.textContent='↻ ACTUALIZANDO...';
      const list=lineGrid.querySelector('.line-arrivals');
      if(list)list.innerHTML='<div class="nearby-empty"><strong>ACTUALIZANDO MINUTOS_</strong><span>CONSULTANDO ARRIBOS EN TIEMPO REAL</span></div>';
      nativeApi.loadArrivals(currentStop.identifier,currentLine.code);
      setTimeout(()=>{if(refresh.isConnected){refresh.disabled=false;refresh.textContent='↻ ACTUALIZAR MINUTOS';}},1200);
      return;
    }
    const mapBtn=e.target.closest('[data-map-route-line]');if(mapBtn){const code=Number(mapBtn.dataset.mapRouteLine);if(code&&window.TuColectivoMap)window.TuColectivoMap.showRoute(code);return;}
    const c=e.target.closest('.line-card');if(c){const l=lines.find(x=>String(x.code)===c.dataset.line);if(l)openLine(l);return;}
  });
  document.addEventListener('app:navigate',e=>{if(e.detail.go==='lineas'){currentLine=null;currentStreet=null;currentIntersection=null;currentStop=null;loadLines();}});
  window.TuColectivoLineBack=back;
  const listStyle=document.createElement('style');
  listStyle.textContent='.line-sub-head{display:flex;flex-direction:column;align-items:flex-start;gap:7px}.line-sub-head>strong{display:block;line-height:1.2}.line-sub-head>.line-sub-context{display:block;max-width:100%;line-height:1.35;overflow-wrap:anywhere;white-space:normal;opacity:.9}';
  document.head.appendChild(listStyle);
  if(nativeApi)loadLines();
})();
/* CÍRCULO DE ARRIBOS — MISMA LÓGICA VISUAL DE LA RAMA NATIVA */
(()=>{
 const css=document.createElement('style');
 css.textContent=`
 .line-card small{display:none!important}
 .data-card.line-detail-card span{display:block!important}
 .arrival-card{display:flex!important;align-items:center!important;justify-content:space-between!important;min-height:88px!important;padding:10px 14px!important;box-sizing:border-box!important}
 .arrival-refresh{display:block!important;width:100%!important;min-height:44px!important;margin:8px 0 10px!important;padding:10px 14px!important;border:1px solid var(--vt)!important;background:rgba(0,240,255,.08)!important;color:var(--vt)!important;font-family:inherit!important;font-weight:800!important;letter-spacing:.08em!important;border-radius:10px!important;box-shadow:0 0 10px rgba(0,240,255,.12)!important}
 .arrival-refresh:disabled{opacity:.55!important}
 .arrival-arriving .arrival-copy{padding-right:8px}
 .arrival-time-arriving{flex:0 0 auto!important;width:auto!important;min-width:92px!important;height:42px!important}
 .arrival-time-arriving strong{position:static!important;transform:none!important;font-size:15px!important;letter-spacing:.08em!important;color:var(--vt)!important;text-shadow:0 0 8px var(--vt)!important}

 .arrival-copy{min-width:0;display:flex;flex-direction:column;justify-content:center;gap:7px;flex:1;padding-right:12px}
 .arrival-copy b{line-height:1.15!important}
 .arrival-copy span{line-height:1.15!important;white-space:normal!important}
 .arrival-time{position:relative!important;flex:0 0 70px!important;width:70px!important;height:70px!important;display:flex!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:3px!important;box-sizing:border-box!important}
 .arrival-ring{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;overflow:visible!important;transform:rotate(-90deg)!important}
  .arrival-ring-orbit{transform-box:view-box!important;transform-origin:40px 40px!important;animation:arrivalNativeOrbit var(--arrival-duration,1.5s) linear infinite!important}
  .arrival-ring-dot{fill:var(--vt);stroke:var(--vt);stroke-width:1;filter:drop-shadow(0 0 5px var(--vt));animation:none!important}
 .arrival-ring-base{fill:none;stroke:var(--cy);stroke-width:4;filter:drop-shadow(0 0 5px var(--cy))}
 .arrival-ring-arc{fill:none;stroke:var(--vt);stroke-width:4;stroke-linecap:round;stroke-dasharray:56.2 163.7;filter:drop-shadow(0 0 6px var(--vt));transform-origin:40px 40px;animation:none!important}
 .arrival-time strong,.arrival-time small{position:static!important;left:auto!important;z-index:5!important;line-height:1!important;margin:0!important;white-space:nowrap!important;transform:none!important}
 .arrival-time strong{font-size:15px!important}
 .arrival-time small{font-size:8px!important;letter-spacing:.04em!important}
 @keyframes arrivalNativeOrbit{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}
 `;
 document.head.appendChild(css);
})();
