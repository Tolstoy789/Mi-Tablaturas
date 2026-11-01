package com.tolstoy.mitablaturas.feature.pdfviewer

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.di.ApplicationScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val uri: String = checkNotNull(savedStateHandle[ARG_URI]) { "Falta el argumento $ARG_URI" }

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    @Volatile private var pages: PdfPages? = null

    /** URI con la que el PDF está en la biblioteca (la copia local si llegó con permiso temporal). */
    private var libraryUri: String? = null
    private var savedPage = 0
    private var savePageJob: Job? = null

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
        libraryUri = stored.uri
        savedPage = library.practiceSettings(stored.uri)?.page ?: 0
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
        // Si el archivo cambió y tiene menos páginas, se vuelve a la última.
        val restore = savedPage.coerceIn(0, (opened.pageSizes.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(pageSizes = opened.pageSizes, isLoading = false, pageToRestore = restore.takeIf { page -> page > 0 })
        }
    }

    /** La pantalla ya mostró [PdfViewerUiState.pageToRestore]; desde ahora se guarda la página visible. */
    fun onPageRestored() {
        _uiState.update { it.copy(pageToRestore = null) }
    }

    /**
     * Página visible arriba del todo. Se guarda tras un momento sin cambios, para no escribir en
     * cada página que pasa al desplazarse rápido. Se ignora hasta volver a la página guardada.
     */
    fun onPageChanged(page: Int) {
        val state = _uiState.value
        val target = libraryUri
        if (target == null || state.isLoading || state.pageToRestore != null || page == savedPage) return
        savedPage = page
        savePageJob?.cancel()
        savePageJob = appScope.launch {
            delay(PAGE_SAVE_DELAY_MS)
            library.savePdfPage(target, page)
        }
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

        internal const val PAGE_SAVE_DELAY_MS = 500L
    }
}
