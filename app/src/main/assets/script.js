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
