/* Integración del header y la barra originales con la navegación WebView existente. */
(function () {
  'use strict';
  var sections = ['inicio', 'lineas', 'mapa', 'paradas', 'favoritos'];
  var header = window.TcHeader;
  var nav = window.TcBottomNav;
  function validSection(name) { return sections.indexOf(name) !== -1; }
  function syncSection(name) {
    if (!validSection(name)) return;
    if (nav) nav.setActive(name);
    if (header) header.setSection(name);
  }
  function navigate(name) {
    if (!validSection(name)) return;
    window.TuColectivo && window.TuColectivo.navigate(name);
    if (header) header.closeAll();
    window.TuColectivoHeaderOpen = false;
    syncSection(name);
  }
  if (nav) {
    nav.onChange = function (section) {
      if (window.TuColectivo && typeof window.TuColectivo.navigate === 'function') {
        window.TuColectivo.navigate(section);
      }
      if (header) {
        header.closeAll();
        header.setSection(section);
      }
      window.TuColectivoHeaderOpen = false;
    };
  }
  if (header) {
    header.onMenuSelect = function (section) {
      if (window.TuColectivo && typeof window.TuColectivo.navigate === 'function') {
        window.TuColectivo.navigate(section);
      }
      if (nav) nav.setActive(section);
      window.TuColectivoHeaderOpen = false;
    };
    function dispatchSearch(query, filter) {
      document.dispatchEvent(new CustomEvent('app:search', {
        detail: { q: String(query || '').trim(), filter: filter || 'todo' }
      }));
    }
    var searchPanel = document.getElementById('tc-hd-search');
    var searchResults = document.createElement('div');
    searchResults.id = 'tc-hd-results';
    searchResults.setAttribute('role','listbox');
    searchResults.setAttribute('aria-label','Resultados de búsqueda');
    searchResults.style.cssText = 'max-height:38vh;overflow-y:auto;display:grid;gap:6px;padding:4px 0 8px';
    searchPanel.insertBefore(searchResults,searchPanel.querySelector('.tc-hd-hint'));
    var knownLines = [];
    var linesLoading = false;
    var currentSearch = {query:'',filter:'todo'};
    function normalize(value) {
      return String(value == null ? '' : value).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
    }
    function storedFavorites() {
      try { return JSON.parse(localStorage.getItem('tucolectivo:favorites:v1') || '{}'); }
      catch (_) { return {}; }
    }
    function searchEntries() {
      var entries = [];
      var labels = window.TuColectivoLineLabels || {};
      knownLines.forEach(function(line) {
        var label = window.TuColectivoPublicLineLabel ? window.TuColectivoPublicLineLabel(line.code,labels) : (line.name || line.code);
        entries.push({type:'line',code:line.code,label:'LÍNEA '+label,extra:'RECORRIDO · '+line.code,search:label+' '+line.name+' '+line.code});
      });
      (window.TuColectivoStops || []).forEach(function(stop) {
        entries.push({type:'stop',id:stop.id,label:stop.name || 'PARADA',extra:'PARADA · '+(stop.street || ''),search:[stop.name,stop.street,stop.intersection,stop.identifier,stop.code].join(' ')});
      });
      var fav=storedFavorites();
      (fav.lines || []).forEach(function(code) {
        if(!entries.some(function(e){return e.type==='line'&&String(e.code)===String(code);}))
          entries.push({type:'line',code:code,label:'LÍNEA '+(labels[code]||code),extra:'LÍNEA FAVORITA',search:(labels[code]||'')+' '+code});
      });
      (fav.arrivals || []).forEach(function(item) {
        if(item.stop)entries.push({type:'arrival',item:item,label:item.stop.name || 'ARRIBO',extra:'ARRIBO FAVORITO · LÍNEA '+(item.lineLabel||item.line),search:[item.stop.name,item.stop.street,item.lineLabel,item.line].join(' ')});
      });
      (fav.stops || []).forEach(function(id) {
        if(!entries.some(function(e){return e.type==='stop'&&String(e.id)===String(id);}))
          entries.push({type:'favoriteStop',id:id,label:'PARADA '+id,extra:'PARADA FAVORITA',search:id});
      });
      return entries;
    }
    function matchesFilter(item,filter) {
      if(filter==='todo')return true;
      if(filter==='lineas')return item.type==='line';
      if(filter==='paradas')return item.type==='stop'||item.type==='favoriteStop';
      if(filter==='favoritos'){
        var fav=storedFavorites();
        return item.type==='arrival'||(item.type==='line'&&(fav.lines||[]).some(function(v){return String(v)===String(item.code);}))||
          ((item.type==='stop'||item.type==='favoriteStop')&&(fav.stops||[]).some(function(v){return String(v)===String(item.id);}));
      }
      return filter==='calles'&&item.type==='street';
    }
    function renderSearch(query,filter) {
      currentSearch={query:String(query||'').trim(),filter:filter||'todo'};
      searchResults.textContent='';
      if(!currentSearch.query){searchResults.textContent='ESCRIBÍ PARA BUSCAR';return;}
      var term=normalize(currentSearch.query);
      var entries=searchEntries().filter(function(item){return matchesFilter(item,currentSearch.filter)&&normalize(item.search+' '+item.label).includes(term);}).slice(0,30);
      if(!entries.length){searchResults.textContent=currentSearch.filter==='calles'?'LAS CALLES SE CONSULTAN AL SELECCIONAR UNA LÍNEA':'SIN RESULTADOS · SINCRONIZÁ LÍNEAS SI TODAVÍA NO APARECEN';return;}
      entries.forEach(function(item,index){
        var button=document.createElement('button');button.type='button';button.className='tc-hd-result';
        button.style.cssText='display:flex;flex-direction:column;align-items:flex-start;gap:4px;width:100%;padding:11px 13px;border:1px solid currentColor;background:transparent;color:inherit;text-align:left;font:inherit;cursor:pointer';
        var title=document.createElement('strong');title.textContent=item.label;
        var extra=document.createElement('small');extra.textContent=item.extra;
        button.appendChild(title);button.appendChild(extra);
        button.addEventListener('click',function(){selectSearch(item);});
        searchResults.appendChild(button);
      });
    }
    function selectSearch(item) {
      if(item.type==='line'){
        document.dispatchEvent(new CustomEvent('app:searchselect',{detail:{type:'line',code:item.code}}));
      }else if(item.type==='arrival'){
        navigate('favoritos');
      }else if(item.type==='stop'||item.type==='favoriteStop'){
        navigate('paradas');
      }
      header.closeAll();
    }
    document.addEventListener('app:linesloaded',function(e){
      linesLoading=false;
      knownLines=Array.isArray(e.detail.lines)?e.detail.lines:[];
      renderSearch(currentSearch.query,currentSearch.filter);
    });
    header.onSearchInput = function(query,filter){dispatchSearch(query,filter);renderSearch(query,filter);};
    header.onSearch = function(query,filter){
      dispatchSearch(query,filter);
      renderSearch(query,filter);
      var first=searchResults.querySelector('.tc-hd-result');
      if(first)first.click();
    };
    header.onAction = function (action, opened) {
      window.TuColectivoHeaderOpen = !!opened;
      if (action === 'search' && opened && !knownLines.length && !linesLoading &&
          window.TuColectivoNative && typeof window.TuColectivoNative.loadLines === 'function') {
        linesLoading=true;
        window.TuColectivoNative.loadLines();
      }
      if (action === 'notifications' && opened && window.TuColectivoNative &&
          typeof window.TuColectivoNative.getArrivalNotificationState === 'function') {
        window.TuColectivoNative.getArrivalNotificationState();
      }
    };
    header.onThemeChange = function (mode, theme) {
      var meta = document.querySelector('meta[name="theme-color"]');
      if (meta) meta.setAttribute('content', theme === 'dark' ? '#000000' : '#eef3fa');
    };
    header.onNotification = function () {};
    header.onMarkAllRead = function () {};
    var theme = header.getTheme();
    var themeMeta = document.querySelector('meta[name="theme-color"]');
    if (themeMeta) themeMeta.setAttribute('content', theme.theme === 'dark' ? '#000000' : '#eef3fa');
  }
  document.addEventListener('app:navigate', function (event) {
    syncSection(event.detail && event.detail.go);
  });
  document.addEventListener('app:status', function (event) {
    if (header && event.detail && event.detail.text) header.setStatus(event.detail.text);
  });
  /* El historial se alimenta solo con notificaciones reales recibidas del puente nativo. */
  var originalNotificationState = window.onNativeArrivalNotificationsState;
  window.onNativeArrivalNotificationsState = function (payload) {
    if (typeof originalNotificationState === 'function') originalNotificationState(payload);
    if (!header) return;
    var state = {};
    try { state = typeof payload === 'string' ? JSON.parse(payload) : (payload || {}); } catch (_) {}
    var history = Array.isArray(state.history) ? state.history : [];
    var now = Date.now();
    header.setNotifications(history.slice(0, 20).map(function (item, index) {
      var age = Math.max(0, Math.floor((now - Number(item.timestamp || now)) / 60000));
      var when = age < 1 ? 'AHORA' : age + ' MIN';
      return {
        id: String(item.id != null ? item.id : [item.timestamp || index, item.line || '', item.stop || ''].join('-')),
        title: 'LÍNEA ' + String(item.line || '') + ' · ' + String(item.message || 'ALERTA DE ARRIBO'),
        text: [item.stop, item.destination].filter(Boolean).join(' · '),
        time: when,
        unread: false
      };
    }));
  };
  window.setSystemStatusHighTech = function (text) {
    if (header) header.setStatus(String(text || 'SISTEMA LISTO'));
  };
  /* Integración de las tarjetas suministradas en el ZIP con la navegación y la sincronización existentes. */
  if (window.TcHomeCards) {
    window.TcHomeCards.onSelect = navigate;
    window.TcHomeCards.onSync = function () {
      var legacySync = document.getElementById('sync');
      if (legacySync) legacySync.click();
      else window.TcHomeCards.sync.fail();
    };
    document.addEventListener('app:navigate', function (event) {
      if (event.detail && event.detail.go === 'inicio') window.TcHomeCards.replay();
    });
  }
  if (window.TcLineas && document.body) window.TcLineas.bindPress(document.body);
  syncSection('inicio');
})();