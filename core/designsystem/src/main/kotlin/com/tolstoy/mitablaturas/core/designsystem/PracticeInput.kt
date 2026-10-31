package com.tolstoy.mitablaturas.core.designsystem

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView

/** Mantiene la pantalla encendida mientras el visor está en pantalla, para leer tocando. */
@Composable
fun KeepScreenOnEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Acciones de un pedal pasapáginas Bluetooth o de un teclado. */
enum class PedalAction { PLAY_PAUSE, NEXT_PAGE, PREVIOUS_PAGE }

/**
 * Traduce una tecla a una acción del visor.
 *
 * Las teclas de página y multimedia funcionan siempre. Espacio, Intro y las flechas solo
 * cuando ningún botón tiene el foco ([viewerFocused]), para no romper la navegación con teclado.
 */
internal fun pedalActionFor(keyCode: Int, viewerFocused: Boolean): PedalAction? = when (keyCode) {
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> PedalAction.PLAY_PAUSE
    KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT -> PedalAction.NEXT_PAGE
    KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> PedalAction.PREVIOUS_PAGE
    KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER -> PedalAction.PLAY_PAUSE.takeIf { viewerFocused }
    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> PedalAction.NEXT_PAGE.takeIf { viewerFocused }
    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> PedalAction.PREVIOUS_PAGE.takeIf { viewerFocused }
    else -> null
}

/**
 * Recibe las teclas de un pedal Bluetooth o teclado cuando el foco no está dentro de un
 * WebView. Pide el foco al aparecer para que el pedal funcione sin tocar la pantalla.
 */
@Composable
fun Modifier.pedalInput(onPedal: (PedalAction) -> Unit): Modifier {
    val focusRequester = remember { FocusRequester() }
    var viewerFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // onKeyEvent y onFocusChanged van antes de focusable() para recibir lo que llega a ese foco.
    return this
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val action = pedalActionFor(event.nativeKeyEvent.keyCode, viewerFocused) ?: return@onKeyEvent false
            onPedal(action)
            true
        }
        .onFocusChanged { viewerFocused = it.isFocused }
        .focusRequester(focusRequester)
        .focusable()
}
