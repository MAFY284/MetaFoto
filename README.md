# MetaFoto

Visor y analizador de metadatos de fotos para Android, centrado en la privacidad. Todo el análisis se realiza en el dispositivo: sin cuentas, anuncios, telemetría ni servidores propios.

## Descargar APK

[**MetaFoto-1.0.0.apk**](https://github.com/MAFY284/MetaFoto/releases/latest) — APK firmado para Android 8.0+.

## Funciones

- Lectura completa de **EXIF, GPS, IPTC, XMP, ICC** y otras etiquetas disponibles.
- **Mapa interactivo** OpenStreetMap cuando la foto incluye coordenadas.
- Soporte de imágenes **JPG, HEIC, PNG, TIFF y WebP**.
- Entrada desde **galería, archivos o cámara** del sistema.
- Interfaz Jetpack Compose con temas **claro, oscuro y sistema**.
- Solo se pide acceso a la foto seleccionada; también se puede leer la ubicación incluida en la imagen con el permiso de Android correspondiente.
- Sin publicidad, compras ni rastreo.

## Compilar

Requisitos: JDK 17, Android SDK 35.

```bash
export JAVA_HOME=/ruta/al/jdk-17
export ANDROID_HOME=/ruta/al/Android/Sdk
./gradlew :app:assembleDebug
```

Para una compilación release firmada, crea `keystore.properties` en la raíz:

```properties
storeFile=metafoto-release.jks
storePassword=...
keyAlias=metafoto
keyPassword=...
```

Y ejecuta:

```bash
./gradlew :app:assembleRelease
```

## Privacidad

No hay servidores propios ni telemetría. El mapa descarga teselas de OpenStreetMap únicamente cuando hay conexión y el usuario abre la ubicación de una foto.
