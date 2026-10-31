package com.tolstoy.mitablaturas.core.data.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFormat

/** Fila de la biblioteca: un archivo abierto alguna vez y sus ajustes de práctica. */
@Entity(tableName = "recent_documents")
data class RecentDocumentEntity(
    @PrimaryKey val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    /** Nombre del enum [TabFormat]. */
    val format: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long?,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "track_index") val trackIndex: Int = 0,
    val speed: Float = 1f,
    @ColumnInfo(name = "position_ms") val positionMs: Long = 0,
)

/** `null` si el formato guardado ya no existe en esta versión de la app. */
internal fun RecentDocumentEntity.toModel(): LibraryEntry? {
    val tabFormat = TabFormat.entries.firstOrNull { it.name == format } ?: return null
    return LibraryEntry(
        document = TabDocument(uri = uri, displayName = displayName, format = tabFormat, sizeBytes = sizeBytes),
        lastOpenedAt = lastOpenedAt,
        isFavorite = isFavorite,
        practice = toPracticeSettings(),
    )
}

internal fun RecentDocumentEntity.toPracticeSettings() =
    PracticeSettings(trackIndex = trackIndex, speed = speed, positionMs = positionMs)
