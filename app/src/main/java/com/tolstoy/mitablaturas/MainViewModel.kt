package com.tolstoy.mitablaturas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {

    /** `null` hasta leer la preferencia guardada, para no mostrar un diseño y luego cambiarlo. */
    val design: StateFlow<AppDesign?> = preferences.design
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private val _pendingDocument = MutableStateFlow<String?>(null)

    /** Archivo que otra app pidió abrir y que la navegación aún no ha mostrado. */
    val pendingDocument: StateFlow<String?> = _pendingDocument.asStateFlow()

    fun onExternalDocument(uri: String) {
        _pendingDocument.value = uri
    }

    fun onPendingDocumentOpened() {
        _pendingDocument.value = null
    }
}
