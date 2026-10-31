package com.tolstoy.mitablaturas.feature.tabviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Mientras suena la partitura, pide el foco de audio y escucha la desconexión de auriculares.
 * Llama a [onInterrupted] si entra una llamada, otra app empieza a sonar o se quitan los
 * auriculares, para no seguir sonando por el altavoz.
 *
 * El WebView no gestiona el foco de audio del Web Audio que usa AlphaTab, por eso se hace aquí.
 */
@Composable
internal fun PlaybackInterruptionsEffect(isPlaying: Boolean, onInterrupted: () -> Unit) {
    val context = LocalContext.current
    val currentOnInterrupted by rememberUpdatedState(onInterrupted)
    DisposableEffect(isPlaying, context) {
        if (!isPlaying) return@DisposableEffect onDispose { }

        val audioManager = context.getSystemService(AudioManager::class.java)
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener { change ->
                // Una notificación breve (CAN_DUCK) solo baja el volumen; lo hace el sistema.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    currentOnInterrupted()
                }
            }
            .build()
        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            // Por ejemplo, durante una llamada.
            currentOnInterrupted()
        }

        val noisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) currentOnInterrupted()
            }
        }
        ContextCompat.registerReceiver(
            context,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose {
            context.unregisterReceiver(noisyReceiver)
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }
}
