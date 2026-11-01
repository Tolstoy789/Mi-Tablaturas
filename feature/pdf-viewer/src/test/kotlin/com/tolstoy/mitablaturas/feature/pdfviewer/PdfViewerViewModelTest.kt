package com.tolstoy.mitablaturas.feature.pdfviewer

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PdfViewerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRepository(private val document: Result<TabDocument>) : TabFileRepository {
        override suspend fun resolve(uri: String) = document
        override suspend fun readBytes(document: TabDocument) = Result.success(byteArrayOf(1, 2, 3))
    }

    private class FakeLibrary : LibraryRepository {
        val opened = mutableListOf<TabDocument>()
        val pages = mutableMapOf<String, Int>()
        val savedPages = mutableListOf<Int>()
        override val entries = MutableStateFlow<List<LibraryEntry>>(emptyList())
        override suspend fun recordOpened(document: TabDocument) { opened += document }
        override suspend fun setFavorite(uri: String, favorite: Boolean) = Unit
        override suspend fun remove(uri: String) = Unit
        override suspend fun practiceSettings(uri: String): PracticeSettings? = pages[uri]?.let { PracticeSettings(page = it) }
        override suspend fun savePracticeSettings(uri: String, settings: PracticeSettings) = Unit
        override suspend fun saveScoreInfo(uri: String, title: String, artist: String) = Unit
        override suspend fun savePdfPage(uri: String, page: Int) {
            pages[uri] = page
            savedPages += page
        }
    }

    private class FakePages(override val pageSizes: List<PageSize>) : PdfPages {
        var closed = false
        override suspend fun render(index: Int, widthPx: Int): Bitmap = error("No se dibuja en tests")
        override fun close() { closed = true }
    }

    private val pdf = TabDocument("content://1", "Cancionero.pdf", TabFormat.PDF, 3)
    private val library = FakeLibrary()

    private fun TestScope.viewModel(document: Result<TabDocument>, openPages: () -> PdfPages) = PdfViewerViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PdfViewerViewModel.ARG_URI to "content://1")),
        repository = FakeRepository(document),
        library = library,
        opener = object : PdfDocumentOpener {
            override suspend fun open(bytes: ByteArray): PdfPages = openPages()
        },
        appScope = this,
    )

    private val threePages = List(3) { PageSize(595, 842) }

    @Test
    fun `abre el pdf, muestra sus paginas y lo guarda en la biblioteca`() = runTest(dispatcher) {
        val sizes = listOf(PageSize(595, 842), PageSize(595, 842))
        val vm = viewModel(Result.success(pdf)) { FakePages(sizes) }
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(sizes, state.pageSizes)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Cancionero", state.title)
        assertEquals(listOf(pdf), library.opened)
    }

    @Test
    fun `vuelve a la pagina en la que se dejo y despues guarda la visible`() = runTest(dispatcher) {
        library.pages["content://1"] = 1
        val vm = viewModel(Result.success(pdf)) { FakePages(threePages) }
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.pageToRestore)

        // Hasta volver a la página guardada, la primera página visible no la pisa.
        vm.onPageChanged(0)
        advanceUntilIdle()
        assertEquals(emptyList<Int>(), library.savedPages)

        vm.onPageRestored()
        assertNull(vm.uiState.value.pageToRestore)
        vm.onPageChanged(2)
        advanceUntilIdle()
        assertEquals(listOf(2), library.savedPages)
    }

    @Test
    fun `al pasar paginas rapido solo guarda la ultima`() = runTest(dispatcher) {
        val vm = viewModel(Result.success(pdf)) { FakePages(threePages) }
        advanceUntilIdle()
        assertNull(vm.uiState.value.pageToRestore)

        vm.onPageChanged(1)
        advanceTimeBy(PdfViewerViewModel.PAGE_SAVE_DELAY_MS / 2)
        vm.onPageChanged(2)
        advanceUntilIdle()
        assertEquals(listOf(2), library.savedPages)
    }

    @Test
    fun `una pagina guardada que ya no existe vuelve a la ultima`() = runTest(dispatcher) {
        library.pages["content://1"] = 9
        val vm = viewModel(Result.success(pdf)) { FakePages(threePages) }
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.pageToRestore)
    }

    @Test
    fun `un pdf con contrasena muestra su error`() = runTest(dispatcher) {
        val vm = viewModel(Result.success(pdf)) { throw SecurityException("password") }
        advanceUntilIdle()
        assertEquals(PdfViewerError.PROTECTED, vm.uiState.value.error)
    }

    @Test
    fun `un pdf danado muestra su error`() = runTest(dispatcher) {
        val vm = viewModel(Result.success(pdf)) { throw IOException("not a PDF") }
        advanceUntilIdle()
        assertEquals(PdfViewerError.DAMAGED, vm.uiState.value.error)
    }

    @Test
    fun `un archivo que no es pdf no se abre`() = runTest(dispatcher) {
        val gp5 = pdf.copy(displayName = "Blues.gp5", format = TabFormat.GUITAR_PRO_5)
        val vm = viewModel(Result.success(gp5)) { error("no debería abrirse") }
        advanceUntilIdle()
        assertEquals(PdfViewerError.UNSUPPORTED, vm.uiState.value.error)
    }

    @Test
    fun `un archivo ilegible muestra su error`() = runTest(dispatcher) {
        val vm = viewModel(Result.failure(TabFileException.Unreadable("x.pdf"))) { error("no debería abrirse") }
        advanceUntilIdle()
        assertEquals(PdfViewerError.UNREADABLE, vm.uiState.value.error)
    }

    @Test
    fun `una pagina sin pdf abierto no se dibuja`() = runTest(dispatcher) {
        val vm = viewModel(Result.failure(TabFileException.Unreadable("x.pdf"))) { error("no debería abrirse") }
        advanceUntilIdle()
        assertNull(vm.renderPage(0, 1080))
    }
}
