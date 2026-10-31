# Mi Tablaturas

App nativa de Android para abrir, renderizar y reproducir tablaturas de guitarra.

- **Interactivas (Guitar Pro):** `.gp3`, `.gp4`, `.gp5`, `.gpx`, `.gp`, renderizadas con [AlphaTab](https://alphatab.net) en un `WebView` local, con audio vía Web Audio API y un SoundFont `.sf3` (comprimido con Vorbis).
- **Estáticas:** `.pdf`, con `android.graphics.pdf.PdfRenderer`: páginas a todo el ancho, dibujadas al aparecer en pantalla, y paso de página con pedal.

> Estado: abre archivos con el selector de Android o desde otras apps (Ver y Compartir), reproduce tablaturas Guitar Pro con AlphaTab y muestra PDF. La app está en español e inglés, según el idioma del sistema.

La pantalla de inicio guarda una **biblioteca** con las tablaturas abiertas (Room): filtros Todas, Guitar Pro, PDF y Favoritas, y por cada canción la pista, la velocidad y la posición en que se dejó. Los archivos compartidos con permiso temporal (WhatsApp, correo) se copian en el almacenamiento privado de la app para que sigan abriéndose.

Herramientas de práctica en el visor:

- **Tocar la partitura** mueve el cursor a ese compás.
- **Bucle A-B por compases:** coloca el cursor, pulsa «Marcar A», toca el último compás y pulsa «Marcar B». Tocar la partitura no lo quita; se quita con el mismo botón.
- **Cuenta previa**, **metrónomo** y **velocidad** del 25 % al 150 % en pasos del 5 %.
- **+5 % por vuelta:** sube la velocidad al terminar cada repetición hasta el objetivo elegido (80-120 %).
- **Pedal Bluetooth o teclado:** Av Pág / Re Pág (o las flechas) pasan de pantalla y Espacio reproduce o pausa. En los PDF el pedal pasa de página.

En los visores la pantalla no se apaga, y la reproducción se pausa si la app pasa a segundo plano, entra una llamada, suena otra app o se desconectan los auriculares.

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
core/data/              Repositorios, archivos, biblioteca (Room), preferencias (DataStore), Hilt
core/designsystem/      Temas Atril, Escenario y Taller (Material 3), pantalla encendida y pedal
feature/file-picker/    Pantalla de inicio: selector SAF y biblioteca
feature/tab-viewer/     Visor AlphaTab (WebView + puente JS) y herramientas de práctica
feature/pdf-viewer/     Visor de PDF con PdfRenderer
```

## Diseños

El usuario elige el diseño desde el botón **Diseño** de la pantalla de inicio; la elección se guarda con DataStore.

| Diseño | Estilo |
| --- | --- |
| Atril | Claro, papel y tinta con acento cobalto |
| Escenario | Oscuro, alto contraste con acento ámbar |
| Taller (por defecto) | Violeta expresivo; sigue el modo oscuro del sistema |

Todos los colores están en `core/designsystem/.../Theme.kt`, incluidos los de la partitura (`ScoreSheetStyle`), que llegan a AlphaTab como parámetros de la URL del visor. `bridge.js` solo guarda los de Taller como reserva para abrir la página suelta.

## Assets de AlphaTab

AlphaTab y el SoundFont van empaquetados en el APK para que todo funcione sin conexión:

```
feature/tab-viewer/src/main/assets/alphatab/
├── alphaTab.min.js        @coderline/alphatab 1.8.4 (MPL-2.0)
├── bridge.js              Puente JS <-> Kotlin (window.tab / window.AndroidBridge)
├── viewer.html            Página que carga AlphaTab
├── font/Bravura.woff2     Fuente musical (SIL OFL 1.1)
└── soundfont/sonivox.sf3  SoundFont del sintetizador (Apache 2.0)
```

El WebView sirve estos archivos con `WebViewAssetLoader` en `https://appassets.androidplatform.net/assets/alphatab/`, y la partitura abierta en `/score/current`, sin habilitar acceso a `file://`.

Para actualizar AlphaTab:

```bash
npm pack @coderline/alphatab@<versión>
tar xzf coderline-alphatab-<versión>.tgz
cp package/dist/alphaTab.min.js feature/tab-viewer/src/main/assets/alphatab/
cp package/dist/font/Bravura.woff2 feature/tab-viewer/src/main/assets/alphatab/font/
cp package/dist/soundfont/sonivox.sf3 feature/tab-viewer/src/main/assets/alphatab/soundfont/
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

Cada push y pull request ejecuta los tests, lint y compila el APK debug en GitHub Actions (`.github/workflows/android.yml`). El APK queda disponible como artefacto `app-debug` y el informe de lint como `lint-report`.

Dependabot (`.github/dependabot.yml`) propone cada lunes las actualizaciones de Gradle y de las acciones. Hilt se queda en 2.57.x hasta migrar a AGP 9.

Para exigir el CI en verde antes de cada merge, activa en GitHub **Settings → Branches → Add rule** para `main` con *Require status checks to pass* y marca el check `build`.

El CI también compila la release con R8 (`:app:assembleRelease`, sin firmar) para detectar a tiempo clases que R8 elimine por error.

## Publicar una versión

`.github/workflows/release.yml` compila el APK y el AAB firmados al subir un tag `v*` y crea la release en GitHub con los dos archivos. `versionName` sale del tag (`v1.2.0` → `1.2.0`) y `versionCode` del número de ejecución del workflow.

La primera vez:

1. Crea la clave de firma y guárdala fuera del repositorio (si la pierdes no podrás actualizar la app instalada):

   ```bash
   keytool -genkeypair -v -keystore mi-tablaturas.jks -alias mitablaturas \
     -keyalg RSA -keysize 4096 -validity 10000
   base64 -w0 mi-tablaturas.jks > keystore.b64   # en macOS: base64 -i mi-tablaturas.jks -o keystore.b64
   ```

2. En GitHub, **Settings → Secrets and variables → Actions → New repository secret**, crea:

   | Secreto | Valor |
   | --- | --- |
   | `RELEASE_KEYSTORE_BASE64` | Contenido de `keystore.b64` |
   | `RELEASE_KEYSTORE_PASSWORD` | Contraseña del keystore |
   | `RELEASE_KEY_ALIAS` | `mitablaturas` (el alias que usaste) |
   | `RELEASE_KEY_PASSWORD` | Contraseña de la clave |

Después, cada versión es:

```bash
git tag v0.2.0
git push origin v0.2.0
```

También puedes lanzarlo a mano desde **Actions → Release → Run workflow**; entonces no crea release y deja los archivos como artefacto.

Para firmar en local, exporta las mismas variables (`RELEASE_KEYSTORE_PATH` con la ruta al `.jks`, más las tres contraseñas y el alias) y ejecuta `./gradlew :app:assembleRelease`.

## Convenciones

- Commits con [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/): `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`, `ci:`.
- Una rama por cambio y pull request hacia `main`.

## Idiomas

Los textos están en `res/values/strings.xml` (español, idioma por defecto) y `res/values-en/strings.xml` (inglés) de cada módulo. Para añadir un idioma, crea `values-<código>/strings.xml` en `app`, `feature/file-picker`, `feature/tab-viewer` y `feature/pdf-viewer`; lint avisa si falta algún texto.

## Licencias de terceros

La app muestra estos avisos y sus textos completos en **Acerca de y licencias**, en la pantalla de inicio.

- AlphaTab: MPL-2.0 (`assets/alphatab/LICENSE-alphaTab.txt`)
- Bravura: SIL Open Font License 1.1 (`assets/alphatab/font/Bravura-OFL.txt`)
- Sonivox SoundFont: Apache 2.0 (`assets/alphatab/soundfont/LICENSE-sonivox.txt`)
