package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.lifecycle.SavedStateHandle
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.designsystem.PedalAction
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabViewerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRepository(
        private val document: Result<TabDocument>,
        private val bytes: Result<ByteArray> = Result.success(byteArrayOf(1, 2, 3)),
    ) : TabFileRepository {
        override suspend fun resolve(uri: String) = document
        override suspend fun readBytes(document: TabDocument) = bytes
        override suspend fun importCopy(document: TabDocument, bytes: ByteArray) =
            Result.success(document.copy(uri = "file:///copia/${document.displayName}", hasLastingAccess = true))
    }

    private class FakeLibrary(saved: Map<String, PracticeSettings> = emptyMap()) : LibraryRepository {
        val opened = mutableListOf<TabDocument>()
        val settings = saved.toMutableMap()
        val removed = mutableListOf<String>()
        override val entries = MutableStateFlow<List<LibraryEntry>>(emptyList())
        override suspend fun recordOpened(document: TabDocument) { opened += document }
        override suspend fun setFavorite(uri: String, favorite: Boolean) = Unit
        override suspend fun remove(uri: String) { removed += uri }
        override suspend fun practiceSettings(uri: String) = settings[uri]
        override suspend fun savePracticeSettings(uri: String, settings: PracticeSettings) {
            this.settings[uri] = settings
        }
    }

    private val gp5 = TabDocument("content://1", "Blues en La.gp5", TabFormat.GUITAR_PRO_5, 3)

    private fun TestScope.viewModel(repository: TabFileRepository, library: LibraryRepository = FakeLibrary()) =
        TabViewerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TabViewerViewModel.ARG_URI to "content://1")),
            repository = repository,
            library = library,
            appScope = this,
        )

    @Test
    fun `carga los bytes de un gp5`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertArrayEquals(byteArrayOf(1, 2, 3), state.score?.bytes)
        assertEquals("Blues en La", state.displayTitle)
        assertNull(state.error)
    }

    @Test
    fun `un pdf se pasa al visor de pdf`() = runTest(dispatcher) {
        val pdf = gp5.copy(displayName = "cancionero.pdf", format = TabFormat.PDF)
        val vm = viewModel(FakeRepository(Result.success(pdf)))
        advanceUntilIdle()
        assertEquals("content://1", vm.uiState.value.openAsPdf)
        assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.score)
    }

    @Test
    fun `un archivo ilegible muestra error`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.failure(TabFileException.Unreadable("x.gp5"))))
        advanceUntilIdle()
        assertEquals(TabViewerError.FileUnreadable, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isRendering)
    }

    @Test
    fun `los eventos del motor actualizan el estado`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.ScoreLoaded("Blues en La", "Elias", listOf("Guitarra", "Bajo")))
        vm.onEngineEvent(AlphaTabEvent.RenderFinished)
        vm.onEngineEvent(AlphaTabEvent.PlayerReady)
        vm.onEngineEvent(AlphaTabEvent.PlayerStateChanged(isPlaying = true))
        vm.onEngineEvent(AlphaTabEvent.PositionChanged(5_000, 120_000, 96))
        val state = vm.uiState.value
        assertEquals(listOf("Guitarra", "Bajo"), state.trackNames)
        assertFalse(state.isRendering)
        assertTrue(state.isPlayerReady)
        assertTrue(state.isPlaying)
        assertEquals(96, state.tempo)
        assertEquals(120_000L, state.durationMs)
    }

    @Test
    fun `elegir pista envia la orden y vuelve a renderizar`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.ScoreLoaded("", "", listOf("Guitarra", "Bajo")))
        vm.onTrackSelected(1)
        assertEquals(PlayerCommand.SelectTrack(1), vm.commands.first())
        assertEquals(1, vm.uiState.value.selectedTrack)
        assertTrue(vm.uiState.value.isRendering)
    }

    @Test
    fun `la velocidad se limita y se aplica al soltar`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        vm.onSpeedChanging(3f)
        assertEquals(TabViewerViewModel.MAX_SPEED, vm.uiState.value.speed)
        vm.onSpeedChanging(0.75f)
        vm.onSpeedChangeFinished()
        assertEquals(PlayerCommand.SetSpeed(0.75f), vm.commands.first())
    }

    @Test
    fun `un archivo que no es tablatura muestra error de formato`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.failure(TabFileException.UnsupportedFormat("foto.jpg"))))
        advanceUntilIdle()
        assertEquals(TabViewerError.Unsupported, vm.uiState.value.error)
    }

    @Test
    fun `una interrupcion de audio pausa solo si esta sonando`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onAudioInterrupted()
        vm.onStop()
        vm.onEngineEvent(AlphaTabEvent.PlayerStateChanged(isPlaying = true))
        vm.onAudioInterrupted()
        // La interrupción sin sonido no envía nada: solo llegan Detener y la pausa posterior.
        assertEquals(listOf(PlayerCommand.Stop, PlayerCommand.Pause), vm.commands.take(2).toList())
    }

    @Test
    fun `al abrir se guarda en la biblioteca`() = runTest(dispatcher) {
        val library = FakeLibrary()
        viewModel(FakeRepository(Result.success(gp5)), library)
        advanceUntilIdle()
        assertEquals(listOf(gp5), library.opened)
    }

    @Test
    fun `un archivo con permiso temporal se guarda como copia local`() = runTest(dispatcher) {
        val library = FakeLibrary()
        val shared = gp5.copy(hasLastingAccess = false)
        viewModel(FakeRepository(Result.success(shared)), library)
        advanceUntilIdle()
        assertEquals("file:///copia/Blues en La.gp5", library.opened.single().uri)
    }

    @Test
    fun `restaura pista, velocidad y posicion de la ultima vez`() = runTest(dispatcher) {
        val library = FakeLibrary(mapOf("content://1" to PracticeSettings(trackIndex = 1, speed = 0.75f, positionMs = 42_000)))
        val vm = viewModel(FakeRepository(Result.success(gp5)), library)
        advanceUntilIdle()
        assertEquals(0.75f, vm.uiState.value.speed)

        vm.onEngineEvent(AlphaTabEvent.ScoreLoaded("", "", listOf("Guitarra", "Bajo")))
        assertEquals(1, vm.uiState.value.selectedTrack)
        vm.onEngineEvent(AlphaTabEvent.PlayerReady)
        assertEquals(
            listOf(PlayerCommand.SelectTrack(1), PlayerCommand.SetSpeed(0.75f), PlayerCommand.Seek(42_000)),
            vm.commands.take(3).toList(),
        )
    }

    @Test
    fun `al pausar guarda los ajustes`() = runTest(dispatcher) {
        val library = FakeLibrary()
        val vm = viewModel(FakeRepository(Result.success(gp5)), library)
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.ScoreLoaded("", "", listOf("Guitarra", "Bajo")))
        vm.onEngineEvent(AlphaTabEvent.PlayerReady)
        vm.onSpeedChanging(0.5f)
        vm.onEngineEvent(AlphaTabEvent.PositionChanged(30_000, 120_000, 90))
        vm.onEngineEvent(AlphaTabEvent.PlayerStateChanged(isPlaying = false))
        advanceUntilIdle()
        assertEquals(PracticeSettings(trackIndex = 0, speed = 0.5f, positionMs = 30_000), library.settings["content://1"])
    }

    @Test
    fun `si la cancion termino la proxima vez empieza desde el principio`() = runTest(dispatcher) {
        val library = FakeLibrary()
        val vm = viewModel(FakeRepository(Result.success(gp5)), library)
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.PlayerReady)
        vm.onEngineEvent(AlphaTabEvent.PositionChanged(119_800, 120_000, 90))
        vm.onEngineEvent(AlphaTabEvent.PlayerStateChanged(isPlaying = false))
        advanceUntilIdle()
        assertEquals(0L, library.settings["content://1"]?.positionMs)
    }

    @Test
    fun `quitar de la biblioteca usa la uri abierta`() = runTest(dispatcher) {
        val library = FakeLibrary()
        val vm = viewModel(FakeRepository(Result.failure(TabFileException.Unreadable("x.gp5"))), library)
        advanceUntilIdle()
        vm.onRemoveFromLibrary()
        advanceUntilIdle()
        assertEquals(listOf("content://1"), library.removed)
    }

    @Test
    fun `el boton A-B marca A, luego B y despues quita el bucle`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onLoopRangeTapped()
        vm.onEngineEvent(AlphaTabEvent.LoopRangeChanged(startBar = 4, endBar = 0))
        assertEquals(LoopRangeState.START_MARKED, vm.uiState.value.loopRange)
        vm.onLoopRangeTapped()
        vm.onEngineEvent(AlphaTabEvent.LoopRangeChanged(startBar = 4, endBar = 8))
        assertEquals(LoopRangeState.ACTIVE, vm.uiState.value.loopRange)
        assertTrue(vm.uiState.value.isLooping)
        vm.onLoopRangeTapped()
        assertEquals(
            listOf(PlayerCommand.MarkLoopStart, PlayerCommand.MarkLoopEnd, PlayerCommand.ClearLoopRange),
            vm.commands.take(3).toList(),
        )
    }

    @Test
    fun `el entrenador sube un 5 por ciento por vuelta hasta el objetivo`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onSpeedChanging(0.9f)
        vm.onSpeedTrainerToggled()
        vm.onEngineEvent(AlphaTabEvent.LoopCompleted)
        assertEquals(0.95f, vm.uiState.value.speed)
        vm.onEngineEvent(AlphaTabEvent.LoopCompleted)
        vm.onEngineEvent(AlphaTabEvent.LoopCompleted)
        assertEquals(1f, vm.uiState.value.speed)
        assertEquals(listOf(PlayerCommand.SetSpeed(0.95f), PlayerCommand.SetSpeed(1f)), vm.commands.take(2).toList())
    }

    @Test
    fun `sin entrenador las vueltas no cambian la velocidad`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onSpeedChanging(0.5f)
        vm.onEngineEvent(AlphaTabEvent.LoopCompleted)
        assertEquals(0.5f, vm.uiState.value.speed)
    }

    @Test
    fun `el objetivo del entrenador recorre las opciones`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        assertEquals(1f, vm.uiState.value.speedTrainerTarget)
        vm.onSpeedTrainerTargetCycled()
        assertEquals(1.1f, vm.uiState.value.speedTrainerTarget)
        vm.onSpeedTrainerTargetCycled()
        vm.onSpeedTrainerTargetCycled()
        assertEquals(0.8f, vm.uiState.value.speedTrainerTarget)
    }

    @Test
    fun `la velocidad va en pasos del 5 por ciento`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        vm.onSpeedChanging(0.83f)
        assertEquals(0.85f, vm.uiState.value.speed)
    }

    @Test
    fun `cuenta previa y pedal envian sus ordenes`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        vm.onCountInToggled()
        assertTrue(vm.uiState.value.isCountInOn)
        vm.onPedal(PedalAction.NEXT_PAGE)
        vm.onPedal(PedalAction.PREVIOUS_PAGE)
        vm.onPedal(PedalAction.PLAY_PAUSE)
        assertEquals(
            listOf(
                PlayerCommand.SetCountIn(true),
                PlayerCommand.ScrollPage(1),
                PlayerCommand.ScrollPage(-1),
                PlayerCommand.PlayPause,
            ),
            vm.commands.take(4).toList(),
        )
    }

    @Test
    fun `formatTime muestra minutos y segundos`() {
        assertEquals("0:05", formatTime(5_000))
        assertEquals("2:03", formatTime(123_400))
    }
}
