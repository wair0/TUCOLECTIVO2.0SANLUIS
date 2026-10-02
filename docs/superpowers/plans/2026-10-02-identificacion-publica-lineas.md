# IDENTIFICACIÓN PÚBLICA DE LÍNEAS — IMPLEMENTATION PLAN

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mantener el código numérico de SmartMove para consultas internas, pero mostrar siempre la identificación pública por letra/nombre en TU COLECTIVO 2.0.

**Architecture:** El backend conserva `TransitLine.code` como identificador técnico. `MainActivity` serializa el nombre público desde `TransitLine.name`; JavaScript mantiene un catálogo global `codigo → nombre público` para que MAPA, LÍNEAS, ARRIBOS, PARADAS CERCANAS y FAVORITOS rendericen la misma identificación.

**Tech Stack:** Kotlin, Android WebView bridge, JavaScript, SmartMove SOAP.

**Spec:** Documento Maestro de Continuidad; regla de identificación pública de líneas.

## Global Constraints

- No modificar accidentalmente `main`.
- No reemplazar los códigos técnicos usados por SmartMove en las consultas.
- No mostrar códigos internos como identificación pública.
- Mantener MAPA y PARADAS CERCANAS desacoplados.
- No cambiar el mapa base ni el flujo GPS en esta corrección.
- Commit consolidado en español y MAYÚSCULAS.

## Review Focus

- Catálogo de LÍNEAS: una línea como código 360 debe renderizar su nombre público, no 360.
- MAPA: estado de recorrido y vehículos deben usar el nombre público.
- ARRIBOS: la tarjeta debe conservar la identificación pública.
- PARADAS CERCANAS: badges y selección de línea deben usar el catálogo.
- FAVORITOS: no volver a mostrar el código numérico.

### Task 1: Catálogo público nativo

**Files:** Modify `app/src/main/java/com/transpuntano/app/MainActivity.kt`.

- [ ] Serializar `name` mediante `publicLineLabel(code, items)` y conservar `code` intacto.
- [ ] Mantener `raw` disponible para diagnóstico sin usarlo como texto visible.

### Task 2: Catálogo público WebView

**Files:** Modify `app/src/main/assets/script.js`.

- [ ] Crear un resolvedor global `TuColectivoPublicLineLabel`.
- [ ] Poblar `TuColectivoLineLabels` al recibir el catálogo de líneas.
- [ ] Normalizar la representación visible de LÍNEAS, MAPA y PARADAS CERCANAS.
- [ ] Usar el resolvedor en FAVORITOS y estados del MAPA.

### Task 3: Verificación

- [ ] Comprobar que no se cambiaron los códigos usados por `loadStreets`, `loadStops`, `loadArrivals` o `showRoute`.
- [ ] Validar sintaxis JavaScript.
- [ ] Comparar el diff final y confirmar que sólo afecta la presentación de identificación pública.
- [ ] Lanzar el commit consolidado para GitHub Actions.
