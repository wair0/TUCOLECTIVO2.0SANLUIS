# FASE 1 — ARQUITECTURA DE CUENTAS Y SINCRONIZACIÓN

Estado: arquitectura definida; integración real pendiente de configurar el proyecto Firebase. Esta fase no activa login ni modifica los favoritos actuales.

## Alcance
- Acceso opcional: INICIAR SESIÓN, CREAR CUENTA y CONTINUAR SIN CUENTA.
- PERFIL se abrirá solo desde el menú hamburguesa, sin pestaña inferior.
- Identidad visual HIGH-TECH CORPORATIVO V3.2; evitar destellos y animaciones costosas en WebView.
- Firebase Authentication: correo/contraseña, verificación de email, restablecimiento y reautenticación para operaciones sensibles.
- Cloud Firestore: datos de perfil y favoritos por UID. Firebase Storage: foto de perfil con límites de tamaño y tipo MIME.
- La contraseña jamás se representa como dato del perfil, ni se guarda en Firestore, WebView/localStorage o logs. La pantalla mostrará una acción CAMBIAR CONTRASEÑA.

## Esquema previsto
`users/{uid}`: `firstName`, `lastName`, `email`, `photoPath`, `createdAt`, `updatedAt`, `schemaVersion`.
`users/{uid}/favorites/{favoriteId}`: `type` (line|stop|arrival), `entityId`, `lineId` si corresponde, `stopId` si corresponde, `directionId` si corresponde, `createdAt`, `updatedAt`, `deletedAt`, `schemaVersion`.
`favoriteId` será una clave determinista de tipo + identificadores normalizados para evitar duplicados; nunca se usará el texto visible como identidad. Revisar identificadores reales del almacenamiento existente antes de fijar el mapeo.
No guardar estimaciones temporales de arribo como favoritos permanentes.

## Reglas de seguridad requeridas antes de activar sincronización
- Denegar por defecto; permitir leer/escribir `users/{uid}` y subcolección `favorites` solo si `request.auth.uid == uid`.
- Validar campos permitidos, tipos, tamaños, `schemaVersion`, tipos de favoritos y límites de documento.
- Foto: ruta exclusiva por UID, usuario autenticado, imágenes permitidas y límite de bytes.
- Probar con emuladores de Firebase: usuario A no puede acceder a datos de B; invitado no accede a datos privados.
- No publicar reglas abiertas ni colocar credenciales de servicio o claves administrativas en el cliente.

## Contrato de sincronización
1. Invitado: almacenamiento local existente continúa operativo, sin conexión obligatoria.
2. Primera autenticación: leer favoritos locales y remotos, unir por ID estable y persistir sin sobrescribir ni borrar registros válidos. Mostrar estado y errores de sincronización.
3. Cambios posteriores: cola local de operaciones con ID estable, marca de modificación y borrado lógico; reintentar con conectividad. Resolver conflictos explícitamente (no sustituir toda la colección).
4. Descarga: reconstruir favoritos locales desde datos remotos compatibles, sin alterar notificaciones activas de otros perfiles.
5. Cierre de sesión: separar caché por UID, limpiar datos privados de la sesión anterior; conservar datos de invitado por separado.
6. Eliminación de cuenta: reautenticación, borrar foto y datos asociados mediante flujo confiable y después identidad; contemplar reintentos si hay fallos parciales.

## Integración Android / WebView
Preferir Firebase SDK nativo Android y un puente JS mínimo, con métodos explícitos y sin exponer tokens ni objetos de Firebase al JavaScript. Auditar configuración de WebView y orígenes permitidos antes de agregar el puente. Mantener acceso a favoritos locales mientras Firebase no esté configurado.
Revisar compatibilidad con minSdk 24, Gradle y versión del SDK al implementar; fijar versiones probadas.

## Requisitos externos pendientes
1. Crear proyecto Firebase y registrar Android `com.tucolectivo.app`.
2. Habilitar proveedor Email/Password, Firestore y Storage; decidir región y costos.
3. Proveer `google-services.json` de la app correcta y configurar CI para inyectarlo; no versionar secretos ni claves de servicio.
4. Definir política de privacidad, tratamiento de fotos/datos personales y mecanismo de solicitud de eliminación.

## Criterio de salida de fase 1
Arquitectura y contrato documentados; rama aislada; ninguna pantalla de login simulada ni sincronización declarada como funcional antes de contar con Firebase y pruebas de seguridad.
