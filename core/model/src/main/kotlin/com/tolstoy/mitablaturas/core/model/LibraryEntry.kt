package com.tolstoy.mitablaturas.core.model

/**
 * Tablatura abierta alguna vez, tal como aparece en la biblioteca de la pantalla de inicio.
 *
 * @property lastOpenedAt momento de la última apertura, en milisegundos desde epoch.
 * @property practice ajustes con los que se dejó la canción la última vez.
 */
data class LibraryEntry(
    val document: TabDocument,
    val lastOpenedAt: Long,
    val isFavorite: Boolean = false,
    val practice: PracticeSettings = PracticeSettings(),
)

/**
 * Ajustes de práctica que se recuerdan por canción.
 *
 * @property trackIndex pista mostrada (índice en la partitura).
 * @property speed velocidad de reproducción (1 = 100 %).
 * @property positionMs posición del cursor, en milisegundos.
 */
data class PracticeSettings(
    val trackIndex: Int = 0,
    val speed: Float = 1f,
    val positionMs: Long = 0,
)

/** Filtros de la biblioteca. */
enum class LibraryFilter {
    ALL,
    GUITAR_PRO,
    PDF,
    FAVORITES;

    fun matches(entry: LibraryEntry): Boolean = when (this) {
        ALL -> true
        GUITAR_PRO -> entry.document.format.isInteractive
        PDF -> entry.document.format == TabFormat.PDF
        FAVORITES -> entry.isFavorite
    }
}

/** Aplica [filter] conservando el orden (de la más reciente a la más antigua). */
fun List<LibraryEntry>.filteredBy(filter: LibraryFilter): List<LibraryEntry> = filter { filter.matches(it) }
