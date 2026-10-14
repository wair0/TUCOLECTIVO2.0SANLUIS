Pega esto en ChatGPT (con el conector de GitHub) y adjunta el zip. NO pegues nada más.

---
ESTO NO ES UNA ADAPTACIÓN NI UNA REFERENCIA. ES COPIA LITERAL DE ARCHIVOS. Quiero que los archivos del zip queden en el repositorio IDÉNTICOS byte a byte.

Repositorio: wair0/TUCOLECTIVO2.0SANLUIS — rama existente `native/cyberpunkwebview-ui`. NO crees ramas ni Pull Requests: commit y push directo a esa rama.

REGLAS ABSOLUTAS
1. Cada archivo del zip REEMPLAZA POR COMPLETO al archivo del mismo path (o se crea si no existe). Prohibido: fusionar con el contenido anterior, "adaptar", mejorar, reformatear, minificar, quitar comentarios, cambiar nombres de clases/variables, reordenar reglas o dejar "lo que ya estaba".
2. Si un archivo del repo es distinto del del zip, GANA EL DEL ZIP. No intentes conservar cambios previos tuyos en esos archivos.
3. Copia el contenido exacto de cada archivo en UNA sola operación por archivo. Si un archivo te parece largo, igual cópialo entero (no lo resumas ni lo recortes). Si no puedes copiarlo completo y exacto, DETENTE y dime cuál no pudiste, en vez de reescribirlo.
4. Solo se tocan estos 10 paths (nada más):
   app/src/main/assets/header.css
   app/src/main/assets/bottom-nav.css
   app/src/main/assets/header-title.css
   app/src/main/assets/home-cards.css
   app/src/main/assets/home-cards.js
   app/src/main/assets/lineas.css
   app/src/main/assets/lineas.js
   app/src/main/assets/secciones.css   (archivo NUEVO)
   app/src/main/assets/theme-dark.css
   tools/generar_circuits.py
   NO modifiques index.html, script.js, style.css, style-ht.css, header.js, bottom-nav.js, circuits.css ni .github/. `secciones.css` se carga con @import desde lineas.css: NO agregues ningún <link> a index.html.
5. Mensaje de commit (español, MAYÚSCULAS): "APLICAR DISEÑO COMPLETO HIGH-TECH A TODAS LAS SECCIONES".

VERIFICACIÓN OBLIGATORIA (hazla después del push, leyendo cada archivo DESDE GITHUB, no desde tu copia)
- Para cada path calcula el sha256 y el tamaño en bytes y compáralos con `MANIFIESTO_SHA256.txt` del zip. Si no puedes calcular sha256, compara tamaño en bytes, número de líneas y la primera y última línea.
- Entrégame una tabla: path | bytes esperados | bytes en GitHub | OK/FALLA. Cada FALLA se rehace copiando de nuevo el archivo completo del zip y se vuelve a verificar.
- Espera a que termine el workflow "Generar circuits.css" y confirma que `app/src/main/assets/circuits.css` se regeneró (debe pesar ~280 KB y contener el texto `data-screen="paradas"`). Si el workflow falla, copia el log del error; NO escribas circuits.css a mano.

NO sigas hasta que la tabla sea toda OK. No agregues nada que yo no haya pedido.
---

Alternativa sin ChatGPT: ver "Si ChatGPT vuelve a adaptar…" en LEEME.md.
