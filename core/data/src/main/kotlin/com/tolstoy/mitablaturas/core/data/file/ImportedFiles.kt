package com.tolstoy.mitablaturas.core.data.file

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/**
 * Copias locales de archivos compartidos con permiso temporal (WhatsApp, correo...), para que
 * sigan abriéndose desde la biblioteca. Viven en `files/library/<hash>/<nombre>`; el mismo
 * contenido compartido dos veces reutiliza la misma copia.
 */
internal object ImportedFiles {

    private const val DIRECTORY = "library"

    fun root(context: Context): File = File(context.filesDir, DIRECTORY)

    /** Guarda [bytes] y devuelve el archivo creado (o el ya existente con el mismo contenido). */
    fun save(context: Context, displayName: String, bytes: ByteArray): File {
        val folder = File(root(context), contentHash(bytes)).apply { mkdirs() }
        val file = File(folder, safeName(displayName))
        if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
        return file
    }

    /** `true` si [uri] era una copia local y se borró. */
    fun delete(context: Context, uri: String): Boolean {
        val file = localFile(context, uri) ?: return false
        file.parentFile?.deleteRecursively()
        return true
    }

    /** El archivo de [uri] si es una copia dentro de [root]; `null` en cualquier otro caso. */
    fun localFile(context: Context, uri: String): File? {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "file") return null
        val path = parsed.path ?: return null
        val file = File(path).canonicalFile
        val root = root(context).canonicalFile
        return file.takeIf { it.path.startsWith(root.path + File.separator) }
    }

    private fun contentHash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).take(8).joinToString("") { "%02x".format(it) }

    /** Quita separadores y caracteres problemáticos; nunca devuelve un nombre vacío. */
    internal fun safeName(displayName: String): String =
        displayName.replace(Regex("[/\\\\:*?\"<>|\\u0000-\\u001F]"), "_").trim().trimStart('.').ifEmpty { "tablatura" }
}
