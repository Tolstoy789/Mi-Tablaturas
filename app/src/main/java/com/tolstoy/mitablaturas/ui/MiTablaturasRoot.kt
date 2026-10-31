package com.tolstoy.mitablaturas.ui

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tolstoy.mitablaturas.feature.filepicker.FilePickerRoute
import com.tolstoy.mitablaturas.feature.tabviewer.TabViewerRoute
import com.tolstoy.mitablaturas.feature.tabviewer.TabViewerViewModel

private const val ROUTE_PICKER = "picker"
private const val ROUTE_VIEWER = "viewer?${TabViewerViewModel.ARG_URI}={${TabViewerViewModel.ARG_URI}}"

private fun viewerRoute(uri: String) = "viewer?${TabViewerViewModel.ARG_URI}=${Uri.encode(uri)}"

/**
 * @param pendingDocument URI que otra app pidió abrir; se muestra en el visor y se avisa con
 * [onPendingDocumentOpened] para no volver a abrirla.
 */
@Composable
fun MiTablaturasRoot(
    pendingDocument: String?,
    onPendingDocumentOpened: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    LaunchedEffect(pendingDocument) {
        val uri = pendingDocument ?: return@LaunchedEffect
        // Sustituye el visor abierto, si lo hay, y deja el inicio debajo para volver atrás.
        navController.navigate(viewerRoute(uri)) { popUpTo(ROUTE_PICKER) }
        onPendingDocumentOpened()
    }
    // safeDrawing incluye el recorte de cámara, que en horizontal queda a un lado de la pantalla.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_PICKER,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_PICKER) {
                FilePickerRoute(onOpenDocument = { document -> navController.navigate(viewerRoute(document.uri)) })
            }
            composable(
                route = ROUTE_VIEWER,
                arguments = listOf(navArgument(TabViewerViewModel.ARG_URI) { type = NavType.StringType }),
            ) {
                TabViewerRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
