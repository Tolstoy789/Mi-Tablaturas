# Mi Tablaturas

App nativa de Android para abrir, renderizar y reproducir tablaturas de guitarra.

- **Interactivas (Guitar Pro):** `.gp3`, `.gp4`, `.gp5`, `.gpx`, `.gp`, renderizadas con [AlphaTab](https://alphatab.net) en un `WebView` local, con audio vía Web Audio API y un SoundFont `.sf2`.
- **Estáticas:** `.pdf`, con `android.graphics.pdf.PdfRenderer`.

> Estado: abre archivos con el selector de Android y reproduce tablaturas Guitar Pro con AlphaTab. El visor de PDF está pendiente.

## Stack

| Área | Tecnología |
| --- | --- |
| Lenguaje y UI | Kotlin 2.2, Jetpack Compose (Material 3) |
| Arquitectura | Clean Architecture + MVVM, ViewModel, Coroutines, StateFlow |
| DI | Hilt |
| Tablaturas | AlphaTab en `WebView`, render SVG, puente `@JavascriptInterface` / `evaluateJavascript` |
| PDF | `PdfRenderer` + `LazyColumn` |
| Archivos | Storage Access Framework (`ActivityResultContracts.OpenDocument`) |
| Build | Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`) |

## Estructura de módulos

```
app/                    Punto de entrada, tema, navegación
core/model/             Modelos puros de Kotlin (TabFormat, TabDocument)
core/data/              Repositorios, acceso a archivos, preferencias (DataStore), Hilt
core/designsystem/      Temas Atril, Escenario y Taller (Material 3)
feature/file-picker/    Selección de archivos con SAF
feature/tab-viewer/     Visor AlphaTab (WebView + puente JS)
```

`feature/pdf-viewer` se añadirá cuando llegue el visor de PDF.

## Diseños

El usuario elige el diseño desde el botón **Diseño** de la pantalla de inicio; la elección se guarda con DataStore.

| Diseño | Estilo |
| --- | --- |
| Atril | Claro, papel y tinta con acento cobalto |
| Escenario | Oscuro, alto contraste con acento ámbar |
| Taller (por defecto) | Violeta expresivo; sigue el modo oscuro del sistema |

Los colores de la app están en `core/designsystem/.../Theme.kt` y los de la partitura de AlphaTab en `THEMES` de `assets/alphatab/bridge.js`; si cambias unos, actualiza los otros.

## Assets de AlphaTab

AlphaTab y el SoundFont van empaquetados en el APK para que todo funcione sin conexión:

```
feature/tab-viewer/src/main/assets/alphatab/
├── alphaTab.min.js        @coderline/alphatab 1.8.4 (MPL-2.0)
├── bridge.js              Puente JS <-> Kotlin (window.tab / window.AndroidBridge)
├── viewer.html            Página que carga AlphaTab
├── font/Bravura.woff2     Fuente musical (SIL OFL 1.1)
└── soundfont/sonivox.sf2  SoundFont del sintetizador (Apache 2.0)
```

El WebView sirve estos archivos con `WebViewAssetLoader` en `https://appassets.androidplatform.net/assets/alphatab/`, y la partitura abierta en `/score/current`, sin habilitar acceso a `file://`.

Para actualizar AlphaTab:

```bash
npm pack @coderline/alphatab@<versión>
tar xzf coderline-alphatab-<versión>.tgz
cp package/dist/alphaTab.min.js feature/tab-viewer/src/main/assets/alphatab/
cp package/dist/font/Bravura.woff* feature/tab-viewer/src/main/assets/alphatab/font/
cp package/dist/soundfont/sonivox.sf2 feature/tab-viewer/src/main/assets/alphatab/soundfont/
```

## Requisitos

- Android Studio Narwhal (2025.1) o superior
- JDK 17
- Android SDK 36 (compileSdk/targetSdk), dispositivo o emulador con Android 8.0 (API 26) o superior

## Compilar

```bash
git clone https://github.com/Tolstoy789/Mi-Tablaturas.git
cd Mi-Tablaturas
./gradlew test               # tests unitarios
./gradlew :app:assembleDebug # APK en app/build/outputs/apk/debug/
```

También puedes abrir la carpeta en Android Studio y ejecutar la configuración `app`.

Cada push y pull request ejecuta los tests y compila el APK debug en GitHub Actions (`.github/workflows/android.yml`). El APK queda disponible como artefacto `app-debug` del workflow.

## Convenciones

- Commits con [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/): `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`, `ci:`.
- Una rama por cambio y pull request hacia `main`.

## Licencias de terceros

- AlphaTab: MPL-2.0 (`assets/alphatab/LICENSE-alphaTab.txt`)
- Bravura: SIL Open Font License 1.1 (`assets/alphatab/font/Bravura-OFL.txt`)
- Sonivox SoundFont: Apache 2.0 (`assets/alphatab/soundfont/LICENSE-sonivox.txt`)
