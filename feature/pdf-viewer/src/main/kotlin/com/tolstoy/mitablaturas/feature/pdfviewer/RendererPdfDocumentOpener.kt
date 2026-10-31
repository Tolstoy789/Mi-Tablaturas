package com.tolstoy.mitablaturas.feature.pdfviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tolstoy.mitablaturas.core.data.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Abre el PDF con [PdfRenderer]. Copia el contenido a la caché porque `PdfRenderer` necesita un
 * descriptor con acceso aleatorio, que no todas las apps (WhatsApp, Drive) ofrecen.
 */
class RendererPdfDocumentOpener @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PdfDocumentOpener {

    override suspend fun open(bytes: ByteArray): PdfPages = withContext(ioDispatcher) {
        val folder = File(context.cacheDir, "pdf").apply { mkdirs() }
        val file = File.createTempFile("abierto", ".pdf", folder)
        try {
            file.writeBytes(bytes)
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = try {
                PdfRenderer(descriptor)
            } catch (e: Exception) {
                descriptor.close()
                throw e
            }
            // PdfRenderer solo admite una página abierta a la vez: todo pasa por un único hilo.
            RendererPdfPages(file, descriptor, renderer, ioDispatcher.limitedParallelism(1))
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }
}

private class RendererPdfPages(
    private val file: File,
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val renderDispatcher: CoroutineDispatcher,
) : PdfPages {

    override val pageSizes: List<PageSize> = (0 until renderer.pageCount).map { index ->
        renderer.openPage(index).use { PageSize(it.width, it.height) }
    }

    @Volatile private var closed = false

    override suspend fun render(index: Int, widthPx: Int): Bitmap = withContext(renderDispatcher) {
        check(!closed) { "El PDF ya está cerrado" }
        renderer.openPage(index).use { page ->
            val height = (widthPx / pageSizes[index].aspectRatio).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }
    }

    /** Cierra en el hilo de dibujo, después de la página que se esté dibujando. */
    override fun close() {
        if (closed) return
        closed = true
        CoroutineScope(renderDispatcher).launch {
            renderer.close()
            descriptor.close()
            file.delete()
        }
    }
}
