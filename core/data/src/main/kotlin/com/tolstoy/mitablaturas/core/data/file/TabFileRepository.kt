package com.tolstoy.mitablaturas.core.data.file

import com.tolstoy.mitablaturas.core.model.TabDocument

/** Acceso a los archivos de tablatura elegidos por el usuario mediante el Storage Access Framework. */
interface TabFileRepository {

    /**
     * Lee los metadatos de [uri] (nombre y tamaño), valida el formato y conserva
     * el permiso de lectura para poder reabrirlo más tarde.
     *
     * Falla con una [com.tolstoy.mitablaturas.core.model.TabFileException].
     */
    suspend fun resolve(uri: String): Result<TabDocument>

    /** Lee el contenido completo de [document]. Falla con una `TabFileException`. */
    suspend fun readBytes(document: TabDocument): Result<ByteArray>

    /**
     * Guarda una copia local de un archivo con permiso temporal ([TabDocument.hasLastingAccess]
     * `false`) y devuelve el documento que apunta a ella, para poder reabrirlo desde la biblioteca.
     */
    suspend fun importCopy(document: TabDocument, bytes: ByteArray): Result<TabDocument> = Result.success(document)
}
