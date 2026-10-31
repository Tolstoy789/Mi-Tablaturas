package com.tolstoy.mitablaturas.feature.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tolstoy.mitablaturas.core.designsystem.swatches
import com.tolstoy.mitablaturas.core.model.AppDesign

@Composable
internal fun DesignPickerDialog(
    selected: AppDesign,
    onSelect: (AppDesign) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.design_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppDesign.entries.forEach { design ->
                    DesignOption(design, design == selected) { onSelect(design) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.design_dialog_done)) }
        },
    )
}

@Composable
private fun DesignOption(design: AppDesign, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(design.title(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = design.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            design.swatches.forEach { color ->
                Box(
                    Modifier
                        .size(22.dp)
                        .background(color, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
    }
}

@Composable
internal fun AppDesign.title(): String = stringResource(
    when (this) {
        AppDesign.ATRIL -> R.string.design_atril
        AppDesign.ESCENARIO -> R.string.design_escenario
        AppDesign.TALLER -> R.string.design_taller
    },
)

@Composable
private fun AppDesign.description(): String = stringResource(
    when (this) {
        AppDesign.ATRIL -> R.string.design_atril_description
        AppDesign.ESCENARIO -> R.string.design_escenario_description
        AppDesign.TALLER -> R.string.design_taller_description
    },
)
