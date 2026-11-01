package com.tolstoy.mitablaturas.feature.pdfviewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolstoy.mitablaturas.core.designsystem.KeepScreenOnEffect
import com.tolstoy.mitablaturas.core.designsystem.PedalAction
import com.tolstoy.mitablaturas.core.designsystem.pedalInput
import kotlinx.coroutines.launch

@Composable
fun PdfViewerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    KeepScreenOnEffect()
    // Vuelve a la página en la que se dejó y, desde entonces, recuerda la página visible.
    LaunchedEffect(uiState.pageToRestore) {
        val page = uiState.pageToRestore ?: return@LaunchedEffect
        listState.scrollToItem(page)
        viewModel.onPageRestored()
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { viewModel.onPageChanged(it) }
    }
    PdfViewerScreen(
        uiState = uiState,
        listState = listState,
        onBack = onBack,
        renderPage = viewModel::renderPage,
        modifier = modifier.pedalInput { action ->
            // Un pedal pasapáginas avanza o retrocede una página entera.
            val target = when (action) {
                PedalAction.NEXT_PAGE, PedalAction.PLAY_PAUSE -> listState.firstVisibleItemIndex + 1
                PedalAction.PREVIOUS_PAGE -> listState.firstVisibleItemIndex - 1
            }
            if (target in uiState.pageSizes.indices) scope.launch { listState.animateScrollToItem(target) }
        },
    )
}

@Composable
fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    onBack: () -> Unit,
    renderPage: suspend (index: Int, widthPx: Int) -> Bitmap?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier.fillMaxSize()) {
        Header(uiState, listState, onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.error != null -> ErrorMessage(uiState.error, Modifier.align(Alignment.Center))
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(uiState.pageSizes) { index, size ->
                        PdfPage(index, size, renderPage)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(uiState: PdfViewerUiState, listState: LazyListState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.pdf_viewer_back)) }
        Column(Modifier.weight(1f)) {
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val pageCount = uiState.pageSizes.size
            if (pageCount > 0) {
                Text(
                    text = stringResource(R.string.pdf_viewer_page_count, listState.firstVisibleItemIndex + 1, pageCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Una página a todo el ancho; se dibuja al aparecer en pantalla y se libera al salir. */
@Composable
private fun PdfPage(index: Int, size: PageSize, renderPage: suspend (Int, Int) -> Bitmap?) {
    val description = stringResource(R.string.pdf_viewer_page_description, index + 1)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(size.aspectRatio),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth
            var image by remember(index, widthPx) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(index, widthPx) {
                image = renderPage(index, widthPx)?.asImageBitmap()
            }
            val current = image
            if (current != null) {
                Image(
                    bitmap = current,
                    contentDescription = description,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(error: PdfViewerError, modifier: Modifier = Modifier) {
    val text = when (error) {
        PdfViewerError.UNREADABLE -> stringResource(R.string.pdf_viewer_error_unreadable)
        PdfViewerError.UNSUPPORTED -> stringResource(R.string.pdf_viewer_error_unsupported)
        PdfViewerError.PROTECTED -> stringResource(R.string.pdf_viewer_error_protected)
        PdfViewerError.DAMAGED -> stringResource(R.string.pdf_viewer_error_damaged)
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
