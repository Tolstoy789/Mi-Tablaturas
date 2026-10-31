package com.tolstoy.mitablaturas.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tolstoy.mitablaturas.core.model.AppDesign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val design: Flow<AppDesign> = dataStore.data
        // Un archivo de preferencias dañado no debe impedir abrir la app: se usan los valores por defecto.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> AppDesign.fromKey(prefs[KEY_DESIGN]) }
        .distinctUntilChanged()

    override suspend fun setDesign(design: AppDesign) {
        dataStore.edit { it[KEY_DESIGN] = design.key }
    }

    private companion object {
        val KEY_DESIGN = stringPreferencesKey("design")
    }
}
