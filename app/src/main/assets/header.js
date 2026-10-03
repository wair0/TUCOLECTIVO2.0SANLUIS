/* TU COLECTIVO 2.0 · HOLO-DECK
   Efectos visuales seguros: no desplaza ni inclina el header.
   La navegación y el estado siguen a cargo de script.js. */
(() => {
  'use strict';
  const root = document.getElementById('tch');
  if (!root) return;

  const title = root.querySelector('.tch-title');
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Mantener el header anclado a la pantalla: no usar giroscopio ni inclinación por puntero.
  const rig = root.querySelector('.tch-rig');
  if (rig) {
    rig.style.setProperty('--rx', '0deg');
    rig.style.setProperty('--ry', '0deg');
    rig.style.transform = 'translateZ(0)';
  }

  // Feedback táctil discreto, sin interferir con las acciones existentes.
  document.addEventListener('pointerdown', event => {
    const button = event.target.closest('.tch-btn,.tch-item,.tch-panel .chip,.tch-x');
    if (!button) return;
    if (navigator.vibrate) {
      try { navigator.vibrate(8); } catch (_) {}
    }
  }, { passive: true });

  // Sincronizar el indicador de sección con la navegación de script.js.
  document.addEventListener('app:navigate', event => {
    const destination = event.detail && event.detail.go;
    document.querySelectorAll('.tch-item').forEach(item => {
      item.setAttribute('aria-current', String(item.dataset.go === destination));
    });
  });

  // El título permanece visible durante el arranque; solo aplica un glitch breve opcional.
  if (title && !reduceMotion) {
    const glitch = () => {
      title.classList.add('glitch');
      window.setTimeout(() => title.classList.remove('glitch'), 420);
      window.setTimeout(glitch, 7000);
    };
    window.setTimeout(glitch, 5000);
  }
})();
