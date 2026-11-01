package com.tolstoy.mitablaturas.core.model

/**
 * Formatos de tablatura soportados por la app.
 *
 * [isInteractive] indica si el archivo se abre con AlphaTab (render + audio)
 * o con el visor estático de PDF. [isGuitarPro] agrupa los formatos de Guitar Pro.
 */
enum class TabFormat(val extensions: Set<String>, val isInteractive: Boolean, val isGuitarPro: Boolean = false) {
    GUITAR_PRO_3(setOf("gp3"), isInteractive = true, isGuitarPro = true),
    GUITAR_PRO_4(setOf("gp4"), isInteractive = true, isGuitarPro = true),
    GUITAR_PRO_5(setOf("gp5"), isInteractive = true, isGuitarPro = true),
    GUITAR_PRO_6(setOf("gpx"), isInteractive = true, isGuitarPro = true),
    GUITAR_PRO_7(setOf("gp"), isInteractive = true, isGuitarPro = true),

    /** Partitura MusicXML: `.musicxml` o `.xml` sin comprimir, `.mxl` comprimida (un ZIP). */
    MUSIC_XML(setOf("musicxml", "mxl", "xml"), isInteractive = true),
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

        /** Bytes que necesita [fromHeader]: lo suficiente para ver la raíz de un MusicXML. */
        const val HEADER_SIZE = 512

        private val legacyGuitarPro = Regex("^[\\s\\S](?:FICHIER|CLIPBOARD) GUITAR PRO v(\\d)")
        private val musicXmlRoot = Regex("<(?:!DOCTYPE\\s+)?score-(?:partwise|timewise)\\b")

        /** Nombre del primer archivo de un ZIP, que empieza en el byte 30 de su cabecera local. */
        private const val ZIP_NAME_OFFSET = 30

        /**
         * Detecta el formato por los primeros bytes del archivo, para cuando otra app lo
         * comparte sin extensión en el nombre. Devuelve `null` si no lo reconoce.
         *
         * - GP3 a GP5: un byte de longitud seguido de `FICHIER GUITAR PRO vN.NN`.
         * - GPX (Guitar Pro 6): contenedor `BCFZ` (comprimido) o `BCFS`.
         * - GP (Guitar Pro 7 y 8) y MXL: ZIP. El MXL empieza por `mimetype` o `META-INF/`.
         * - MusicXML sin comprimir: XML con raíz `score-partwise` o `score-timewise`.
         * - PDF: `%PDF-`.
         */
        fun fromHeader(header: ByteArray): TabFormat? {
            val ascii = String(header, Charsets.ISO_8859_1)
            return when {
                ascii.startsWith("%PDF-") -> PDF
                ascii.startsWith("BCFZ") || ascii.startsWith("BCFS") -> GUITAR_PRO_6
                ascii.startsWith("PK\u0003\u0004") -> if (isMusicXmlZip(header)) MUSIC_XML else GUITAR_PRO_7
                musicXmlRoot.containsMatchIn(decodeText(header)) -> MUSIC_XML
                else -> when (legacyGuitarPro.find(ascii)?.groupValues?.get(1)) {
                    "3" -> GUITAR_PRO_3
                    "4" -> GUITAR_PRO_4
                    "5" -> GUITAR_PRO_5
                    else -> null
                }
            }
        }

        private fun isMusicXmlZip(header: ByteArray): Boolean {
            if (header.size < ZIP_NAME_OFFSET) return false
            val nameLength = (header[26].toInt() and 0xFF) or ((header[27].toInt() and 0xFF) shl 8)
            val end = minOf(header.size, ZIP_NAME_OFFSET + nameLength)
            val firstName = String(header, ZIP_NAME_OFFSET, end - ZIP_NAME_OFFSET, Charsets.ISO_8859_1)
            return firstName == "mimetype" || firstName.startsWith("META-INF/")
        }

        /** Texto de la cabecera según su BOM: UTF-16 si lo indica y, si no, UTF-8. */
        private fun decodeText(header: ByteArray): String {
            val b0 = header.getOrNull(0)?.toInt()?.and(0xFF)
            val b1 = header.getOrNull(1)?.toInt()?.and(0xFF)
            return when {
                b0 == 0xFF && b1 == 0xFE -> String(header, Charsets.UTF_16LE)
                b0 == 0xFE && b1 == 0xFF -> String(header, Charsets.UTF_16BE)
                else -> String(header, Charsets.UTF_8)
            }
        }
    }
}
