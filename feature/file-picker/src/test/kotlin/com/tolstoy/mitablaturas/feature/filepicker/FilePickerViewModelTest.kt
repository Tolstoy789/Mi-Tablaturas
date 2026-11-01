package com.tolstoy.mitablaturas.feature.filepicker

import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibraryFilter
import com.tolstoy.mitablaturas.core.model.LibrarySort
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilePickerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRepository(private val result: Result<TabDocument>) : TabFileRepository {
        override suspend fun resolve(uri: String) = result
        override suspend fun readBytes(document: TabDocument) = Result.success(ByteArray(0))
    }

    private class FakePreferences : UserPreferencesRepository {
        override val design = MutableStateFlow(AppDesign.DEFAULT)
        override suspend fun setDesign(design: AppDesign) {
            this.design.value = design
        }
        override val scoreScale = MutableStateFlow(0.9f)
        override suspend fun setScoreScale(scale: Float) {
            scoreScale.value = scale
        }
        override val tabOnly = MutableStateFlow(false)
        override suspend fun setTabOnly(enabled: Boolean) {
            tabOnly.value = enabled
        }
        override val librarySort = MutableStateFlow(LibrarySort.DEFAULT)
        override suspend fun setLibrarySort(sort: LibrarySort) {
            librarySort.value = sort
        }
    }

    private class FakeLibrary(initial: List<LibraryEntry> = emptyList()) : LibraryRepository {
        override val entries = MutableStateFlow(initial)
        override suspend fun recordOpened(document: TabDocument) = Unit
        override suspend fun setFavorite(uri: String, favorite: Boolean) {
            entries.value = entries.value.map { if (it.document.uri == uri) it.copy(isFavorite = favorite) else it }
        }
        override suspend fun remove(uri: String) {
            entries.value = entries.value.filterNot { it.document.uri == uri }
        }
        override suspend fun practiceSettings(uri: String): PracticeSettings? = null
        override suspend fun savePracticeSettings(uri: String, settings: PracticeSettings) = Unit
        override suspend fun saveScoreInfo(uri: String, title: String, artist: String) = Unit
        override suspend fun savePdfPage(uri: String, page: Int) = Unit
    }

    private val preferences = FakePreferences()
    private val library = FakeLibrary()

    private fun viewModel(repository: TabFileRepository, library: LibraryRepository = this.library) =
        FilePickerViewModel(repository, preferences, library)

    private val document = TabDocument("content://1", "Blues en La.gp5", TabFormat.GUITAR_PRO_5, 1_000)

    @Test
    fun `un archivo valido pasa por Loading y termina en Ready`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(document)))
        vm.onDocumentPicked("content://1")
        assertEquals(FilePickerUiState.Loading, vm.uiState.value)
        advanceUntilIdle()
        assertEquals(FilePickerUiState.Ready(document), vm.uiState.value)
    }

    @Test
    fun `un formato no soportado muestra su error`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.failure(TabFileException.UnsupportedFormat("a.mp3"))))
        vm.onDocumentPicked("content://2")
        advanceUntilIdle()
        assertEquals(FilePickerUiState.Error(FilePickerError.UnsupportedFormat("a.mp3")), vm.uiState.value)
        vm.onErrorDismissed()
        assertEquals(FilePickerUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `cancelar el selector no cambia el estado`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(document)))
        vm.onDocumentPicked(null)
        assertEquals(FilePickerUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `elegir un diseno lo guarda en preferencias`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(document)))
        vm.onDesignSelected(AppDesign.ESCENARIO)
        advanceUntilIdle()
        assertEquals(AppDesign.ESCENARIO, preferences.design.value)
    }

    @Test
    fun `tras abrir el documento vuelve a Idle`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(document)))
        vm.onDocumentPicked("content://1")
        advanceUntilIdle()
        vm.onDocumentOpened()
        assertEquals(FilePickerUiState.Idle, vm.uiState.value)
    }

    private fun entry(uri: String, format: TabFormat, favorite: Boolean = false) =
        LibraryEntry(TabDocument(uri, "$uri.${format.extensions.first()}", format, 10), lastOpenedAt = 0, isFavorite = favorite)

    @Test
    fun `la biblioteca aplica el filtro elegido`() = runTest(dispatcher) {
        val gp = entry("a", TabFormat.GUITAR_PRO_5)
        val pdf = entry("b", TabFormat.PDF, favorite = true)
        val vm = viewModel(FakeRepository(Result.success(document)), FakeLibrary(listOf(gp, pdf)))
        backgroundScope.launch { vm.libraryState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf(gp, pdf), vm.libraryState.value.entries)

        vm.onFilterSelected(LibraryFilter.FAVORITES)
        advanceUntilIdle()
        assertEquals(listOf(pdf), vm.libraryState.value.entries)
        assertTrue(vm.libraryState.value.hasEntries)
    }

    @Test
    fun `marcar favorita y quitar actualizan la biblioteca`() = runTest(dispatcher) {
        val gp = entry("a", TabFormat.GUITAR_PRO_5)
        val vm = viewModel(FakeRepository(Result.success(document)), FakeLibrary(listOf(gp)))
        backgroundScope.launch { vm.libraryState.collect {} }
        advanceUntilIdle()

        vm.onFavoriteToggled(gp)
        advanceUntilIdle()
        assertTrue(vm.libraryState.value.entries.single().isFavorite)

        vm.onRemoveFromLibrary(gp)
        advanceUntilIdle()
        assertTrue(vm.libraryState.value.entries.isEmpty())
        assertFalse(vm.libraryState.value.hasEntries)
    }

    @Test
    fun `busca y ordena la biblioteca, y recuerda el orden`() = runTest(dispatcher) {
        fun song(uri: String, name: String, openedAt: Long, opens: Int, artist: String = "") = LibraryEntry(
            TabDocument(uri, name, TabFormat.fromFileName(name)!!, 10),
            lastOpenedAt = openedAt,
            artist = artist,
            openCount = opens,
        )
        val zamba = song("a", "Zamba de mi esperanza.gp5", openedAt = 3, opens = 1)
        val blues = song("b", "Blues en La.gp", openedAt = 1, opens = 9, artist = "Elías")
        val coral = song("c", "Coral.musicxml", openedAt = 2, opens = 4)
        val vm = viewModel(FakeRepository(Result.success(document)), FakeLibrary(listOf(zamba, blues, coral)))
        backgroundScope.launch { vm.libraryState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf(zamba, coral, blues), vm.libraryState.value.entries)
        // Con menos de 4 canciones no hacen falta buscar ni ordenar.
        assertFalse(vm.libraryState.value.showTools)

        vm.onSortSelected(LibrarySort.MOST_OPENED)
        advanceUntilIdle()
        assertEquals(listOf(blues, coral, zamba), vm.libraryState.value.entries)
        assertEquals(LibrarySort.MOST_OPENED, preferences.librarySort.value)

        vm.onQueryChanged("ELIAS")
        advanceUntilIdle()
        assertEquals(listOf(blues), vm.libraryState.value.entries)
        assertTrue(vm.libraryState.value.showTools)

        vm.onQueryChanged("nada")
        advanceUntilIdle()
        assertTrue(vm.libraryState.value.entries.isEmpty())
        assertEquals(3, vm.libraryState.value.totalCount)
    }

    @Test
    fun `formatSize usa la unidad adecuada`() {
        assertEquals("512 B", formatSize(512))
        assertEquals("47 KB", formatSize(48_213))
    }
}
