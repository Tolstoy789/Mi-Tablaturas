#!/usr/bin/env bash
# Ejecuta las pruebas instrumentadas del visor en el emulador y deja en el log lo que escribió
# la página (tamaño del visor, SVG dibujados, avisos de AlphaTab).
set -u
adb logcat -c || true
./gradlew :feature:tab-viewer:connectedDebugAndroidTest --stacktrace
status=$?
echo "----- logcat (AlphaTab y consola del WebView) -----"
adb logcat -d -v brief -s AlphaTab:D chromium:I TestRunner:I | tail -n 120 || true
exit $status
