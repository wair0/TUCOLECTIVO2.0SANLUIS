# TU COLECTIVO 2.0 — ESTILO CORPORATIVO (alternativa sin aspecto de videojuego)

Segundo estilo completo para toda la app. Diseño sobrio tipo aplicación empresarial / banca móvil: superficies limpias, un solo color de acento (azul), tipografía del sistema
(Roboto en Android, sin fuentes externas), tarjetas redondeadas con borde fino y sombra suave, texto en formato oración. **Sin** neones, brillos, hexágonos, esquinas cortadas, circuitos, radares ni marcos tipo HUD.
`docs/preview_estilo_corporativo.html` es tu app real con este estilo y datos de ejemplo (abrir en el navegador del teléfono; el menú ☰ → Apariencia cambia claro/oscuro).

## Cómo se ve (resumen)
- **Header**: barra blanca/azul-grisácea con "TU COLECTIVO" + etiqueta "2.0", botones de icono redondeados y una píldora con punto verde ("Sección: líneas"). **Estático**.
- **Barra inferior**: pestañas estándar con icono + etiqueta; la activa lleva una píldora de acento y una línea fina arriba que se desliza.
- **Inicio**: 4 tarjetas con icono en recuadro redondeado y flecha; SINCRONIZAR LÍNEAS con barra de progreso lisa de 0 a 100 % (verde al terminar).
- **Líneas**: tarjetas con icono, "Línea" y el nombre público en grande (A, BCG, 110); calles, intersecciones y paradas como lista limpia; arribos con anillo fino de minutos (arco de acento que gira suave, número grande tabular y "min").
  ARRIBANDO es una píldora roja suave con punto que late. El círculo desaparece en ARRIBANDO, igual que antes.
- **Mapa, Paradas cercanas, Arribos, Favoritos**: mismas tarjetas, botones (primario azul / secundario con borde), campos de formulario redondeados y estados vacíos con borde punteado.
- **Colores**: claro = fondo #F3F5F9, tarjetas blancas, texto #0F172A, acento #2563EB. Oscuro = fondo #0B1018 (grafito azulado, no negro puro), tarjetas #121926, texto #EAF0FA, acento #709BFF.
  Contraste medido en las 7 pantallas y ambos modos: todos los textos ≥ 4,5:1.

## Archivos (reemplazar cada uno COMPLETO, sin editar)
Ver `MANIFIESTO_SHA256.txt`. Son los mismos nombres de siempre, así que reemplazan al estilo anterior sin tocar `index.html`, `script.js`, `style*.css`, `header.js`, `bottom-nav.js` ni `.github/`:
`header.css`, `bottom-nav.css`, `header-title.css`, `home-cards.css`, `home-cards.js`, `lineas.css` (+ `secciones.css` por @import), `lineas.js`, `theme-dark.css` (la paleta), `circuits.css` (ahora vacío) y `tools/generar_circuits.py` (ahora solo escribe ese archivo vacío; el workflow "Generar circuits.css" sigue funcionando).
Se conserva TODA la lógica: clases y variables que usan header.js, bottom-nav.js, home-cards.js y script.js (.open, .active, .hot, .run, --i, --t, --p…), el parche de `esc`/`destination`/`destinations` de arribos y la regla "no mostrar códigos internos" (la línea muestra su nombre público).
Los títulos exteriores de cada sección siguen ocultos (tu "limpieza de títulos" de style.css).

## Volver al estilo anterior
Aplica de nuevo los archivos de `TC_DISENO_COMPLETO_V2.zip` (mismos nombres). No hace falta borrar nada.

## Si ChatGPT vuelve a "adaptar" en lugar de copiar — hazlo tú (navegador del teléfono)
1. Repo en github.com, rama `native/cyberpunkwebview-ui`, carpeta `app/src/main/assets`.
2. Por cada archivo: ábrelo → lápiz ✏️ → borra todo → pega el contenido COMPLETO del archivo del zip → "Commit changes". `secciones.css` ya existe en tu repo: se reemplaza igual.
3. Haz lo mismo con `tools/generar_circuits.py`. Los archivos son pequeños (el más grande, `secciones.css`, pesa ~23 KB).
