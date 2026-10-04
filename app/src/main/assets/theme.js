/* TU COLECTIVO 2.0 — NEXUS: modo claro / oscuro (sin dependencias, 100% offline) */
(function () {
  var KEY = 'tc-theme', root = document.documentElement;
  function saved() { try { return localStorage.getItem(KEY); } catch (e) { return null; } }
  function apply(t) {
    root.setAttribute('data-theme', t);
    var m = document.querySelector('meta[name="theme-color"]');
    if (m) m.setAttribute('content', t === 'dark' ? '#080e1f' : '#eef3fa');
    var b = document.getElementById('themeToggle');
    if (b) {
      b.setAttribute('aria-checked', String(t === 'dark'));
      var s = b.querySelector('small'); if (s) s.textContent = t === 'dark' ? 'ACTIVADO' : 'DESACTIVADO';
    }
  }
  apply(saved() === 'dark' ? 'dark' : 'light');
  document.addEventListener('DOMContentLoaded', function () {
    apply(root.getAttribute('data-theme'));
    var b = document.getElementById('themeToggle');
    if (!b) return;
    b.addEventListener('click', function () {
      var t = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      try { localStorage.setItem(KEY, t); } catch (e) {}
      apply(t);
    });
  });
})();
