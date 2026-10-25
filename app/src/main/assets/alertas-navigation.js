
/* Fase 2: acceso contextual a Favoritos desde la nueva sección Alertas. */
document.addEventListener('click',function(e){var b=e.target.closest('.tc-alerts-link[data-section]');if(b&&window.TuColectivo&&typeof window.TuColectivo.navigate==='function'){window.TuColectivo.navigate(b.dataset.section);window.TcBottomNav?.setActive(b.dataset.section);window.TcHeader?.setSection(b.dataset.section);}});
