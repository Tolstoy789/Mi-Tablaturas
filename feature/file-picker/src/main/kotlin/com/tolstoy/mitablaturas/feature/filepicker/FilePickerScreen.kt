package com.tolstoy.mitablaturas.feature.filepicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolstoy.mitablaturas.core.model.TabDocument
import com.tolstoy.mitablaturas.core.model.TabFormat

/** Tipos MIME a ofrecer: los Guitar Pro no tienen un MIME estándar, así que se filtra por extensión después. */
private val PICKER_MIME_TYPES = arrayOf("*/*")

@Composable
fun FilePickerRoute(
    onOpenDocument: (TabDocument) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilePickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onDocumentPicked(uri?.toString())
    }
    FilePickerScreen(
        uiState = uiState,
        onOpenFile = { launcher.launch(PICKER_MIME_TYPES) },
        onDismissError = viewModel::onErrorDismissed,
        onOpenDocument = onOpenDocument,
        modifier = modifier,
    )
}

@Composable
fun FilePickerScreen(
    uiState: FilePickerUiState,
    onOpenFile: () -> Unit,
    onDismissError: () -> Unit,
    onOpenDocument: (TabDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val result: @Composable () -> Unit = {
            when (uiState) {
                is FilePickerUiState.Ready -> DocumentCard(uiState.document, onOpenDocument)
                is FilePickerUiState.Error -> ErrorCard(uiState.error, onDismissError)
                FilePickerUiState.Idle, FilePickerUiState.Loading -> Unit
            }
        }
        val openCard: @Composable () -> Unit = {
            OpenFileCard(isLoading = uiState is FilePickerUiState.Loading, onOpenFile = onOpenFile)
        }
        if (isLandscape) {
            // En horizontal: título y tarjeta para abrir a la izquierda, resultado a la derecha.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Title()
                    openCard()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) { result() }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Title()
                openCard()
                result()
            }
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResource(R.string.file_picker_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun OpenFileCard(isLoading: Boolean, onOpenFile: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.file_picker_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.file_picker_card_body), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onOpenFile,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.file_picker_open))
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(document: TabDocument, onOpen: (TabDocument) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormatBadge(document.format)
            Column(Modifier.weight(1f)) {
                Text(document.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listOfNotNull(document.format.label(), document.sizeBytes?.let(::formatSize)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (document.format.isInteractive) {
                Button(onClick = { onOpen(document) }) {
                    Text(stringResource(R.string.file_picker_view))
                }
            } else {
                Text(
                    text = stringResource(R.string.file_picker_pdf_soon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FormatBadge(format: TabFormat) {
    val (container, content) = if (format.isInteractive) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(container, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(format.shortLabel(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = content)
    }
}

@Composable
private fun ErrorCard(error: FilePickerError, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(error.message(), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.file_picker_dismiss))
            }
        }
    }
}

@Composable
private fun FilePickerError.message(): String = when (this) {
    is FilePickerError.UnsupportedFormat -> stringResource(R.string.file_picker_error_format, fileName)
    is FilePickerError.EmptyFile -> stringResource(R.string.file_picker_error_empty, fileName)
    is FilePickerError.TooLarge -> stringResource(R.string.file_picker_error_too_large, fileName, maxBytes / (1024 * 1024))
    FilePickerError.Unreadable -> stringResource(R.string.file_picker_error_unreadable)
}

private fun TabFormat.shortLabel(): String = extensions.first().uppercase()

@Composable
private fun TabFormat.label(): String = when (this) {
    TabFormat.PDF -> stringResource(R.string.file_picker_format_pdf)
    else -> stringResource(R.string.file_picker_format_gp)
}

internal fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

@Preview(showBackground = true)
@Composable
private fun FilePickerScreenReadyPreview() {
    MaterialTheme {
        FilePickerScreen(
            uiState = FilePickerUiState.Ready(
                TabDocument("content://demo", "Blues en La.gp5", TabFormat.GUITAR_PRO_5, 48_213),
            ),
            onOpenFile = {},
            onDismissError = {},
            onOpenDocument = {},
        )
    }
}
