package com.tolstoy.mitablaturas.core.data.preferences

import com.tolstoy.mitablaturas.core.model.AppDesign
import kotlinx.coroutines.flow.Flow

/** Preferencias del usuario que deben sobrevivir entre sesiones. */
interface UserPreferencesRepository {
    val design: Flow<AppDesign>
    suspend fun setDesign(design: AppDesign)
}
