package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.roundToInt

@Composable
fun TabViewerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TabViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onBackgrounded() }

    TabViewerScreen(
        uiState = uiState,
        commands = viewModel.commands,
        actions = TabViewerActions(
            onBack = onBack,
            onEngineEvent = viewModel::onEngineEvent,
            onPlayPause = viewModel::onPlayPause,
            onStop = viewModel::onStop,
            onTrackSelected = viewModel::onTrackSelected,
            onLoopToggled = viewModel::onLoopToggled,
            onMetronomeToggled = viewModel::onMetronomeToggled,
            onSpeedChanging = viewModel::onSpeedChanging,
            onSpeedChangeFinished = viewModel::onSpeedChangeFinished,
        ),
        modifier = modifier,
    )
}

/** Agrupa las lambdas en una clase estable para evitar recomposiciones al cambiar la posición. */
data class TabViewerActions(
    val onBack: () -> Unit = {},
    val onEngineEvent: (AlphaTabEvent) -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onTrackSelected: (Int) -> Unit = {},
    val onLoopToggled: () -> Unit = {},
    val onMetronomeToggled: () -> Unit = {},
    val onSpeedChanging: (Float) -> Unit = {},
    val onSpeedChangeFinished: () -> Unit = {},
)

/** Ancho del panel lateral con pistas y herramientas en horizontal. */
private val SidePanelWidth = 232.dp

@Composable
fun TabViewerScreen(
    uiState: TabViewerUiState,
    actions: TabViewerActions,
    modifier: Modifier = Modifier,
    commands: Flow<PlayerCommand> = emptyFlow(),
) {
    // movableContentOf conserva el WebView (y la reproducción) al cambiar entre
    // el diseño vertical y el horizontal, en lugar de destruirlo y recargar la partitura.
    val currentCommands by rememberUpdatedState(commands)
    val currentOnEngineEvent by rememberUpdatedState(actions.onEngineEvent)
    val sheet = remember {
        movableContentOf { state: TabViewerUiState, sheetModifier: Modifier ->
            ScoreSheet(state, currentCommands, { event -> currentOnEngineEvent(event) }, sheetModifier)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Header(uiState, actions.onBack)
                    sheet(
                        uiState,
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 10.dp, bottom = 10.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(SidePanelWidth)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.trackNames.size > 1) {
                        TrackChips(uiState.trackNames, uiState.selectedTrack, actions.onTrackSelected, vertical = true)
                    }
                    PracticeTools(uiState, actions, compact = true)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Header(uiState, actions.onBack)
                if (uiState.trackNames.size > 1) {
                    TrackChips(uiState.trackNames, uiState.selectedTrack, actions.onTrackSelected, vertical = false)
                }
                sheet(
                    uiState,
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                )
                PracticeTools(uiState, actions, compact = false)
            }
        }
    }
}

@Composable
private fun ScoreSheet(
    uiState: TabViewerUiState,
    commands: Flow<PlayerCommand>,
    onEngineEvent: (AlphaTabEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            val error = uiState.error
            val score = uiState.score
            if (score != null && error !is TabViewerError.Engine) {
                TabViewerWebView(
                    score = score,
                    commands = commands,
                    onEvent = onEngineEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            when {
                error != null -> ErrorMessage(error, Modifier.align(Alignment.Center))
                uiState.isRendering -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Header(uiState: TabViewerUiState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.tab_viewer_back)) }
        Column(Modifier.weight(1f)) {
            Text(
                text = uiState.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(
                uiState.artist.takeIf { it.isNotBlank() },
                if (uiState.durationMs > 0) "${formatTime(uiState.positionMs)} / ${formatTime(uiState.durationMs)}" else null,
                if (uiState.tempo > 0) "${uiState.tempo} BPM" else null,
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TrackChips(names: List<String>, selected: Int, onSelected: (Int) -> Unit, vertical: Boolean) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer,
    )
    val chips: @Composable () -> Unit = {
        names.forEachIndexed { index, name ->
            val (strong, soft) = palette[index % palette.size]
            val isSelected = index == selected
            Surface(
                onClick = { onSelected(index) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) strong else soft,
                contentColor = if (isSelected) Color.White else strong,
                modifier = if (vertical) Modifier.fillMaxWidth() else Modifier,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.tab_viewer_tracks),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            chips()
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { chips() }
    }
}

/**
 * @param compact en el panel lateral horizontal: herramientas en cuadrícula de 2 × 2 y
 * velocidad debajo de su etiqueta.
 */
@Composable
private fun PracticeTools(uiState: TabViewerUiState, actions: TabViewerActions, compact: Boolean) {
    val enabled = uiState.isPlayerReady && uiState.error == null
    val playLabel = stringResource(if (uiState.isPlaying) R.string.tab_viewer_pause else R.string.tab_viewer_play)
    val stopLabel = stringResource(R.string.tab_viewer_stop)
    val loopLabel = stringResource(R.string.tab_viewer_loop)
    val metronomeLabel = stringResource(R.string.tab_viewer_metronome)

    Column(
        modifier = if (compact) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolPill(playLabel, true, enabled, actions.onPlayPause, Modifier.weight(1f))
                ToolPill(stopLabel, false, enabled, actions.onStop, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolPill(loopLabel, uiState.isLooping, enabled, actions.onLoopToggled, Modifier.weight(1f))
                ToolPill(metronomeLabel, uiState.isMetronomeOn, enabled, actions.onMetronomeToggled, Modifier.weight(1f))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolPill(playLabel, true, enabled, actions.onPlayPause, Modifier.weight(1f))
                ToolPill(stopLabel, false, enabled, actions.onStop, Modifier.weight(1f))
                ToolPill(loopLabel, uiState.isLooping, enabled, actions.onLoopToggled, Modifier.weight(1f))
                ToolPill(metronomeLabel, uiState.isMetronomeOn, enabled, actions.onMetronomeToggled, Modifier.weight(1.3f))
            }
        }
        SpeedControl(uiState.speed, enabled, actions, stacked = compact)
    }
}

@Composable
private fun SpeedControl(speed: Float, enabled: Boolean, actions: TabViewerActions, stacked: Boolean) {
    val label = stringResource(R.string.tab_viewer_speed)
    val value = "${(speed * 100).roundToInt()} %"
    val slider: @Composable (Modifier) -> Unit = { sliderModifier ->
        Slider(
            value = speed,
            onValueChange = actions.onSpeedChanging,
            onValueChangeFinished = actions.onSpeedChangeFinished,
            valueRange = TabViewerViewModel.MIN_SPEED..TabViewerViewModel.MAX_SPEED,
            steps = 4,
            enabled = enabled,
            modifier = sliderModifier,
        )
    }
    if (stacked) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.labelMedium)
            }
            slider(Modifier.fillMaxWidth())
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            slider(Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(44.dp),
            )
        }
    }
}

@Composable
private fun ToolPill(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val content = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) container else container.copy(alpha = 0.4f),
        contentColor = content,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun ErrorMessage(error: TabViewerError, modifier: Modifier = Modifier) {
    val text = when (error) {
        TabViewerError.FileUnreadable -> stringResource(R.string.tab_viewer_error_unreadable)
        TabViewerError.NotInteractive -> stringResource(R.string.tab_viewer_error_not_interactive)
        is TabViewerError.Engine -> stringResource(R.string.tab_viewer_error_engine)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    )
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
