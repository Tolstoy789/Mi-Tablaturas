package com.tolstoy.mitablaturas.feature.pdfviewer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfViewerModule {

    @Binds
    abstract fun bindPdfDocumentOpener(impl: RendererPdfDocumentOpener): PdfDocumentOpener
}
