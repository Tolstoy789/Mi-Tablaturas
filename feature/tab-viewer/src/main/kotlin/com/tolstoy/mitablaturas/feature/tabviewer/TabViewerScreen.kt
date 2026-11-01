package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolstoy.mitablaturas.core.designsystem.KeepScreenOnEffect
import com.tolstoy.mitablaturas.core.designsystem.LocalScoreSheetStyle
import com.tolstoy.mitablaturas.core.designsystem.pedalInput
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.AlphaTabEvent
import com.tolstoy.mitablaturas.feature.tabviewer.bridge.PlayerCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.roundToInt

@Composable
fun TabViewerRoute(
    onBack: () -> Unit,
    onOpenPdf: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TabViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pdfUri = uiState.openAsPdf
    LaunchedEffect(pdfUri) { if (pdfUri != null) onOpenPdf(pdfUri) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onBackgrounded() }
    KeepScreenOnEffect()
    PlaybackInterruptionsEffect(isPlaying = uiState.isPlaying, onInterrupted = viewModel::onAudioInterrupted)
    // En pantalla completa, «atrás» vuelve al visor normal en lugar de cerrar la canción.
    BackHandler(enabled = uiState.isFocusMode, onBack = viewModel::onFocusModeToggled)
    if (uiState.isFocusMode) HideSystemBarsEffect()

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
            onRemoveFromLibrary = {
                viewModel.onRemoveFromLibrary()
                onBack()
            },
            onLoopRangeTapped = viewModel::onLoopRangeTapped,
            onCountInToggled = viewModel::onCountInToggled,
            onSpeedTrainerToggled = viewModel::onSpeedTrainerToggled,
            onSpeedTrainerTargetCycled = viewModel::onSpeedTrainerTargetCycled,
            onFocusModeToggled = viewModel::onFocusModeToggled,
            onZoom = viewModel::onZoom,
        ),
        modifier = modifier.pedalInput(viewModel::onPedal),
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
    val onRemoveFromLibrary: () -> Unit = {},
    val onLoopRangeTapped: () -> Unit = {},
    val onCountInToggled: () -> Unit = {},
    val onSpeedTrainerToggled: () -> Unit = {},
    val onSpeedTrainerTargetCycled: () -> Unit = {},
    val onFocusModeToggled: () -> Unit = {},
    /** Pasos de tamaño de la partitura: +1 agranda, -1 reduce. */
    val onZoom: (Int) -> Unit = {},
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
    val currentOnRemove by rememberUpdatedState(actions.onRemoveFromLibrary)
    val sheet = remember {
        movableContentOf { state: TabViewerUiState, sheetModifier: Modifier ->
            ScoreSheet(
                uiState = state,
                commands = currentCommands,
                onEngineEvent = { event -> currentOnEngineEvent(event) },
                onRemoveFromLibrary = { currentOnRemove() },
                modifier = sheetModifier,
            )
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        if (uiState.isFocusMode) {
            // El fondo de la hoja detrás tapa las esquinas redondeadas al ocupar toda la pantalla.
            Box(Modifier.fillMaxSize().background(LocalScoreSheetStyle.current.background)) {
                sheet(uiState, Modifier.fillMaxSize())
                FocusBar(
                    uiState = uiState,
                    actions = actions,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                )
            }
        } else if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Header(uiState, actions.onBack, actions.onFocusModeToggled)
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
                Header(uiState, actions.onBack, actions.onFocusModeToggled)
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
    onRemoveFromLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetStyle = LocalScoreSheetStyle.current
    Surface(shape = MaterialTheme.shapes.large, color = sheetStyle.background, modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            val error = uiState.error
            val score = uiState.score
            if (score != null && error !is TabViewerError.Engine) {
                TabViewerWebView(
                    score = score,
                    sheetStyle = sheetStyle,
                    commands = commands,
                    onEvent = onEngineEvent,
                    modifier = Modifier.fillMaxSize(),
                    initialScale = uiState.scoreScale,
                )
            }
            when {
                error != null -> ErrorMessage(error, onRemoveFromLibrary, Modifier.align(Alignment.Center))
                uiState.isRendering -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Header(uiState: TabViewerUiState, onBack: () -> Unit, onFocusMode: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp),
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
        val focusDescription = stringResource(R.string.tab_viewer_focus_enter)
        IconButton(onClick = onFocusMode, modifier = Modifier.semantics { contentDescription = focusDescription }) {
            FullscreenIcon(expand = true)
        }
    }
}

@Composable
private fun TrackChips(names: List<String>, selected: Int, onSelected: (Int) -> Unit, vertical: Boolean) {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        Triple(colors.primary, colors.onPrimary, colors.primaryContainer),
        Triple(colors.secondary, colors.onSecondary, colors.secondaryContainer),
        Triple(colors.tertiary, colors.onTertiary, colors.tertiaryContainer),
    )
    val chips: @Composable () -> Unit = {
        names.forEachIndexed { index, name ->
            val (strong, onStrong, soft) = palette[index % palette.size]
            val isSelected = index == selected
            Surface(
                onClick = { onSelected(index) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) strong else soft,
                contentColor = if (isSelected) onStrong else strong,
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
    val playDescription = stringResource(
        if (uiState.isPlaying) R.string.tab_viewer_pause_description else R.string.tab_viewer_play_description,
    )
    val stopLabel = stringResource(R.string.tab_viewer_stop)
    val loopLabel = stringResource(R.string.tab_viewer_loop)
    val metronomeLabel = stringResource(R.string.tab_viewer_metronome)
    val countInLabel = stringResource(R.string.tab_viewer_count_in)
    val trainerLabel = stringResource(R.string.tab_viewer_speed_trainer)

    Column(
        modifier = if (compact) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val play: @Composable (Modifier) -> Unit = {
            ActionPill(playLabel, enabled, actions.onPlayPause, it, emphasized = true, description = playDescription)
        }
        val stop: @Composable (Modifier) -> Unit = { ActionPill(stopLabel, enabled, actions.onStop, it) }
        val loop: @Composable (Modifier) -> Unit = {
            TogglePill(loopLabel, uiState.isLooping, enabled, { actions.onLoopToggled() }, it)
        }
        val metronome: @Composable (Modifier) -> Unit = {
            TogglePill(metronomeLabel, uiState.isMetronomeOn, enabled, { actions.onMetronomeToggled() }, it)
        }
        val loopRange: @Composable (Modifier) -> Unit = { LoopRangePill(uiState, enabled, actions.onLoopRangeTapped, it) }
        val countIn: @Composable (Modifier) -> Unit = {
            TogglePill(countInLabel, uiState.isCountInOn, enabled, { actions.onCountInToggled() }, it)
        }
        val trainer: @Composable (Modifier) -> Unit = {
            TogglePill(trainerLabel, uiState.isSpeedTrainerOn, enabled, { actions.onSpeedTrainerToggled() }, it)
        }
        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                play(Modifier.weight(1f))
                stop(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                loop(Modifier.weight(1f))
                metronome(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                loopRange(Modifier.weight(1f))
                countIn(Modifier.weight(1f))
            }
            trainer(Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                play(Modifier.weight(1f))
                stop(Modifier.weight(1f))
                loop(Modifier.weight(1f))
                metronome(Modifier.weight(1.3f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                loopRange(Modifier.weight(1.2f))
                countIn(Modifier.weight(1.1f))
                trainer(Modifier.weight(1.1f))
            }
        }
        PracticeHints(uiState, actions.onSpeedTrainerTargetCycled)
        SpeedControl(uiState.speed, enabled, actions, stacked = compact)
    }
}

/** Botón del bucle A-B: marca A, luego B, y con el bucle activo lo quita. */
@Composable
private fun LoopRangePill(uiState: TabViewerUiState, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (label, description) = when (uiState.loopRange) {
        LoopRangeState.NONE -> stringResource(R.string.tab_viewer_loop_mark_a) to
            stringResource(R.string.tab_viewer_loop_mark_a_description)
        LoopRangeState.START_MARKED -> stringResource(R.string.tab_viewer_loop_mark_b) to
            stringResource(R.string.tab_viewer_loop_mark_b_description, uiState.loopStartBar)
        LoopRangeState.ACTIVE -> stringResource(R.string.tab_viewer_loop_range, uiState.loopStartBar, uiState.loopEndBar) to
            stringResource(R.string.tab_viewer_loop_range_description, uiState.loopStartBar, uiState.loopEndBar)
    }
    ActionPill(
        label = label,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        emphasized = uiState.loopRange == LoopRangeState.ACTIVE,
        description = description,
    )
}

/** Indicaciones bajo los botones: cómo cerrar el bucle y hasta dónde sube el entrenador. */
@Composable
private fun PracticeHints(uiState: TabViewerUiState, onTargetCycled: () -> Unit) {
    if (uiState.loopRange == LoopRangeState.START_MARKED) {
        Text(
            text = stringResource(R.string.tab_viewer_loop_hint, uiState.loopStartBar),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (uiState.isSpeedTrainerOn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.tab_viewer_speed_trainer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(onClick = onTargetCycled) {
                Text(stringResource(R.string.tab_viewer_speed_trainer_target, (uiState.speedTrainerTarget * 100).roundToInt()))
            }
        }
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

/** Altura mínima de los botones de práctica: objetivo táctil de 48 dp, cómodo con la guitarra en brazos. */
private val PillMinHeight = 48.dp

/** Botón de acción (reproducir, detener). [description] sustituye a [label] en TalkBack si los símbolos no se leen bien. */
@Composable
private fun ActionPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    description: String = label,
) {
    val colors = pillColors(active = emphasized, enabled = enabled)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = colors.first,
        contentColor = colors.second,
        modifier = modifier
            .heightIn(min = PillMinHeight)
            .semantics { contentDescription = description },
    ) {
        PillLabel(label)
    }
}

/** Interruptor (repetir, metrónomo): TalkBack anuncia si está activado. */
@Composable
private fun TogglePill(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = pillColors(active = checked, enabled = enabled)
    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = colors.first,
        contentColor = colors.second,
        modifier = modifier.heightIn(min = PillMinHeight),
    ) {
        PillLabel(label)
    }
}

@Composable
private fun pillColors(active: Boolean, enabled: Boolean): Pair<Color, Color> {
    val container = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val content = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    return (if (enabled) container else container.copy(alpha = 0.4f)) to content
}

@Composable
private fun PillLabel(label: String) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun ErrorMessage(error: TabViewerError, onRemoveFromLibrary: () -> Unit, modifier: Modifier = Modifier) {
    val text = when (error) {
        TabViewerError.FileUnreadable -> stringResource(R.string.tab_viewer_error_unreadable)
        TabViewerError.Unsupported -> stringResource(R.string.tab_viewer_error_unsupported)
        is TabViewerError.Engine -> stringResource(R.string.tab_viewer_error_engine)
    }
    Column(
        modifier = modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        // Un archivo borrado o sin permiso no volverá a abrirse: se ofrece quitarlo de la lista.
        if (error == TabViewerError.FileUnreadable) {
            TextButton(onClick = onRemoveFromLibrary) {
                Text(stringResource(R.string.tab_viewer_remove_from_library))
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
