package com.algorithmx.core.pdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.algorithmx.core.pdf.models.AlxExtractionResult
import com.algorithmx.core.pdf.models.AlxPdfThumbnail
import com.algorithmx.core.pdf.models.AlxTocItem
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlxExtractionEngine(private val context: Context) {

    suspend fun extractToc(uri: Uri): List<AlxTocItem> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdDocument = PDDocument.load(inputStream)
                val outline = pdDocument.documentCatalog.documentOutline
                val result = if (outline != null) {
                    val tocItems = mutableListOf<AlxTocItem>()
                    var current = outline.firstChild
                    while (current != null) {
                        tocItems.add(mapOutlineItem(pdDocument, current, 0))
                        current = current.nextSibling
                    }
                    tocItems
                } else {
                    emptyList()
                }
                pdDocument.close()
                result
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun mapOutlineItem(pdDocument: PDDocument, item: PDOutlineItem, level: Int): AlxTocItem {
        val children = mutableListOf<AlxTocItem>()
        var currentChild = item.firstChild
        while (currentChild != null) {
            children.add(mapOutlineItem(pdDocument, currentChild, level + 1))
            currentChild = currentChild.nextSibling
        }
        
        val pageIndex = try {
            pdDocument.pages.indexOf(item.findDestinationPage(pdDocument))
        } catch (e: Exception) {
            -1
        }
        return AlxTocItem(
            title = item.title,
            pageIndex = if (pageIndex >= 0) pageIndex else 0,
            children = children,
            level = level
        )
    }

    suspend fun extractFullContent(uri: Uri, pageRange: List<Int>? = null): AlxExtractionResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdDocument = PDDocument.load(inputStream)
                val maxPages = pdDocument.numberOfPages
                val targetIndices = pageRange ?: (0 until maxPages).toList()

                val stripper = PDFTextStripper()
                val textBuilder = StringBuilder()
                val images = mutableListOf<Bitmap>()
                
                for (index in targetIndices) {
                    if (index >= maxPages) continue
                    val page = pdDocument.getPage(index)
                    
                    // Text
                    stripper.startPage = index + 1
                    stripper.endPage = index + 1
                    textBuilder.append("--- Page ${index + 1} ---\n")
                    textBuilder.append(stripper.getText(pdDocument))
                    textBuilder.append("\n\n")
                    
                    // Images
                    val resources = page.resources
                    for (name in resources.xObjectNames) {
                        val xObject = resources.getXObject(name)
                        if (xObject is PDImageXObject) {
                            images.add(xObject.image)
                        }
                    }
                }
                pdDocument.close()
                AlxExtractionResult(text = textBuilder.toString(), images = images)
            } ?: AlxExtractionResult(error = "Could not open stream")
        } catch (e: Exception) {
            AlxExtractionResult(error = e.message)
        }
    }

    suspend fun generateThumbnails(
        uri: Uri, 
        onBatchGenerated: suspend (List<AlxPdfThumbnail>) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                val batch = mutableListOf<AlxPdfThumbnail>()

                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val width = 200
                    val height = (width * page.height / page.width)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    batch.add(AlxPdfThumbnail(i, bitmap))
                    
                    if (batch.size >= 5 || i == totalPages - 1) {
                        onBatchGenerated(batch.toList())
                        batch.clear()
                    }
                }
                renderer.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
