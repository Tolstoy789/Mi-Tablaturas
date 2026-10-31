package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.lifecycle.SavedStateHandle
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.core.model.TabFormat
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
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
    }

    private val gp5 = TabDocument("content://1", "Blues en La.gp5", TabFormat.GUITAR_PRO_5, 3)

    private fun viewModel(repository: TabFileRepository) =
        TabViewerViewModel(SavedStateHandle(mapOf(TabViewerViewModel.ARG_URI to "content://1")), repository)

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
    fun `un pdf no se abre en el visor interactivo`() = runTest(dispatcher) {
        val pdf = gp5.copy(displayName = "cancionero.pdf", format = TabFormat.PDF)
        val vm = viewModel(FakeRepository(Result.success(pdf)))
        advanceUntilIdle()
        assertEquals(TabViewerError.NotInteractive, vm.uiState.value.error)
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
    fun `formatTime muestra minutos y segundos`() {
        assertEquals("0:05", formatTime(5_000))
        assertEquals("2:03", formatTime(123_400))
    }
}
