# Mantiene los métodos expuestos a JavaScript (puente AlphaTab <-> Kotlin).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
