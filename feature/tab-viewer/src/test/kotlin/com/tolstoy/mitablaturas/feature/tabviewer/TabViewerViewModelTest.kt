package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.lifecycle.SavedStateHandle
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.data.preferences.UserPreferencesRepository
import com.tolstoy.mitablaturas.core.model.AppDesign
import com.tolstoy.mitablaturas.core.designsystem.PedalAction
import com.tolstoy.mitablaturas.core.model.LibraryEntry
import com.tolstoy.mitablaturas.core.model.LibrarySort
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.ScoreSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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
        val scoreInfo = mutableMapOf<String, Pair<String, String>>()
        override suspend fun saveScoreInfo(uri: String, title: String, artist: String) {
            scoreInfo[uri] = title to artist
        }
        override suspend fun savePdfPage(uri: String, page: Int) = Unit
    }

    private class FakePreferences(scale: Float = 0.9f, tabOnly: Boolean = false) : UserPreferencesRepository {
        override val design = MutableStateFlow(AppDesign.DEFAULT)
        override suspend fun setDesign(design: AppDesign) {
            this.design.value = design
        }
        override val scoreScale = MutableStateFlow(scale)
        override suspend fun setScoreScale(scale: Float) {
            scoreScale.value = scale
        }
        override val tabOnly = MutableStateFlow(tabOnly)
        override suspend fun setTabOnly(enabled: Boolean) {
            tabOnly.value = enabled
        }
        override val librarySort = MutableStateFlow(LibrarySort.DEFAULT)
        override suspend fun setLibrarySort(sort: LibrarySort) {
            librarySort.value = sort
        }
    }

    private val gp5 = TabDocument("content://1", "Blues en La.gp5", TabFormat.GUITAR_PRO_5, 3)

    private fun TestScope.viewModel(
        repository: TabFileRepository,
        library: LibraryRepository = FakeLibrary(),
        preferences: UserPreferencesRepository = FakePreferences(),
    ) = TabViewerViewModel(
        savedStateHandle = SavedStateHandle(mapOf(TabViewerViewModel.ARG_URI to "content://1")),
        repository = repository,
        library = library,
        preferences = preferences,
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
    fun `pantalla completa se activa y se desactiva`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isFocusMode)
        vm.onFocusModeToggled()
        assertTrue(vm.uiState.value.isFocusMode)
        vm.onFocusModeToggled()
        assertFalse(vm.uiState.value.isFocusMode)
    }

    @Test
    fun `abre con el tamano guardado`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)), preferences = FakePreferences(scale = 1.3f))
        advanceUntilIdle()
        assertEquals(1.3f, vm.uiState.value.scoreScale)
    }

    @Test
    fun `varios toques de tamano se dibujan una sola vez y se guardan`() = runTest(dispatcher) {
        val preferences = FakePreferences()
        val vm = viewModel(FakeRepository(Result.success(gp5)), preferences = preferences)
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.RenderFinished)
        assertTrue(vm.uiState.value.canRedraw)

        vm.onZoom(1)
        vm.onZoom(1)
        vm.onZoom(1)
        // El número cambia al instante; el dibujo espera a que se deje de pulsar.
        assertEquals(1.2f, vm.uiState.value.scoreScale)
        assertFalse(vm.uiState.value.isRendering)
        advanceTimeBy(TabViewerViewModel.ZOOM_SETTLE_MS + 1)
        assertTrue(vm.uiState.value.isRendering)
        assertFalse(vm.uiState.value.canRedraw)
        advanceUntilIdle()
        assertEquals(1.2f, preferences.scoreScale.value)

        // Tras la orden de tamaño solo queda la siguiente: no hubo un dibujo por cada toque.
        vm.onStop()
        assertEquals(listOf(PlayerCommand.SetScale(1.2f), PlayerCommand.Stop), vm.commands.take(2).toList())
    }

    @Test
    fun `el tamano no pasa del maximo`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)), preferences = FakePreferences(scale = 2.0f))
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.RenderFinished)
        vm.onZoom(1)
        advanceUntilIdle()
        assertEquals(2.0f, vm.uiState.value.scoreScale)
        assertFalse(vm.uiState.value.isRendering)
        vm.onStop()
        assertEquals(PlayerCommand.Stop, vm.commands.first())
    }

    @Test
    fun `solo tablatura vuelve a dibujar y se guarda`() = runTest(dispatcher) {
        val preferences = FakePreferences()
        val vm = viewModel(FakeRepository(Result.success(gp5)), preferences = preferences)
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.RenderFinished)
        vm.onTabOnlyToggled()
        assertTrue(vm.uiState.value.isTabOnly)
        assertTrue(vm.uiState.value.isRendering)
        assertFalse(vm.uiState.value.canRedraw)
        assertEquals(PlayerCommand.SetTabOnly(true), vm.commands.first())
        advanceUntilIdle()
        assertTrue(preferences.tabOnly.value)
    }

    @Test
    fun `guarda el titulo y el artista de la partitura en la biblioteca`() = runTest(dispatcher) {
        val library = FakeLibrary()
        val vm = viewModel(FakeRepository(Result.success(gp5)), library = library)
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.ScoreLoaded("Blues en La", "Elías", listOf("Guitarra")))
        advanceUntilIdle()
        assertEquals("Blues en La" to "Elías", library.scoreInfo["content://1"])
    }

    @Test
    fun `abre con la vista guardada`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)), preferences = FakePreferences(tabOnly = true))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isTabOnly)
    }

    @Test
    fun `la mezcla de pistas envia solo lo que cambia`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onEngineEvent(AlphaTabEvent.ScoreLoaded("Blues en La", "Elias", listOf("Guitarra", "Bajo")))
        assertEquals(listOf(TrackMix(), TrackMix()), vm.uiState.value.trackMix)

        vm.onTrackMuteToggled(0)
        vm.onTrackSoloToggled(1)
        vm.onTrackVolumeChanged(1, 0.52f)
        // Redondeado a pasos del 5 %, 0,49 es el mismo 50 %: no se envía otra vez.
        vm.onTrackVolumeChanged(1, 0.49f)
        vm.onTrackMuteToggled(5)

        assertEquals(TrackMix(isMuted = true), vm.uiState.value.trackMix[0])
        assertEquals(TrackMix(isSolo = true, volume = 0.5f), vm.uiState.value.trackMix[1])
        vm.onStop()
        assertEquals(
            listOf(
                PlayerCommand.SetTrackMute(0, true),
                PlayerCommand.SetTrackSolo(1, true),
                PlayerCommand.SetTrackVolume(1, 0.5f),
                PlayerCommand.Stop,
            ),
            vm.commands.take(4).toList(),
        )
    }

    @Test
    fun `volumen del metronomo en pasos y transposicion con limite`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        vm.onMetronomeVolumeChanged(0.33f)
        repeat(14) { vm.onTranspositionChanged(-1) }

        assertEquals(0.35f, vm.uiState.value.metronomeVolume)
        assertEquals(TabViewerViewModel.MIN_TRANSPOSITION, vm.uiState.value.transposition)
        val commands = vm.commands.take(13).toList()
        assertEquals(PlayerCommand.SetMetronomeVolume(0.35f), commands.first())
        assertEquals(PlayerCommand.SetTransposition(-12), commands.last())
    }

    @Test
    fun `una seccion se salta con un toque y se repite con una pulsacion larga`() = runTest(dispatcher) {
        val vm = viewModel(FakeRepository(Result.success(gp5)))
        advanceUntilIdle()
        val sections = listOf(ScoreSection("Intro", 1, 8), ScoreSection("Estribillo", 9, 16))
        vm.onEngineEvent(AlphaTabEvent.SectionsLoaded(sections))
        assertEquals(sections, vm.uiState.value.sections)

        vm.onSectionTapped(1)
        assertTrue(vm.uiState.value.showSectionHint)
        vm.onSectionLongPressed(0)
        assertFalse(vm.uiState.value.showSectionHint)
        vm.onEngineEvent(AlphaTabEvent.LoopRangeChanged(1, 8))
        // Con el bucle activo, un toque pasa a repetir esa sección en lugar de salir del bucle.
        vm.onSectionTapped(1)

        assertEquals(
            listOf(PlayerCommand.JumpToBar(9), PlayerCommand.LoopBars(1, 8), PlayerCommand.LoopBars(9, 16)),
            vm.commands.take(3).toList(),
        )
    }

    @Test
    fun `formatTime muestra minutos y segundos`() {
        assertEquals("0:05", formatTime(5_000))
        assertEquals("2:03", formatTime(123_400))
    }
}
