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
    header.onSearchInput = dispatchSearch;
    header.onSearch = dispatchSearch;
    header.onAction = function (action, opened) {
      window.TuColectivoHeaderOpen = !!opened;
      if (action === 'notifications' && opened && window.TuColectivoNative &&
          typeof window.TuColectivoNative.getArrivalNotificationState === 'function') {
        window.TuColectivoNative.getArrivalNotificationState();
      }
    };
    header.onThemeChange = function (mode, theme) {
      var meta = document.querySelector('meta[name="theme-color"]');
      if (meta) meta.setAttribute('content', theme === 'dark' ? '#080e1f' : '#eef3fa');
    };
    header.onNotification = function () {};
    header.onMarkAllRead = function () {};
    var theme = header.getTheme();
    var themeMeta = document.querySelector('meta[name="theme-color"]');
    if (themeMeta) themeMeta.setAttribute('content', theme.theme === 'dark' ? '#080e1f' : '#eef3fa');
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
  syncSection('inicio');
})();