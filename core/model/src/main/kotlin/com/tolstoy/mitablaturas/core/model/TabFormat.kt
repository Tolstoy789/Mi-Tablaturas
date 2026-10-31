package com.tolstoy.mitablaturas.core.model

/**
 * Formatos de tablatura soportados por la app.
 *
 * [isInteractive] indica si el archivo se abre con AlphaTab (render + audio)
 * o con el visor estático de PDF.
 */
enum class TabFormat(val extensions: Set<String>, val isInteractive: Boolean) {
    GUITAR_PRO_3(setOf("gp3"), isInteractive = true),
    GUITAR_PRO_4(setOf("gp4"), isInteractive = true),
    GUITAR_PRO_5(setOf("gp5"), isInteractive = true),
    GUITAR_PRO_6(setOf("gpx"), isInteractive = true),
    GUITAR_PRO_7(setOf("gp"), isInteractive = true),
    PDF(setOf("pdf"), isInteractive = false);

    companion object {
        /**
         * Devuelve el formato a partir del nombre de archivo, o `null` si la
         * extensión no está soportada. La comparación ignora mayúsculas.
         */
        fun fromFileName(fileName: String): TabFormat? {
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
                .trim()
                .lowercase()
            if (extension.isEmpty()) return null
            return entries.firstOrNull { extension in it.extensions }
        }
    }
}
