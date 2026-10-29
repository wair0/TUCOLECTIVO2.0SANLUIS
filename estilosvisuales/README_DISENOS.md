# Nuevos diseños High-Tech Bus (Login · Perfil · Menú hamburguesa)
Copiar `app/src/main/assets/*` sobre el proyecto (login.css y profile.css reemplazan a los actuales). login.js, profile.js y header.js NO cambian: se conservaron todos los IDs.

Cambios en `index.html`:
1. `<head>`: agregar `<link rel="stylesheet" href="header-menu.css">` después de header.css/header-hitech.css (login.css y profile.css ya están enlazados).
2. Reemplazar `<section id="tc-auth">…</section>` por el contenido de `login.html`.
3. Reemplazar `<section id="tc-profile">…</section>` por el contenido de `profile.html`.
4. Reemplazar el bloque `<div class="tc-hd-panel" data-for="menu">…</div>` por `header-menu.html`.
5. Agregar `<script src="profile-ui.js"></script>` justo ANTES de `<script src="profile.js"></script>`.
Los archivos *.html son fragmentos para pegar, no páginas completas.
