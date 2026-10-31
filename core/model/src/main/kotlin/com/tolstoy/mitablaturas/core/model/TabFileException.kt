package com.tolstoy.mitablaturas.core.model

/** Errores esperables al abrir un archivo de tablatura. */
sealed class TabFileException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** La extensión del archivo no corresponde a ningún [TabFormat]. */
    class UnsupportedFormat(val fileName: String) :
        TabFileException("Formato no soportado: $fileName")

    /** El archivo está vacío (0 bytes). */
    class EmptyFile(val fileName: String) :
        TabFileException("El archivo está vacío: $fileName")

    /** El archivo supera el tamaño máximo permitido. */
    class TooLarge(val fileName: String, val sizeBytes: Long, val maxBytes: Long) :
        TabFileException("El archivo $fileName pesa $sizeBytes bytes (máximo $maxBytes)")

    /** No se pudo leer el archivo (proveedor caído, permiso revocado, archivo borrado…). */
    class Unreadable(val fileName: String?, cause: Throwable? = null) :
        TabFileException("No se pudo leer el archivo ${fileName.orEmpty()}".trim(), cause)
}
