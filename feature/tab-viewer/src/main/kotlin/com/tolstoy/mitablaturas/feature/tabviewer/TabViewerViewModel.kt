package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.di.ApplicationScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.data.library.LibraryRepository
import com.tolstoy.mitablaturas.core.model.PracticeSettings
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TabFileRepository,
    private val library: LibraryRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val uri: String = checkNotNull(savedStateHandle[ARG_URI]) { "Falta el argumento $ARG_URI" }

    private val _uiState = MutableStateFlow(TabViewerUiState())
    val uiState: StateFlow<TabViewerUiState> = _uiState.asStateFlow()

    private val _commands = Channel<PlayerCommand>(Channel.BUFFERED)
    /** Órdenes para el WebView; las consume `TabViewerWebView`. */
    val commands: Flow<PlayerCommand> = _commands.receiveAsFlow()

    /** URI con la que la canción está en la biblioteca (la copia local si llegó con permiso temporal). */
    @Volatile private var libraryUri: String? = null

    /** Ajustes guardados que faltan por aplicar cuando AlphaTab esté listo. */
    @Volatile private var pendingRestore: PracticeSettings? = null

    init {
        loadScore()
    }

    private fun loadScore() {
        viewModelScope.launch {
            val document = repository.resolve(uri).getOrElse { error ->
                val viewerError = if (error is TabFileException.UnsupportedFormat) {
                    TabViewerError.Unsupported
                } else {
                    TabViewerError.FileUnreadable
                }
                _uiState.update { s -> s.copy(error = viewerError, isRendering = false) }
                return@launch
            }
            _uiState.update { it.copy(fileName = document.displayName) }
            if (!document.format.isInteractive) {
                _uiState.update { it.copy(error = TabViewerError.NotInteractive, isRendering = false) }
                return@launch
            }
            val bytes = repository.readBytes(document).getOrElse {
                _uiState.update { it.copy(error = TabViewerError.FileUnreadable, isRendering = false) }
                return@launch
            }
            val saved = rememberInLibrary(document, bytes)
            pendingRestore = saved
            _uiState.update { it.copy(score = ScoreBytes(bytes), speed = saved?.speed ?: it.speed) }
        }
    }

    /** Añade la canción a la biblioteca y devuelve sus ajustes de la última vez, si los hay. */
    private suspend fun rememberInLibrary(document: TabDocument, bytes: ByteArray): PracticeSettings? {
        val stored = if (document.hasLastingAccess) document else repository.importCopy(document, bytes).getOrDefault(document)
        libraryUri = stored.uri
        val saved = library.practiceSettings(stored.uri)
        library.recordOpened(stored)
        return saved
    }

    /** Llamado desde el hilo del puente JS: `update` es seguro entre hilos. */
    fun onEngineEvent(event: AlphaTabEvent) {
        _uiState.update { state ->
            when (event) {
                is AlphaTabEvent.ScoreLoaded -> state.copy(
                    title = event.title,
                    artist = event.artist,
                    trackNames = event.trackNames,
                    selectedTrack = 0,
                )
                AlphaTabEvent.RenderFinished -> state.copy(isRendering = false)
                AlphaTabEvent.PlayerReady -> state.copy(isPlayerReady = true)
                is AlphaTabEvent.PlayerStateChanged -> state.copy(isPlaying = event.isPlaying)
                is AlphaTabEvent.PositionChanged -> state.copy(
                    positionMs = event.currentMs,
                    durationMs = event.endMs,
                    tempo = event.tempo,
                )
                is AlphaTabEvent.Error -> state.copy(
                    error = TabViewerError.Engine(event.message),
                    isRendering = false,
                )
            }
        }
        when (event) {
            is AlphaTabEvent.ScoreLoaded -> restoreTrack(event.trackNames.size)
            AlphaTabEvent.PlayerReady -> restorePlayback()
            is AlphaTabEvent.PlayerStateChanged -> if (!event.isPlaying) savePractice()
            else -> Unit
        }
    }

    private fun restoreTrack(trackCount: Int) {
        val track = pendingRestore?.trackIndex ?: return
        if (track in 1 until trackCount) onTrackSelected(track)
    }

    private fun restorePlayback() {
        val saved = pendingRestore ?: return
        pendingRestore = null
        if (saved.speed != 1f) send(PlayerCommand.SetSpeed(saved.speed))
        if (saved.positionMs > 0) send(PlayerCommand.Seek(saved.positionMs))
    }

    /** Guarda pista, velocidad y posición; usa el ámbito de la app para terminar aunque se cierre el visor. */
    private fun savePractice() {
        val target = libraryUri ?: return
        val state = _uiState.value
        // Si la canción terminó, la próxima vez empieza desde el principio.
        val atEnd = state.durationMs > 0 && state.positionMs >= state.durationMs - END_MARGIN_MS
        // Mientras no se haya restaurado la posición guardada, se conserva en lugar de pisarla con 0.
        val position = pendingRestore?.positionMs ?: if (atEnd) 0 else state.positionMs
        val settings = PracticeSettings(trackIndex = state.selectedTrack, speed = state.speed, positionMs = position)
        appScope.launch { library.savePracticeSettings(target, settings) }
    }

    /** El archivo ya no se puede leer: se quita de la biblioteca. */
    fun onRemoveFromLibrary() {
        val target = libraryUri ?: uri
        appScope.launch { library.remove(target) }
    }

    override fun onCleared() {
        savePractice()
    }

    fun onPlayPause() = send(PlayerCommand.PlayPause)

    fun onStop() = send(PlayerCommand.Stop)

    /** La app pasa a segundo plano: se pausa para no seguir sonando y se guardan los ajustes. */
    fun onBackgrounded() {
        pauseIfPlaying()
        savePractice()
    }

    /** Entra una llamada, suena otra app o se desconectan los auriculares. */
    fun onAudioInterrupted() = pauseIfPlaying()

    private fun pauseIfPlaying() {
        if (_uiState.value.isPlaying) send(PlayerCommand.Pause)
    }

    fun onTrackSelected(index: Int) {
        val state = _uiState.value
        if (index !in state.trackNames.indices || index == state.selectedTrack) return
        _uiState.update { it.copy(selectedTrack = index, isRendering = true) }
        send(PlayerCommand.SelectTrack(index))
        savePractice()
    }

    fun onLoopToggled() {
        val enabled = !_uiState.value.isLooping
        _uiState.update { it.copy(isLooping = enabled) }
        send(PlayerCommand.SetLooping(enabled))
    }

    fun onMetronomeToggled() {
        val enabled = !_uiState.value.isMetronomeOn
        _uiState.update { it.copy(isMetronomeOn = enabled) }
        send(PlayerCommand.SetMetronome(enabled))
    }

    /** Actualiza el valor mostrado mientras se arrastra el control. */
    fun onSpeedChanging(speed: Float) {
        _uiState.update { it.copy(speed = speed.coerceIn(MIN_SPEED, MAX_SPEED)) }
    }

    /** Aplica la velocidad al soltar el control, para no saturar el puente. */
    fun onSpeedChangeFinished() {
        send(PlayerCommand.SetSpeed(_uiState.value.speed))
        savePractice()
    }

    private fun send(command: PlayerCommand) {
        _commands.trySend(command)
    }

    companion object {
        const val ARG_URI = "uri"
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 1.5f
        private const val END_MARGIN_MS = 1_000L
    }
}
