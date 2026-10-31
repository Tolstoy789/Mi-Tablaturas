package com.tolstoy.mitablaturas

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/**
 * URI del archivo que otra app pide abrir (Ver) o comparte (Compartir), o `null` si el
 * intent no trae ninguno. Solo se aceptan URIs `content://`, que son las que llegan con permiso.
 */
internal fun Intent.externalDocumentUri(): Uri? {
    val uri = when (action) {
        Intent.ACTION_VIEW -> data
        Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
        else -> null
    }
    return uri?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }
}
