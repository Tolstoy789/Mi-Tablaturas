package com.tolstoy.mitablaturas.feature.filepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
import com.tolstoy.mitablaturas.core.model.LibrarySort
import com.tolstoy.mitablaturas.core.model.filteredBy
import com.tolstoy.mitablaturas.core.model.matching
import com.tolstoy.mitablaturas.core.model.ordered
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
    private val query = MutableStateFlow("")

    /** Tablaturas de la biblioteca con el filtro, la búsqueda y el orden (guardado) aplicados. */
    val libraryState: StateFlow<LibraryUiState> =
        combine(library.entries, filter, query, preferences.librarySort) { entries, selected, text, sort ->
            LibraryUiState(
                entries = entries.filteredBy(selected).matching(text).ordered(sort),
                filter = selected,
                sort = sort,
                query = text,
                totalCount = entries.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onFilterSelected(selected: LibraryFilter) {
        filter.value = selected
    }

    /** Texto de la búsqueda; la pantalla guarda el del campo y lo envía en cada cambio. */
    fun onQueryChanged(text: String) {
        query.value = text
    }

    fun onSortSelected(sort: LibrarySort) {
        viewModelScope.launch { preferences.setLibrarySort(sort) }
    }

    fun onFavoriteToggled(entry: LibraryEntry) {
        viewModelScope.launch { library.setFavorite(entry.document.uri, !entry.isFavorite) }
    }

    fun onRemoveFromLibrary(entry: LibraryEntry) {
        viewModelScope.launch { library.remove(entry.document.uri) }
    }
}
