package com.tolstoy.mitablaturas.core.model

/**
 * Referencia a un archivo de tablatura elegido por el usuario.
 *
 * @property uri URI de contenido devuelta por el Storage Access Framework, como texto.
 * @property displayName nombre visible del archivo (incluye la extensión).
 * @property format formato detectado a partir de [displayName].
 * @property sizeBytes tamaño en bytes, o `null` si el proveedor no lo informa.
 */
data class TabDocument(
    val uri: String,
    val displayName: String,
    val format: TabFormat,
    val sizeBytes: Long?,
)
