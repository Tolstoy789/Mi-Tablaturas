package com.tolstoy.mitablaturas.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tolstoy.mitablaturas.feature.filepicker.FilePickerRoute

/** Raíz de la UI. La navegación hacia el visor de tablaturas se añade en el paso 3. */
@Composable
fun MiTablaturasRoot(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        FilePickerRoute(modifier = Modifier.padding(innerPadding))
    }
}
