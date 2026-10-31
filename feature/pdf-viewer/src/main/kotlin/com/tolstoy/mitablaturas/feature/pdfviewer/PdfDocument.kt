package com.tolstoy.mitablaturas.feature.pdfviewer

import android.graphics.Bitmap
import java.io.Closeable

/** Tamaño de una página en puntos PDF (1/72 de pulgada). */
data class PageSize(val width: Int, val height: Int) {
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height else 1f
}

/** PDF abierto y listo para dibujar sus páginas. */
interface PdfPages : Closeable {
    val pageSizes: List<PageSize>

    /** Dibuja la página [index] con [widthPx] píxeles de ancho, sobre fondo blanco. */
    suspend fun render(index: Int, widthPx: Int): Bitmap
}

/** Abre un PDF a partir de su contenido. */
interface PdfDocumentOpener {
    /**
     * @throws SecurityException si el PDF está protegido con contraseña.
     * @throws java.io.IOException si el archivo está dañado o no es un PDF.
     */
    suspend fun open(bytes: ByteArray): PdfPages
}
