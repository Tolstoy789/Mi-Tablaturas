package com.tolstoy.mitablaturas.feature.tabviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Hoja «Sonido»: silencio, solo y volumen de cada pista (para tocar encima o aprender una parte),
 * volumen del metrónomo y transposición. Los cambios se oyen al momento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoundSheet(uiState: TabViewerUiState, actions: TabViewerActions, onDismiss: () -> Unit) {
    val enabled = uiState.canControlPlayer
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tab_viewer_sound),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.tab_viewer_sound_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.trackNames.forEachIndexed { index, name ->
                TrackMixRow(name, uiState.trackMix.getOrNull(index) ?: TrackMix(), enabled, index, actions)
            }
            HorizontalDivider()
            Text(stringResource(R.string.tab_viewer_metronome), style = MaterialTheme.typography.titleSmall)
            VolumeSlider(
                value = uiState.metronomeVolume,
                enabled = enabled,
                description = stringResource(R.string.tab_viewer_metronome_volume),
                onChange = actions.onMetronomeVolumeChanged,
            )
            HorizontalDivider()
            TranspositionRow(uiState.transposition, enabled, actions.onTranspositionChanged)
        }
    }
}

@Composable
private fun TrackMixRow(name: String, mix: TrackMix, enabled: Boolean, index: Int, actions: TabViewerActions) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TogglePill(
                label = stringResource(R.string.tab_viewer_mute),
                checked = mix.isMuted,
                enabled = enabled,
                onCheckedChange = { actions.onTrackMuteToggled(index) },
                modifier = Modifier.width(96.dp),
            )
            TogglePill(
                label = stringResource(R.string.tab_viewer_solo),
                checked = mix.isSolo,
                enabled = enabled,
                onCheckedChange = { actions.onTrackSoloToggled(index) },
                modifier = Modifier.width(72.dp),
            )
        }
        VolumeSlider(
            value = mix.volume,
            enabled = enabled && !mix.isMuted,
            description = stringResource(R.string.tab_viewer_track_volume, name),
            onChange = { actions.onTrackVolumeChanged(index, it) },
        )
    }
}

@Composable
private fun VolumeSlider(value: Float, enabled: Boolean, description: String, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = description },
        )
        Text(
            text = stringResource(R.string.tab_viewer_percent, (value * 100).roundToInt()),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

/** Semitonos con − y +; 0 se muestra como «Original». */
@Composable
private fun TranspositionRow(semitones: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.tab_viewer_transposition),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            ActionPill(
                label = stringResource(R.string.tab_viewer_minus_symbol),
                enabled = enabled && semitones > TabViewerViewModel.MIN_TRANSPOSITION,
                onClick = { onChange(-1) },
                modifier = Modifier.width(56.dp),
                description = stringResource(R.string.tab_viewer_transposition_down),
            )
            Text(
                text = if (semitones == 0) stringResource(R.string.tab_viewer_transposition_original) else signed(semitones),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(72.dp),
            )
            ActionPill(
                label = stringResource(R.string.tab_viewer_plus_symbol),
                enabled = enabled && semitones < TabViewerViewModel.MAX_TRANSPOSITION,
                onClick = { onChange(1) },
                modifier = Modifier.width(56.dp),
                description = stringResource(R.string.tab_viewer_transposition_up),
            )
        }
        Text(
            text = stringResource(R.string.tab_viewer_transposition_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** «+2» o «−1», con el signo menos tipográfico. */
internal fun signed(semitones: Int): String = if (semitones > 0) "+$semitones" else "−${-semitones}"
