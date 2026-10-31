# Mi Tablaturas

App nativa de Android para abrir, renderizar y reproducir tablaturas de guitarra.

- **Interactivas (Guitar Pro):** `.gp3`, `.gp4`, `.gp5`, `.gpx`, `.gp`, renderizadas con [AlphaTab](https://alphatab.net) en un `WebView` local, con audio vía Web Audio API y un SoundFont `.sf2`.
- **Estáticas:** `.pdf`, con `android.graphics.pdf.PdfRenderer`.

> Estado: esqueleto inicial. El primer objetivo es abrir y reproducir un `.gp5`.

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
core/data/              Repositorios, acceso a archivos, dispatchers (Hilt)
feature/file-picker/    Selección de archivos con SAF
feature/tab-viewer/     Visor AlphaTab (WebView + puente JS)
```

`feature/pdf-viewer` se añadirá cuando llegue el visor de PDF.

## Assets de AlphaTab

AlphaTab y el SoundFont se empaquetan en el APK para que todo funcione sin conexión:

```
feature/tab-viewer/src/main/assets/alphatab/
├── alphaTab.min.js        Bundle de @coderline/alphatab
├── font/                  Fuentes Bravura que usa AlphaTab
├── soundfont/sonivox.sf2  SoundFont para el sintetizador
└── viewer.html            Página que carga AlphaTab y expone el puente
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

- AlphaTab: MPL-2.0
- SoundFont: se documentará su licencia al añadirlo.
