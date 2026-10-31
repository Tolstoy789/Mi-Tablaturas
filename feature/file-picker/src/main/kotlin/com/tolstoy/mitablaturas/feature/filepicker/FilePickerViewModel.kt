package com.tolstoy.mitablaturas.feature.filepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePickerViewModel @Inject constructor(
    private val repository: TabFileRepository,
    private val preferences: UserPreferencesRepository,
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
}
