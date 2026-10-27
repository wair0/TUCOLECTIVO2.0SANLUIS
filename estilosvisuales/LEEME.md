# TU COLECTIVO 2.0 — ESTILO CORPORATIVO · VERSIÓN 3.2

Incluye todo lo anterior (iconos de línea fina, título animado por letras, mayúsculas, oro neón en oscuro, medidor verde neón sin onda, etiquetas LÍNEA/HACIA rojo neón, efectos de pulsación y transición) más:

1. **Textos del modo claro en celeste neón**: texto principal y acento `#0072A3` (celeste profundo) con **resplandor celeste neón** (`rgba(0,200,255,.38)`) en títulos y nombres; texto secundario `#2F6C86`; fondo con un leve tinte celeste. Se eligió el celeste más vivo que mantiene contraste ≥ 4,5:1 sobre blanco (un celeste #00D4FF puro sería ilegible sobre fondo claro; el neón se logra con el resplandor y los bordes). Los botones rellenos pasan a celeste profundo con texto blanco.
2. **Bordes celeste neón en todas las tarjetas** (inicio, sincronizar, líneas, calles/intersecciones/paradas, arribos, parada, mapa, ventana de parada, paradas cercanas, favoritos, estados vacíos, paneles del menú): borde de 1 px celeste neón (`#00C4FF` en claro, `#00E5FF` en oscuro) con halo suave; al pulsar o seleccionar el borde y el halo se intensifican. La tarjeta ARRIBANDO conserva su borde rojo. Aplica en claro y oscuro.
3. **Bordes del header y de la barra inferior animados**: línea celeste neón en el borde inferior del header y superior de la barra, con **dos haces de luz que la recorren** en sentidos opuestos y un halo que respira (3,4 s). Se desactiva con "reducir movimiento".

`docs/preview_corporativo_v32.html` es tu app real con datos de ejemplo (arranca en modo claro; ☰ → Apariencia cambia el tema). `docs/bordes_animados.png` muestra 3 instantes del borde animado, en oscuro (arriba) y claro (abajo).

## Archivos (reemplazar cada uno COMPLETO, sin editar) — ver MANIFIESTO_SHA256.txt
`header.css`, `bottom-nav.css`, `header-title.css`, `home-cards.css`, `home-cards.js`, `lineas.css` (+ `secciones.css` por @import), `lineas.js`, `theme-dark.css` (paleta y bordes neón), `circuits.css` (vacío a propósito) y `tools/generar_circuits.py`.
Respecto a la V3.1 cambian 3: `theme-dark.css`, `header.css` y `bottom-nav.css`; aplicar los 11 también es correcto. No hay que tocar `index.html`, `script.js`, `style*.css`, `header.js`, `bottom-nav.js` ni `.github/`.

## Si ChatGPT vuelve a "adaptar" en lugar de copiar — hazlo tú (navegador del teléfono)
Repo en github.com, rama `native/cyberpunkwebview-ui`, carpeta `app/src/main/assets`: por cada archivo → lápiz ✏️ → borra todo → pega el contenido COMPLETO del archivo del zip → "Commit changes". Igual con `tools/generar_circuits.py`.
