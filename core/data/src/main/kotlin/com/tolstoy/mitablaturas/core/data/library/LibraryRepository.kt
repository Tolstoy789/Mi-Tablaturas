package com.tolstoy.mitablaturas.core.data.library

import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import kotlinx.coroutines.flow.Flow

/** Tablaturas abiertas recientemente, favoritas y ajustes de práctica por canción. */
interface LibraryRepository {

    /** De la más reciente a la más antigua. */
    val entries: Flow<List<LibraryEntry>>

    /** Añade [document] a la biblioteca o actualiza su fecha de apertura. */
    suspend fun recordOpened(document: TabDocument)

    suspend fun setFavorite(uri: String, favorite: Boolean)

    /** Quita la entrada, libera su permiso de lectura y borra la copia local si la había. */
    suspend fun remove(uri: String)

    /** Ajustes guardados de [uri], o `null` si nunca se abrió. */
    suspend fun practiceSettings(uri: String): PracticeSettings?

    suspend fun savePracticeSettings(uri: String, settings: PracticeSettings)

    /** Título y artista que trae la partitura, para mostrarlos y buscarlos en la biblioteca. */
    suspend fun saveScoreInfo(uri: String, title: String, artist: String)

    /** Página del PDF en la que se dejó (desde 0); se lee en [PracticeSettings.page]. */
    suspend fun savePdfPage(uri: String, page: Int)
}
