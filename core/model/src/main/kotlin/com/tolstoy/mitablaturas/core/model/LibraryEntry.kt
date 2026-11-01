package com.tolstoy.mitablaturas.core.model

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Tablatura abierta alguna vez, tal como aparece en la biblioteca de la pantalla de inicio.
 *
 * @property lastOpenedAt momento de la última apertura, en milisegundos desde epoch.
 * @property practice ajustes con los que se dejó la canción la última vez.
 * @property title título y [artist] artista que trae el archivo; vacíos hasta abrirlo en el visor
 * de partituras, y siempre en los PDF.
 * @property openCount veces que se abrió.
 */
data class LibraryEntry(
    val document: TabDocument,
    val lastOpenedAt: Long,
    val isFavorite: Boolean = false,
    val practice: PracticeSettings = PracticeSettings(),
    val title: String = "",
    val artist: String = "",
    val openCount: Int = 1,
) {
    /** Lo que se muestra como nombre: el título del archivo o, si no trae, el nombre sin extensión. */
    val displayTitle: String get() = title.ifBlank { document.displayName.substringBeforeLast('.') }
}

/**
 * Ajustes de práctica que se recuerdan por canción.
 *
 * @property trackIndex pista mostrada (índice en la partitura).
 * @property speed velocidad de reproducción (1 = 100 %).
 * @property positionMs posición del cursor, en milisegundos.
 * @property page página de un PDF en la que se dejó, desde 0.
 */
data class PracticeSettings(
    val trackIndex: Int = 0,
    val speed: Float = 1f,
    val positionMs: Long = 0,
    val page: Int = 0,
)

/** Filtros de la biblioteca. */
enum class LibraryFilter {
    ALL,
    GUITAR_PRO,
    PDF,
    FAVORITES;

    fun matches(entry: LibraryEntry): Boolean = when (this) {
        ALL -> true
        GUITAR_PRO -> entry.document.format.isGuitarPro
        PDF -> entry.document.format == TabFormat.PDF
        FAVORITES -> entry.isFavorite
    }
}

/** Aplica [filter] conservando el orden (de la más reciente a la más antigua). */
fun List<LibraryEntry>.filteredBy(filter: LibraryFilter): List<LibraryEntry> = filter { filter.matches(it) }

/** Orden de la biblioteca; se guarda entre sesiones. */
enum class LibrarySort {
    /** La última que se abrió, primero. */
    RECENT,

    /** Por nombre, de la A a la Z. */
    NAME,

    /** Las que más veces se abrieron, primero. */
    MOST_OPENED;

    companion object {
        val DEFAULT = RECENT

        /** Orden guardado como [name], o [DEFAULT] si no se reconoce. */
        fun fromKey(key: String?): LibrarySort = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}

/**
 * Ordena según [sort]. Los empates se resuelven por la apertura más reciente. Por nombre se
 * ordena como en un diccionario del idioma del teléfono: sin distinguir mayúsculas ni tildes.
 */
fun List<LibraryEntry>.ordered(sort: LibrarySort, locale: Locale = Locale.getDefault()): List<LibraryEntry> {
    val recentFirst = compareByDescending<LibraryEntry> { it.lastOpenedAt }
    val comparator = when (sort) {
        LibrarySort.RECENT -> recentFirst
        LibrarySort.NAME -> {
            val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
            compareBy<LibraryEntry, String>(collator) { it.displayTitle.trim() }.then(recentFirst)
        }
        LibrarySort.MOST_OPENED -> compareByDescending<LibraryEntry> { it.openCount }.then(recentFirst)
    }
    return sortedWith(comparator)
}

/**
 * Entradas en cuyo título, artista o nombre de archivo aparecen todas las palabras de [query],
 * sin distinguir mayúsculas ni tildes: «cancion» encuentra «Canción». Sin palabras, devuelve todas.
 */
fun List<LibraryEntry>.matching(query: String): List<LibraryEntry> {
    val words = searchKey(query).split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return this
    return filter { entry ->
        val text = searchKey("${entry.title} ${entry.artist} ${entry.document.displayName}")
        words.all { it in text }
    }
}

private val combiningMarks = Regex("\\p{Mn}+")
private val spaces = Regex("\\s+")

/** Texto sin tildes, en minúsculas y con los espacios unificados, para comparar búsquedas. */
internal fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace(spaces, " ")
        .trim()
