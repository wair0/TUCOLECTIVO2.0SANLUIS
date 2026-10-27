Pega esto en ChatGPT (con el conector de GitHub) y adjunta el zip (ESTILO CORPORATIVO V3.2). NO pegues nada más.

---
ESTO NO ES UNA ADAPTACIÓN NI UNA REFERENCIA. ES COPIA LITERAL DE ARCHIVOS. Quiero que los archivos del zip queden en el repositorio IDÉNTICOS byte a byte.

Repositorio: wair0/TUCOLECTIVO2.0SANLUIS — rama existente `native/cyberpunkwebview-ui`. NO crees ramas ni Pull Requests: commit y push directo a esa rama.

REGLAS ABSOLUTAS
1. Cada archivo del zip REEMPLAZA POR COMPLETO al archivo del mismo path. Prohibido: fusionar con el contenido anterior, "adaptar", mejorar, reformatear, minificar, quitar comentarios, cambiar nombres de clases o variables, reordenar reglas, conservar "lo que ya estaba" o agregar estilos propios.
2. Si un archivo del repo es distinto del del zip, GANA EL DEL ZIP.
3. Copia el contenido exacto de cada archivo en UNA sola operación por archivo. Los archivos son pequeños (el mayor pesa ~23 KB). Si no puedes copiar alguno completo y exacto, DETENTE y dime cuál; no lo reescribas.
4. Solo se tocan estos 11 paths (nada más):
   app/src/main/assets/header.css
   app/src/main/assets/bottom-nav.css
   app/src/main/assets/header-title.css
   app/src/main/assets/home-cards.css
   app/src/main/assets/home-cards.js
   app/src/main/assets/lineas.css
   app/src/main/assets/lineas.js
   app/src/main/assets/secciones.css
   app/src/main/assets/theme-dark.css
   app/src/main/assets/circuits.css
   tools/generar_circuits.py
   NO modifiques index.html, script.js, style.css, style-ht.css, header.js, bottom-nav.js ni .github/. `secciones.css` se carga con @import desde lineas.css: NO agregues ningún <link> a index.html.
   Nota: `circuits.css` ahora es un archivo de UNA línea (143 bytes) y `tools/generar_circuits.py` solo escribe esa misma línea; es correcto, no lo "arregles".
5. Mensaje de commit (español, MAYÚSCULAS): "APLICAR ESTILO CORPORATIVO V3.2: CELESTE NEON Y BORDES ANIMADOS".

VERIFICACIÓN OBLIGATORIA (después del push, leyendo cada archivo DESDE GITHUB, no desde tu copia)
- Para cada path calcula sha256 y tamaño en bytes y compáralos con `MANIFIESTO_SHA256.txt` del zip. Si no puedes calcular sha256, compara tamaño en bytes, número de líneas y primera y última línea.
- Entrégame una tabla: path | bytes esperados | bytes en GitHub | OK/FALLA. Cada FALLA se rehace copiando de nuevo el archivo completo del zip y se vuelve a verificar.
- Si corre el workflow "Generar circuits.css", confirma que termina bien y que `circuits.css` sigue siendo el archivo de 143 bytes. Si falla, copia el log; NO escribas nada a mano.

NO sigas hasta que la tabla sea toda OK. No agregues nada que yo no haya pedido.
---
