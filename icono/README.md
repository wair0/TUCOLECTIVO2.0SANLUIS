# TU COLECTIVO 2.0 - Icono de la app (High-Tech Corporativo)

Icono adaptativo de Android: el fondo ocupa TODA la superficie (sin margenes), por lo que se ve bien en
circulo, cuadrado, squircle, gota u otra forma que aplique el launcher.

## Contenido
- `app/src/main/res/mipmap-*/`: `ic_launcher_background`, `ic_launcher_foreground`, `ic_launcher_monochrome` (iconos tematicos Android 13+), `ic_launcher` y `ic_launcher_round` (Android < 8).
- `app/src/main/res/mipmap-anydpi-v26/`: XML del icono adaptativo.
- `store/ic_launcher-playstore-512.png`: icono para Google Play (cuadrado, sin mascara).
- `source/`: capas maestras en 1024 px (fondo, primer plano, monocromo) e imagen compuesta.

## Uso
1. Copia `app/src/main/res/` a tu proyecto (reemplaza los `mipmap-*` anteriores).
2. En `AndroidManifest.xml`:
   ```xml
   <application
       android:icon="@mipmap/ic_launcher"
       android:roundIcon="@mipmap/ic_launcher_round" ...>
   ```
