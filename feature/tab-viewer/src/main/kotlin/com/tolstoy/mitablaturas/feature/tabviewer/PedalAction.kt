package com.tolstoy.mitablaturas.feature.tabviewer

import android.view.KeyEvent

/** Acciones de un pedal pasapáginas Bluetooth o de un teclado. */
enum class PedalAction { PLAY_PAUSE, NEXT_PAGE, PREVIOUS_PAGE }

/**
 * Traduce una tecla a una acción del visor.
 *
 * Las teclas de página y multimedia funcionan siempre. Espacio, Intro y las flechas solo
 * cuando ningún botón tiene el foco ([viewerFocused]), para no romper la navegación con teclado.
 * Si el foco está en la partitura (WebView), las teclas las atiende `bridge.js`.
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
