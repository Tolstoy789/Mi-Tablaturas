package com.tolstoy.mitablaturas.feature.pdfviewer

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TabFileRepository,
    private val library: LibraryRepository,
    private val opener: PdfDocumentOpener,
) : ViewModel() {

    private val uri: String = checkNotNull(savedStateHandle[ARG_URI]) { "Falta el argumento $ARG_URI" }

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    @Volatile private var pages: PdfPages? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val document = repository.resolve(uri).getOrElse { error ->
            fail(if (error is TabFileException.UnsupportedFormat) PdfViewerError.UNSUPPORTED else PdfViewerError.UNREADABLE)
            return
        }
        _uiState.update { it.copy(fileName = document.displayName) }
        if (document.format != TabFormat.PDF) return fail(PdfViewerError.UNSUPPORTED)
        val bytes = repository.readBytes(document).getOrElse { return fail(PdfViewerError.UNREADABLE) }

        val stored = if (document.hasLastingAccess) document else repository.importCopy(document, bytes).getOrDefault(document)
        library.recordOpened(stored)

        val opened = try {
            opener.open(bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            return fail(PdfViewerError.PROTECTED)
        } catch (_: IOException) {
            return fail(PdfViewerError.DAMAGED)
        } catch (_: IllegalArgumentException) {
            return fail(PdfViewerError.DAMAGED)
        }
        pages = opened
        _uiState.update { it.copy(pageSizes = opened.pageSizes, isLoading = false) }
    }

    private fun fail(error: PdfViewerError) {
        _uiState.update { it.copy(error = error, isLoading = false) }
    }

    /** Dibuja una página; `null` si el PDF ya no está abierto o falla. */
    suspend fun renderPage(index: Int, widthPx: Int): Bitmap? {
        val current = pages ?: return null
        return try {
            current.render(index, widthPx.coerceIn(1, MAX_PAGE_WIDTH_PX))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        pages?.close()
        pages = null
    }

    companion object {
        const val ARG_URI = "uri"

        /** Suficiente para tablets en horizontal sin gastar memoria de más. */
        private const val MAX_PAGE_WIDTH_PX = 2_048
    }
}
