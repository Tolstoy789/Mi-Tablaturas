package com.tolstoy.mitablaturas.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.model.ScoreScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    // Un archivo de preferencias dañado no debe impedir abrir la app: se usan los valores por defecto.
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val design: Flow<AppDesign> = preferences
        .map { prefs -> AppDesign.fromKey(prefs[KEY_DESIGN]) }
        .distinctUntilChanged()

    override suspend fun setDesign(design: AppDesign) {
        dataStore.edit { it[KEY_DESIGN] = design.key }
    }

    override val scoreScale: Flow<Float> = preferences
        .map { prefs -> ScoreScale.normalize(prefs[KEY_SCORE_SCALE] ?: ScoreScale.DEFAULT) }
        .distinctUntilChanged()

    override suspend fun setScoreScale(scale: Float) {
        dataStore.edit { it[KEY_SCORE_SCALE] = ScoreScale.normalize(scale) }
    }

    override val tabOnly: Flow<Boolean> = preferences
        .map { prefs -> prefs[KEY_TAB_ONLY] ?: false }
        .distinctUntilChanged()

    override suspend fun setTabOnly(enabled: Boolean) {
        dataStore.edit { it[KEY_TAB_ONLY] = enabled }
    }

    private companion object {
        val KEY_DESIGN = stringPreferencesKey("design")
        val KEY_SCORE_SCALE = floatPreferencesKey("score_scale")
        val KEY_TAB_ONLY = booleanPreferencesKey("tab_only")
    }
}
