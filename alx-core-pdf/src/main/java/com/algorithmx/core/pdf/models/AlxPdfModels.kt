package com.algorithmx.core.pdf.models

import android.graphics.Bitmap

data class AlxPdfThumbnail(val pageIndex: Int, val bitmap: Bitmap)

data class AlxTocItem(
    val title: String,
    val pageIndex: Int,
    val children: List<AlxTocItem> = emptyList(),
    val level: Int = 0
)

data class AlxPdfSection(
    val title: String,
    val startPage: Int,
    val endPage: Int,
    val level: Int
) {
    val isSinglePage: Boolean = startPage == endPage
}

data class AlxExtractionResult(
    val text: String = "",
    val images: List<Bitmap> = emptyList(),
    val textError: String? = null,
    val imageError: String? = null,
    val generalError: String? = null
)
