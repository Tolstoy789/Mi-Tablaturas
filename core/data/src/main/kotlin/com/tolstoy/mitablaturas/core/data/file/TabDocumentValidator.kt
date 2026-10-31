package com.tolstoy.mitablaturas.core.data.file

import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat

/** Reglas de validación independientes de Android, para poder probarlas en la JVM. */
object TabDocumentValidator {

    /** Los Guitar Pro rara vez pasan de 1 MB; 20 MB deja margen para PDFs escaneados. */
    const val MAX_SIZE_BYTES: Long = 20L * 1024 * 1024

    /**
     * @param sizeBytes tamaño informado por el proveedor, o `null` si no lo informa.
     * @throws TabFileException si el archivo no es válido.
     */
    fun validate(uri: String, displayName: String, sizeBytes: Long?): TabDocument {
        val format = TabFormat.fromFileName(displayName)
            ?: throw TabFileException.UnsupportedFormat(displayName)
        checkSize(displayName, sizeBytes)
        return TabDocument(uri = uri, displayName = displayName, format = format, sizeBytes = sizeBytes)
    }

    /** Valida un tamaño conocido; un tamaño `null` (desconocido) se acepta. */
    fun checkSize(displayName: String, sizeBytes: Long?) {
        if (sizeBytes == null) return
        if (sizeBytes == 0L) throw TabFileException.EmptyFile(displayName)
        if (sizeBytes > MAX_SIZE_BYTES) {
            throw TabFileException.TooLarge(displayName, sizeBytes, MAX_SIZE_BYTES)
        }
    }
}
