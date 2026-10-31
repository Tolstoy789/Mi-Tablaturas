package com.tolstoy.mitablaturas.feature.filepicker

import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    }

    private val preferences = FakePreferences()

    private val document = TabDocument("content://1", "Blues en La.gp5", TabFormat.GUITAR_PRO_5, 1_000)

    @Test
    fun `un archivo valido pasa por Loading y termina en Ready`() = runTest(dispatcher) {
        val vm = FilePickerViewModel(FakeRepository(Result.success(document)), preferences)
        vm.onDocumentPicked("content://1")
        assertEquals(FilePickerUiState.Loading, vm.uiState.value)
        advanceUntilIdle()
        assertEquals(FilePickerUiState.Ready(document), vm.uiState.value)
    }

    @Test
    fun `un formato no soportado muestra su error`() = runTest(dispatcher) {
        val vm = FilePickerViewModel(FakeRepository(Result.failure(TabFileException.UnsupportedFormat("a.mp3"))), preferences)
        vm.onDocumentPicked("content://2")
        advanceUntilIdle()
        assertEquals(FilePickerUiState.Error(FilePickerError.UnsupportedFormat("a.mp3")), vm.uiState.value)
        vm.onErrorDismissed()
        assertEquals(FilePickerUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `cancelar el selector no cambia el estado`() = runTest(dispatcher) {
        val vm = FilePickerViewModel(FakeRepository(Result.success(document)), preferences)
        vm.onDocumentPicked(null)
        assertEquals(FilePickerUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `elegir un diseno lo guarda en preferencias`() = runTest(dispatcher) {
        val vm = FilePickerViewModel(FakeRepository(Result.success(document)), preferences)
        vm.onDesignSelected(AppDesign.ESCENARIO)
        advanceUntilIdle()
        assertEquals(AppDesign.ESCENARIO, preferences.design.value)
    }

    @Test
    fun `formatSize usa la unidad adecuada`() {
        assertEquals("512 B", formatSize(512))
        assertEquals("47 KB", formatSize(48_213))
    }
}
