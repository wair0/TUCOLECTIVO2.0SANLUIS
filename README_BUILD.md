# TRANSPUNTANO 2.0 — Instrucciones de compilación del APK

## Requisitos (en tu PC o CI)

- Android Studio (recomendado) **o**
- JDK 17+
- Android SDK (API 35)
- Node no es necesario (proyecto nativo puro)

## 1. Abrir el proyecto

```bash
# Descomprime el ZIP
unzip Transpuntano20_Complete.zip
cd transpuntano20
```

Abre la carpeta en **Android Studio** → “Open”.

Espera a que sincronice Gradle (primera vez puede descargar dependencias).

## 2. Compilar APK Release firmado (recomendado)

### Opción A — Android Studio (más fácil)

1. Menú **Build → Generate Signed Bundle / APK**
2. Elige **APK**
3. Selecciona el keystore incluido:
   - Ruta: `keystore/transpuntano20-release.jks`
   - Password: `android`
   - Alias: `transpuntano20`
   - Key password: `android`
4. Elige **release**
5. Finish

El APK firmado queda en:
```
app/build/outputs/apk/release/app-release.apk
```

### Opción B — Línea de comandos

```bash
# Windows / Linux / Mac
./gradlew assembleRelease

# El APK unsigned estará en:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

Luego firma manualmente (si no usaste el signingConfig):

```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore keystore/transpuntano20-release.jks \
  -storepass android \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  transpuntano20

# Verificar
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release-unsigned.apk
```

(Opcional, si tienes Android SDK build-tools):
```bash
zipalign -v 4 app-release-unsigned.apk Transpuntano20.apk
apksigner sign --ks keystore/transpuntano20-release.jks --ks-pass pass:android Transpuntano20.apk
```

## 3. Keystore incluido

| Campo              | Valor                          |
|--------------------|--------------------------------|
| Archivo            | `keystore/transpuntano20-release.jks` |
| Alias              | `transpuntano20`               |
| Store password     | `android`                      |
| Key password       | `android`                      |
| Validez            | 10000 días                     |

**Importante para producción:** cambia las contraseñas y genera un keystore nuevo propio. No uses estas credenciales en Google Play.

## 4. Configuración de firma automática (opcional)

Puedes añadir en `app/build.gradle.kts` (dentro de `android { }`):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../keystore/transpuntano20-release.jks")
        storePassword = "android"
        keyAlias = "transpuntano20"
        keyPassword = "android"
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false
    }
}
```

Luego solo ejecuta `./gradlew assembleRelease` y obtienes el APK firmado.

## 5. Características del APK

- Nombre: **TRANSPUNTANO 2.0**
- Package: `com.transpuntano.transpuntano20`
- Version: 2.0.0
- minSdk 24 / targetSdk 35
- WebView con CSS cyberpunk inyectado
- Bottom Navigation: Inicio · Líneas · Paradas · Favoritos · Cercanas
- Offline + Error + Reintentar
- Back nativo Android
- HTTPS only
- Jerarquía real de la fuente oficial (Línea → Calle → Intersección → Parada → Arribos)

## 6. ¿Por qué no entrego el .apk ya compilado?

El entorno de generación tiene memoria limitada (~1.2 GB). Gradle + Android Gradle Plugin requieren más RAM para compilar de forma fiable. Por eso entrego:

1. Proyecto fuente completo
2. Keystore listo
3. Instrucciones exactas

Cualquier PC con Android Studio genera el APK en menos de 3 minutos.

## Contacto / problemas

Si al abrir el proyecto aparece algún error de SDK, instala en Android Studio:
- Android SDK Platform 35
- Android SDK Build-Tools 35

¡Listo! Instala el APK en tu teléfono y disfruta de TRANSPUNTANO 2.0.


## GitHub Actions
Este proyecto incluye `.github/workflows/build-apk.yml`. Al hacer push a `main` o ejecutar
`Actions -> Build Android APK -> Run workflow`, GitHub compilará un APK debug y lo dejará
disponible como Artifact para descargar.

Por seguridad, el paquete preparado para GitHub no incluye `local.properties` ni el keystore
de desarrollo que venía en el ZIP original. Para publicar una versión release firmada, usa
un keystore propio almacenado como GitHub Actions Secret y configura `signingConfigs` sin
contraseñas dentro del código.
