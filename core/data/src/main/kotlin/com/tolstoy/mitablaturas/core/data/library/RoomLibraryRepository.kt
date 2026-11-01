package com.tolstoy.mitablaturas.core.data.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tolstoy.mitablaturas.core.data.di.IoDispatcher
import com.tolstoy.mitablaturas.core.data.file.ImportedFiles
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomLibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: RecentDocumentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LibraryRepository {

    override val entries: Flow<List<LibraryEntry>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toModel() } }

    override suspend fun recordOpened(document: TabDocument) {
        dao.recordOpened(
            RecentDocumentEntity(
                uri = document.uri,
                displayName = document.displayName,
                format = document.format.name,
                sizeBytes = document.sizeBytes,
                lastOpenedAt = System.currentTimeMillis(),
            ),
        )
        // Android limita los permisos persistentes por app; las favoritas no caducan.
        dao.staleUris(keep = MAX_RECENTS).forEach { remove(it) }
    }

    override suspend fun setFavorite(uri: String, favorite: Boolean) = dao.setFavorite(uri, favorite)

    override suspend fun remove(uri: String) {
        dao.delete(uri)
        withContext(ioDispatcher) { releaseAccess(uri) }
    }

    override suspend fun practiceSettings(uri: String): PracticeSettings? = dao.get(uri)?.toPracticeSettings()

    override suspend fun savePracticeSettings(uri: String, settings: PracticeSettings) =
        dao.updatePractice(uri, settings.trackIndex, settings.speed, settings.positionMs)

    override suspend fun saveScoreInfo(uri: String, title: String, artist: String) =
        dao.updateScoreInfo(uri, title.trim(), artist.trim())

    override suspend fun savePdfPage(uri: String, page: Int) = dao.updatePdfPage(uri, page.coerceAtLeast(0))

    private fun releaseAccess(uri: String) {
        if (ImportedFiles.delete(context, uri)) return
        try {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // El permiso ya no existía (p. ej. era temporal).
        }
    }

    private companion object {
        const val MAX_RECENTS = 100
    }
}
