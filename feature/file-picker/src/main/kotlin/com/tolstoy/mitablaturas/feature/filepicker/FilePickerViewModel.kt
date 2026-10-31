package com.tolstoy.mitablaturas.feature.filepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
import com.tolstoy.mitablaturas.core.model.filteredBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePickerViewModel @Inject constructor(
    private val repository: TabFileRepository,
    private val preferences: UserPreferencesRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    /** Diseño elegido por el usuario; se guarda entre sesiones. */
    val design: StateFlow<AppDesign> = preferences.design
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDesign.DEFAULT)

    fun onDesignSelected(design: AppDesign) {
        viewModelScope.launch { preferences.setDesign(design) }
    }

    private val _uiState = MutableStateFlow<FilePickerUiState>(FilePickerUiState.Idle)
    val uiState: StateFlow<FilePickerUiState> = _uiState.asStateFlow()

    private var resolveJob: Job? = null

    /** Llamado con la URI devuelta por `OpenDocument`, o `null` si el usuario canceló. */
    fun onDocumentPicked(uri: String?) {
        if (uri == null) return
        resolveJob?.cancel()
        _uiState.value = FilePickerUiState.Loading
        resolveJob = viewModelScope.launch {
            _uiState.value = repository.resolve(uri).fold(
                onSuccess = { FilePickerUiState.Ready(it) },
                onFailure = { FilePickerUiState.Error(it.toFilePickerError()) },
            )
        }
    }

    fun onErrorDismissed() {
        if (_uiState.value is FilePickerUiState.Error) _uiState.value = FilePickerUiState.Idle
    }

    /** La pantalla ya navegó al visor con el documento elegido. */
    fun onDocumentOpened() {
        if (_uiState.value is FilePickerUiState.Ready) _uiState.value = FilePickerUiState.Idle
    }

    private val filter = MutableStateFlow(LibraryFilter.ALL)

    /** Tablaturas recientes con el filtro aplicado. */
    val libraryState: StateFlow<LibraryUiState> = combine(library.entries, filter) { entries, selected ->
        LibraryUiState(entries = entries.filteredBy(selected), filter = selected, hasEntries = entries.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onFilterSelected(selected: LibraryFilter) {
        filter.value = selected
    }

    fun onFavoriteToggled(entry: LibraryEntry) {
        viewModelScope.launch { library.setFavorite(entry.document.uri, !entry.isFavorite) }
    }

    fun onRemoveFromLibrary(entry: LibraryEntry) {
        viewModelScope.launch { library.remove(entry.document.uri) }
    }
}
