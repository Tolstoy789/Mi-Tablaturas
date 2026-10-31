package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolstoy.mitablaturas.core.data.file.TabFileRepository
import com.tolstoy.mitablaturas.core.model.TabFileException
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    private val uri: String = checkNotNull(savedStateHandle[ARG_URI]) { "Falta el argumento $ARG_URI" }

    private val _uiState = MutableStateFlow(TabViewerUiState())
    val uiState: StateFlow<TabViewerUiState> = _uiState.asStateFlow()

    private val _commands = Channel<PlayerCommand>(Channel.BUFFERED)
    /** Órdenes para el WebView; las consume `TabViewerWebView`. */
    val commands: Flow<PlayerCommand> = _commands.receiveAsFlow()

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
            repository.readBytes(document).fold(
                onSuccess = { bytes -> _uiState.update { it.copy(score = ScoreBytes(bytes)) } },
                onFailure = { _uiState.update { it.copy(error = TabViewerError.FileUnreadable, isRendering = false) } },
            )
        }
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
    }

    fun onPlayPause() = send(PlayerCommand.PlayPause)

    fun onStop() = send(PlayerCommand.Stop)

    /** La app pasa a segundo plano: se pausa para no seguir sonando. */
    fun onBackgrounded() = pauseIfPlaying()

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
    fun onSpeedChangeFinished() = send(PlayerCommand.SetSpeed(_uiState.value.speed))

    private fun send(command: PlayerCommand) {
        _commands.trySend(command)
    }

    companion object {
        const val ARG_URI = "uri"
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 1.5f
    }
}
