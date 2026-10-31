package com.tolstoy.mitablaturas.core.data.file

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.tolstoy.mitablaturas.core.data.di.IoDispatcher
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class ContentResolverTabFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TabFileRepository {

    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun resolve(uri: String): Result<TabDocument> = withContext(ioDispatcher) {
        runCatchingTab(fileName = null) {
            val parsed = Uri.parse(uri)
            val localCopy = ImportedFiles.localFile(context, uri)
            val (displayName, size) = if (localCopy != null) {
                if (!localCopy.exists()) throw TabFileException.Unreadable(localCopy.name)
                localCopy.name to localCopy.length()
            } else {
                queryMetadata(parsed)
            }
            // Algunas apps comparten el archivo sin extensión en el nombre: se mira su cabecera.
            val format = TabFormat.fromFileName(displayName) ?: sniffFormat(parsed)
            val document = TabDocumentValidator.validate(uri, displayName, size, format)
            val lasting = localCopy != null || persistReadPermission(parsed)
            document.copy(hasLastingAccess = lasting)
        }
    }

    override suspend fun importCopy(document: TabDocument, bytes: ByteArray): Result<TabDocument> =
        withContext(ioDispatcher) {
            runCatchingTab(document.displayName) {
                // Si llegó sin extensión, se añade la del formato detectado para reconocerlo después.
                val name = if (TabFormat.fromFileName(document.displayName) == null) {
                    "${document.displayName}.${document.format.extensions.first()}"
                } else {
                    document.displayName
                }
                val file = ImportedFiles.save(context, name, bytes)
                document.copy(
                    uri = Uri.fromFile(file).toString(),
                    displayName = file.name,
                    sizeBytes = bytes.size.toLong(),
                    hasLastingAccess = true,
                )
            }
        }

    override suspend fun readBytes(document: TabDocument): Result<ByteArray> = withContext(ioDispatcher) {
        runCatchingTab(document.displayName) {
            val stream = resolver.openInputStream(Uri.parse(document.uri))
                ?: throw TabFileException.Unreadable(document.displayName)
            val bytes = stream.use { it.readLimited(document.displayName) }
            TabDocumentValidator.checkSize(document.displayName, bytes.size.toLong())
            bytes
        }
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long?> {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                return (name ?: uri.lastPathSegment.orEmpty()) to size
            }
        }
        throw TabFileException.Unreadable(uri.lastPathSegment)
    }

    private fun sniffFormat(uri: Uri): TabFormat? {
        val stream = resolver.openInputStream(uri) ?: return null
        val header = ByteArray(TabFormat.HEADER_SIZE)
        var filled = 0
        stream.use {
            while (filled < header.size) {
                val read = it.read(header, filled, header.size - filled)
                if (read == -1) break
                filled += read
            }
        }
        return TabFormat.fromHeader(header.copyOf(filled))
    }

    /**
     * No todos los proveedores conceden permisos persistentes; si falla, el archivo sigue
     * abierto en esta sesión. Devuelve `true` si el permiso queda guardado.
     */
    private fun persistReadPermission(uri: Uri): Boolean {
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return true
        } catch (_: SecurityException) {
            // Permiso solo temporal: suficiente para abrir el archivo ahora.
        }
        // Puede que ya estuviera guardado de una apertura anterior.
        return resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }

    /** Lee como mucho [TabDocumentValidator.MAX_SIZE_BYTES] para no agotar memoria con archivos enormes. */
    private fun InputStream.readLimited(fileName: String): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > TabDocumentValidator.MAX_SIZE_BYTES) {
                throw TabFileException.TooLarge(fileName, total, TabDocumentValidator.MAX_SIZE_BYTES)
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private inline fun <T> runCatchingTab(fileName: String?, block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: TabFileException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(TabFileException.Unreadable(fileName, e))
        } catch (e: SecurityException) {
            Result.failure(TabFileException.Unreadable(fileName, e))
        } catch (e: IllegalArgumentException) {
            Result.failure(TabFileException.Unreadable(fileName, e))
        }
}
