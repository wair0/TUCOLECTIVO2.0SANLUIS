# TU COLECTIVO 2.0 — paquete completo (todas las secciones)

Este zip es la versión FINAL y COMPLETA del diseño High-Tech Corporativo. Probado cargando el `index.html` real de tu repositorio con un puente Android simulado,
recorriendo INICIO, LÍNEAS (calles, intersecciones, paradas, arribos), MAPA, PARADAS CERCANAS (+ arribos), FAVORITOS, en claro y oscuro, sin errores de JavaScript.
`docs/preview_diseno_completo.html` es esa misma app (abrir en el navegador del teléfono). `docs/*.png` son capturas.

## Qué se corrige / agrega
1. **Tarjetas de líneas y subsecciones**: reglas viejas de `style.css`/`style-ht.css` las pisaban (fondo gris, radio, barrita de 4 px, textos de 10 px). Ahora van blindadas con `html body [data-screen="lineas"]…`.
   El hexágono lleva el icono de ruta y el texto es LÍNEA + nombre público (A, BCG, 110). El código interno de SmartMove nunca se muestra (solo `data-line`).
2. **Header y barra inferior**: se restauran las esquinas cortadas y la opacidad de las etiquetas (guardas con `!important` en `theme-dark.css`, por si otro archivo las quitó).
   **El header queda ESTÁTICO** (sin pulsos de luz); solo la barra inferior y las tarjetas de arribos tienen animación. El título conserva su revelado y destello.
3. **Textos muy oscuros**: paleta nueva con contraste medido (todos los textos ≥ 4,5:1 en claro y oscuro). Oscuro: fondo #000, texto principal oro neón #FFD60A, secundario oro claro #E3D58F, acento cian, minutos verde neón.
   Claro: texto #08122B, secundario #2B4170, acento azul. Las variables viejas de `style.css` también pasan a negro en modo oscuro.
4. **Estilo aplicado a las demás secciones** (`secciones.css`): MAPA (marco con esquinas cortadas, controles, tarjeta ACTUALIZAR RECORRIDO, ventana de parada), PARADAS CERCANAS (botón LOCALIZAR, tarjetas de parada, chips de línea, estados vacíos),
   ARRIBOS de paradas cercanas (tarjetas con pistas animadas y minutos en verde neón), FAVORITOS (horario de alertas, campos, botón, grupos, tarjetas con botón quitar). Circuitos conectados a los textos en todas.
5. **Separación header–contenido** de 22 px (`--hh:84px` en `header.css`).
6. **Error de `script.js` encontrado**: en los arribos de PARADAS CERCANAS el código usa `esc(destination)` y `destinations` fuera de su alcance (ReferenceError: la pantalla quedaba en "CONSULTANDO ARRIBOS_" o la ventana del mapa no se habilitaba).
   `lineas.js` trae un parche seguro (variables globales de respaldo) que lo evita sin tocar `script.js`. Corrección definitiva opcional: cambiar `esc(destination)` por `escapeHtml(rawDestination)` y definir `destinations` en ese bloque.
7. Los títulos exteriores de cada sección (LÍNEAS / 14 RUTAS, etc.) quedan ocultos, como decidiste en la "limpieza de títulos" de `style.css`. Solo se ven las cabeceras internas de las subsecciones de LÍNEAS.

## Archivos (reemplazar cada uno COMPLETO, sin editar)
Ver `MANIFIESTO_SHA256.txt` (ruta, tamaño en bytes, líneas, primera y última línea, sha256).
- `app/src/main/assets/`: `header.css`, `bottom-nav.css`, `header-title.css`, `home-cards.css`, `home-cards.js`, `lineas.css`, `lineas.js`, `secciones.css` (NUEVO), `theme-dark.css`
- `tools/generar_circuits.py` (el workflow `Generar circuits.css` lo ejecuta solo y regenera `app/src/main/assets/circuits.css`)
No hay que tocar `index.html`, `script.js`, `style.css`, `style-ht.css`, `header.js`, `bottom-nav.js`, ni `.github/`. `secciones.css` se carga con `@import` desde `lineas.css` (misma carpeta).
`opcional_manual/circuits.css` es el resultado del generador, SOLO para subirlo a mano si el workflow falla (pesa ~280 KB: no se lo pidas a ChatGPT).

## Si ChatGPT vuelve a "adaptar" en lugar de copiar — hazlo tú en 5 minutos (navegador del teléfono)
1. Abre el repo en github.com, rama `native/cyberpunkwebview-ui`, carpeta `app/src/main/assets`.
2. Para cada archivo: ábrelo → lápiz ✏️ (Edit) → selecciona todo y borra → pega el contenido COMPLETO del archivo del zip → "Commit changes".
   Para `secciones.css`: "Add file → Create new file" con ese nombre. Para `tools/generar_circuits.py`: abrir y reemplazar igual.
3. Espera a que termine la Action "Generar circuits.css" (pestaña Actions). Si falla, sube `opcional_manual/circuits.css` a `app/src/main/assets/`.
