# Mantiene los métodos expuestos a JavaScript (puente AlphaTab <-> Kotlin).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# La biblioteca (Room) guarda TabFormat por su nombre: no debe cambiar entre versiones.
-keepclassmembers enum com.tolstoy.mitablaturas.core.model.TabFormat {
    <fields>;
}
