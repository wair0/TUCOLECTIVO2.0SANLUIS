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
    if (id === 'm-bell') { const d = $('.dot', btn); if (d) d.remove(); }
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
    const b = e.target.closest('.btn,.item,.chip,.home-card,.data-card,.nav-btn,.sync-btn');
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

  $('#sync').addEventListener('click', () => {
    const b = $('#sync');
    b.classList.remove('done');
    b.classList.add('busy');
    b.querySelector('span').textContent = 'SINCRONIZANDO...';
    setStatus('SINCRONIZANDO...', 'busy');
    setTimeout(() => {
      b.classList.remove('busy');
      b.classList.add('done');
      b.querySelector('span').textContent = 'LÍNEAS ACTUALIZADAS';
      b.querySelector('b').textContent = '14';
      setStatus('14 LÍNEAS', 'ok');
      emit('app:status', { text: '14 LÍNEAS', state: 'ok' });
    }, 1100);
  });

  document.addEventListener('app:status', e => setStatus(e.detail.text, e.detail.state));
  window.setSystemStatus = setStatus;
  window.TuColectivo = { setStatus, closeMenus: closeAll, navigate: go };
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
  const ctx = canvas.getContext('2d');
  const TILE = 256;
  const FALLBACK = {lat:-33.3017,lng:-66.3378};
  const map = {lat:FALLBACK.lat,lng:FALLBACK.lng,zoom:14,drag:false,pointers:new Map(),last:null,baseDistance:0,baseZoom:14};
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
    drawMarker(w/2,h/2);
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
    if(!navigator.geolocation){state.textContent='GPS NO DISPONIBLE';return;}
    state.textContent='LOCALIZANDO...';
    navigator.geolocation.getCurrentPosition(
      p=>{setCenter(p.coords.latitude,p.coords.longitude);state.textContent='UBICACIÓN ACTIVA';},
      ()=>{state.textContent='GPS NO DISPONIBLE';},
      {enableHighAccuracy:true,timeout:10000,maximumAge:30000}
    );
  }
  function distance(a,b){return Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);}
  canvas.addEventListener('pointerdown',e=>{
    canvas.setPointerCapture(e.pointerId);
    map.pointers.set(e.pointerId,e);
    if(map.pointers.size===1){map.drag=true;map.last=e;}
    else if(map.pointers.size===2){map.drag=false;const p=[...map.pointers.values()];map.baseDistance=distance(p[0],p[1]);map.baseZoom=map.zoom;}
  });
  canvas.addEventListener('pointermove',e=>{
    if(!map.pointers.has(e.pointerId))return;
    map.pointers.set(e.pointerId,e);
    if(map.pointers.size===1&&map.drag&&map.last){
      const dx=e.clientX-map.last.clientX,dy=e.clientY-map.last.clientY;
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
    map.pointers.delete(e.pointerId);if(map.pointers.size===0){map.drag=false;map.last=null;}
  }));
  canvas.addEventListener('wheel',e=>{e.preventDefault();zoomAt(e.deltaY<0?1:-1);},{passive:false});
  plus.addEventListener('click',()=>zoomAt(1));
  minus.addEventListener('click',()=>zoomAt(-1));
  locate.addEventListener('click',locateUser);
  window.addEventListener('resize',resize);
  document.addEventListener('app:navigate',e=>{if(e.detail.go==='mapa')setTimeout(resize,60);});
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
  if(!locateBtn||!nearbyList)return;

  const stops=[
    {id:'colón-cervantes',name:'AV. COLÓN Y CERVANTES',lat:-33.2989,lng:-66.3371,lines:[1,2,12]},
    {id:'pringles-illia',name:'PLAZA PRINGLES · PRINGLES Y RIVADAVIA',lat:-33.3012,lng:-66.3379,lines:[3,4]},
    {id:'terminal',name:'TERMINAL DE ÓMNIBUS · AV. ILLIA Y ESTEBAN ADARO',lat:-33.3005,lng:-66.3208,lines:[1,6,12]},
    {id:'san-martin-junín',name:'SAN MARTÍN Y JUNÍN',lat:-33.3019,lng:-66.3370,lines:[2,5,9]},
    {id:'riobamba-juan',name:'RIobamba Y JUAN SAÁ',lat:-33.2969,lng:-66.3410,lines:[7,10,14]}
  ];
  window.TuColectivoStops=stops;

  function meters(a,b){
    const R=6371000,rad=Math.PI/180;
    const dLat=(b.lat-a.lat)*rad,dLng=(b.lng-a.lng)*rad;
    const x=Math.sin(dLat/2)**2+Math.cos(a.lat*rad)*Math.cos(b.lat*rad)*Math.sin(dLng/2)**2;
    return Math.round(R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x)));
  }
  function formatDistance(m){return m<1000?Math.max(1,m)+' M':(m/1000).toFixed(1).replace('.',',')+' KM';}
  function lineMarkup(lines){return '<div class="line-badges">'+lines.map(n=>'<span class="line-badge-mini">LÍNEA '+n+'</span>').join('')+'</div>';}

  function renderNearby(position){
    const here={lat:position.coords.latitude,lng:position.coords.longitude};
    const ranked=stops.map(stop=>({...stop,distance:meters(here,stop)})).sort((a,b)=>a.distance-b.distance).slice(0,5);
    nearbyList.innerHTML=ranked.map(stop=>'<button class="data-card nearby-stop" data-stop-id="'+stop.id+'" type="button"><b>'+stop.name+'</b><span>'+stop.lines.length+' '+(stop.lines.length===1?'LÍNEA':'LÍNEAS')+' DISPONIBLES</span>'+lineMarkup(stop.lines)+'<em>'+formatDistance(stop.distance)+'</em></button>').join('');
    nearbyState.textContent='GPS: ACTIVO';
    nearbyState.style.color='var(--cy)';
  }

  function showArrival(stop,line){
    const minutes=Math.max(2,(line*3)%17+2);
    arrivalStop.innerHTML='<strong>'+stop.name+'</strong><span>LÍNEA '+line+' · PARADA SELECCIONADA</span>';
    arrivalList.innerHTML='<div class="arrival-card"><b>LÍNEA '+line+'</b><span>PRÓXIMO ARRIBO ESTIMADO</span><em class="arrival-time"><strong>'+minutes+'</strong><small>MIN</small></em></div><div class="arrival-card"><b>LÍNEA '+line+'</b><span>SIGUIENTE SERVICIO</span><em class="arrival-time"><strong>'+Math.min(59,minutes+8)+'</strong><small>MIN</small></em></div>';
    arrivalsMeta.textContent='LÍNEA '+line;
    window.TuColectivo.navigate('arribos');
  }

  function chooseStop(stop){
    if(stop.lines.length===1){showArrival(stop,stop.lines[0]);return;}
    nearbyList.innerHTML='<div class="nearby-empty"><strong>'+stop.name+'</strong><span>SELECCIONÁ LA LÍNEA PARA VER SUS ARRIBOS</span>'+lineMarkup(stop.lines)+'</div>'+stop.lines.map(line=>'<button class="data-card stop-line-choice" data-stop-id="'+stop.id+'" data-line="'+line+'" type="button"><b>LÍNEA '+line+'</b><span>VER ARRIBOS DE ESTA PARADA</span><em>›</em></button>').join('');
  }

  locateBtn.addEventListener('click',()=>{
    if(!navigator.geolocation){nearbyState.textContent='GPS: NO DISPONIBLE';return;}
    nearbyState.textContent='GPS: LOCALIZANDO...';
    locateBtn.disabled=true;
    navigator.geolocation.getCurrentPosition(
      position=>{locateBtn.disabled=false;renderNearby(position);},
      error=>{locateBtn.disabled=false;nearbyState.textContent=error&&error.code===1?'GPS: PERMISO DENEGADO':'GPS: SIN SEÑAL';nearbyList.innerHTML='<div class="nearby-empty"><strong>NO SE PUDIERON LOCALIZAR PARADAS_</strong><span>VERIFICÁ EL PERMISO DE UBICACIÓN Y VOLVÉ A INTENTAR</span></div>';},
      {enableHighAccuracy:true,timeout:10000,maximumAge:30000}
    );
  });

  nearbyList.addEventListener('click',e=>{
    const choice=e.target.closest('.stop-line-choice');
    if(choice){
      const stop=stops.find(s=>s.id===choice.dataset.stopId);
      if(stop)showArrival(stop,Number(choice.dataset.line));
      return;
    }
    const card=e.target.closest('.nearby-stop');
    if(card){
      const stop=stops.find(s=>s.id===card.dataset.stopId);
      if(stop)chooseStop(stop);
    }
  });

  arrivalsBack.addEventListener('click',()=>window.TuColectivo.navigate('paradas'));
})();

/* FASE 8 — FAVORITOS */
(() => {
  'use strict';
  const list=document.getElementById('favoritesList');
  const STORAGE='tucolectivo:favorites:v1';
  if(!list)return;

  const empty='<div class="favorites-empty"><strong>SIN FAVORITOS_</strong><span>GUARDÁ LÍNEAS O PARADAS PARA VERLAS AQUÍ</span></div>';
  let favorites=load();

  function load(){
    try{
      const raw=localStorage.getItem(STORAGE);
      const parsed=raw?JSON.parse(raw):{lines:[],stops:[]};
      return {
        lines:Array.isArray(parsed.lines)?parsed.lines.map(Number).filter(n=>n>=1&&n<=14):[],
        stops:Array.isArray(parsed.stops)?parsed.stops.filter(Boolean):[]
      };
    }catch(_){return {lines:[],stops:[]};}
  }
  function save(){try{localStorage.setItem(STORAGE,JSON.stringify(favorites));}catch(_){}}
  function has(type,id){return favorites[type].some(v=>String(v)===String(id));}
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
    if(!lines.length&&!stops.length){list.innerHTML=empty;return;}
    let html='';
    if(lines.length){
      html+='<div class="favorite-group">LÍNEAS FAVORITAS</div>';
      html+=lines.map(n=>'<div class="favorite-item" data-fav-type="lines" data-fav-id="'+n+'"><div><b>LÍNEA '+n+'</b><small>RECORRIDO ACTIVO · ACCESO RÁPIDO</small></div><button class="favorite-remove" type="button" aria-label="Quitar LÍNEA '+n+' de favoritos">×</button></div>').join('');
    }
    if(stops.length){
      html+='<div class="favorite-group">PARADAS FAVORITAS</div>';
      html+=stops.map(s=>'<div class="favorite-item" data-fav-type="stops" data-fav-id="'+s.id+'"><div><b>'+s.name+'</b><small>'+s.lines.length+' '+(s.lines.length===1?'LÍNEA':'LÍNEAS')+' · ACCESO RÁPIDO</small></div><button class="favorite-remove" type="button" aria-label="Quitar parada de favoritos">×</button></div>').join('');
    }
    list.innerHTML=html;
  }

  document.addEventListener('click',e=>{
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
      if(item)toggle(item.dataset.favType,item.dataset.favId);
      return;
    }
  });
  document.addEventListener('keydown',e=>{
    const b=e.target.closest('.favorite-toggle');
    if(b&&(e.key==='Enter'||e.key===' ')){e.preventDefault();b.click();}
  });
  document.addEventListener('app:navigate',e=>{if(e.detail.go==='favoritos')render();});
  window.TuColectivoFavorites={toggle,render};
  render();syncButtons();
})();
