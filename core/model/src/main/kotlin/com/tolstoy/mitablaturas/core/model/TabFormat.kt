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

        /** Bytes que necesita [fromHeader]. */
        const val HEADER_SIZE = 32

        private val legacyGuitarPro = Regex("^[\\s\\S](?:FICHIER|CLIPBOARD) GUITAR PRO v(\\d)")

        /**
         * Detecta el formato por los primeros bytes del archivo, para cuando otra app lo
         * comparte sin extensión en el nombre. Devuelve `null` si no lo reconoce.
         *
         * - GP3 a GP5: un byte de longitud seguido de `FICHIER GUITAR PRO vN.NN`.
         * - GPX (Guitar Pro 6): contenedor `BCFZ` (comprimido) o `BCFS`.
         * - GP (Guitar Pro 7 y 8): un ZIP.
         * - PDF: `%PDF-`.
         */
        fun fromHeader(header: ByteArray): TabFormat? {
            val ascii = String(header, Charsets.ISO_8859_1)
            return when {
                ascii.startsWith("%PDF-") -> PDF
                ascii.startsWith("BCFZ") || ascii.startsWith("BCFS") -> GUITAR_PRO_6
                ascii.startsWith("PK\u0003\u0004") -> GUITAR_PRO_7
                else -> when (legacyGuitarPro.find(ascii)?.groupValues?.get(1)) {
                    "3" -> GUITAR_PRO_3
                    "4" -> GUITAR_PRO_4
                    "5" -> GUITAR_PRO_5
                    else -> null
                }
            }
        }
    }
}
