package com.tolstoy.mitablaturas.core.data.preferences

import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.model.LibrarySort
import kotlinx.coroutines.flow.Flow

/** Preferencias del usuario que deben sobrevivir entre sesiones. */
interface UserPreferencesRepository {
    val design: Flow<AppDesign>
    suspend fun setDesign(design: AppDesign)

    /** Tamaño de la partitura en el visor; ver [com.tolstoy.mitablaturas.core.model.ScoreScale]. */
    val scoreScale: Flow<Float>
    suspend fun setScoreScale(scale: Float)

    /** El visor muestra solo la tablatura, sin pentagrama. */
    val tabOnly: Flow<Boolean>
    suspend fun setTabOnly(enabled: Boolean)

    /** Orden de la biblioteca en la pantalla de inicio. */
    val librarySort: Flow<LibrarySort>
    suspend fun setLibrarySort(sort: LibrarySort)
}
