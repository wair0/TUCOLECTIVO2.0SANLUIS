# FASE 5 — SEGURIDAD Y GESTIÓN DE CUENTA

Estado: acciones y bloqueo seguro preparados. No hay proveedor Firebase configurado; ninguna acción privada se presenta como funcional.

## Reglas de implementación obligatorias
- La sesión debe obtenerse del SDK nativo de Firebase Authentication. No aceptar UID, email ni tokens suministrados por JavaScript como prueba de identidad.
- Actualizar nombre/apellido: validar longitud y caracteres; persistir bajo `users/{auth.uid}`; mostrar resultado solo después de confirmación del servidor.
- Foto: selector Android, URI temporal, verificar MIME de imagen y tamaño; subir a ruta restringida por UID, guardar referencia y limpiar foto anterior cuando corresponda.
- Contraseña: reautenticar al usuario según Firebase Auth y llamar a la operación nativa de cambio de contraseña. Nunca guardar ni registrar la contraseña, ni enviarla a Firestore, ni retornarla a WebView.
- Cerrar sesión: detener sincronización y tareas pendientes; separar la caché de UID, limpiar los datos privados de la vista; preservar favoritos de invitado independientemente.
- Eliminar cuenta: confirmación explícita y reautenticación reciente; ejecutar eliminación verificable de favoritos, perfil y foto mediante backend confiable (idempotente y recuperable), después eliminar identidad Auth. No indicar éxito si falla cualquier etapa.
- No habilitar botones de edición, contraseña, foto, cierre de sesión o eliminación solo por datos locales; requieren sesión nativa real.
- Mantener acceso sin cuenta, sin borrado ni migración automática de los favoritos existentes.

## Pruebas de aceptación antes de habilitar acciones
1. Invitado: todos los controles privados deshabilitados; CONTINUAR SIN CUENTA funciona.
2. Usuario A: no puede consultar ni editar datos de B, incluso manipulando argumentos del puente JS.
3. Cambio de contraseña: exige reautenticación; nunca se muestra el secreto en perfil ni logs.
4. Cierre de sesión: no filtra datos privados en pantalla, favoritos, notificaciones o cola de sincronización.
5. Eliminación: requiere confirmación, limpia subcolecciones y foto, maneja reintentos, no deja identidad sin seguimiento de limpieza.
6. Sin conexión: no comunica éxito de operaciones remotas que no fueron confirmadas.
7. Revisar reglas Firestore/Storage con emuladores y configuración de Firebase antes de publicar.

## Dependencias bloqueantes
Proyecto Firebase de `com.tucolectivo.app`, proveedor Email/Password, Firestore, Storage, configuración Android y mecanismo confiable de limpieza. No insertar cuentas de servicio en el APK ni en el repositorio.
