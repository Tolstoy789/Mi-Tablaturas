package com.tolstoy.mitablaturas.core.data.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecentDocumentDao {

    @Query("SELECT * FROM recent_documents ORDER BY last_opened_at DESC")
    abstract fun observeAll(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents WHERE uri = :uri")
    abstract suspend fun get(uri: String): RecentDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: RecentDocumentEntity): Long

    @Query(
        """
        UPDATE recent_documents
        SET display_name = :displayName, format = :format, size_bytes = :sizeBytes, last_opened_at = :openedAt,
            open_count = open_count + 1
        WHERE uri = :uri
        """,
    )
    abstract suspend fun updateOpened(uri: String, displayName: String, format: String, sizeBytes: Long?, openedAt: Long)

    /** Inserta la fila o, si ya existía, actualiza sus datos y suma una apertura sin perder favorita ni ajustes. */
    @Transaction
    open suspend fun recordOpened(entity: RecentDocumentEntity) {
        if (insert(entity) == -1L) {
            updateOpened(entity.uri, entity.displayName, entity.format, entity.sizeBytes, entity.lastOpenedAt)
        }
    }

    @Query("UPDATE recent_documents SET is_favorite = :favorite WHERE uri = :uri")
    abstract suspend fun setFavorite(uri: String, favorite: Boolean)

    @Query("UPDATE recent_documents SET track_index = :trackIndex, speed = :speed, position_ms = :positionMs WHERE uri = :uri")
    abstract suspend fun updatePractice(uri: String, trackIndex: Int, speed: Float, positionMs: Long)

    @Query("UPDATE recent_documents SET title = :title, artist = :artist WHERE uri = :uri")
    abstract suspend fun updateScoreInfo(uri: String, title: String, artist: String)

    @Query("UPDATE recent_documents SET pdf_page = :page WHERE uri = :uri")
    abstract suspend fun updatePdfPage(uri: String, page: Int)

    @Query("DELETE FROM recent_documents WHERE uri = :uri")
    abstract suspend fun delete(uri: String)

    /** No favoritas que quedan fuera de las [keep] más recientes. */
    @Query(
        """
        SELECT uri FROM recent_documents
        WHERE is_favorite = 0
        ORDER BY last_opened_at DESC
        LIMIT -1 OFFSET :keep
        """,
    )
    abstract suspend fun staleUris(keep: Int): List<String>
}
