package com.algorithmx.core.pdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.algorithmx.core.pdf.models.AlxExtractionResult
import com.algorithmx.core.pdf.models.AlxPdfThumbnail
import com.algorithmx.core.pdf.models.AlxTocItem
import com.algorithmx.core.pdf.models.AlxPdfSection
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

    suspend fun extractSections(uri: Uri): List<AlxPdfSection> = withContext(Dispatchers.IO) {
        val toc = extractToc(uri)
        if (toc.isEmpty()) return@withContext emptyList()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdDocument = PDDocument.load(inputStream)
                val totalPages = pdDocument.numberOfPages
                pdDocument.close()

                val flattenedToc = flattenToc(toc)
                val sections = mutableListOf<AlxPdfSection>()

                for (i in flattenedToc.indices) {
                    val current = flattenedToc[i]
                    val start = current.pageIndex
                    
                    // Logic: A section ends where the next item of EQUAL OR HIGHER level starts.
                    // If no such item exists, it ends at the document's end.
                    var nextBoundaryPage = totalPages
                    
                    for (j in (i + 1) until flattenedToc.size) {
                        val potentialNext = flattenedToc[j]
                        if (potentialNext.level <= current.level) {
                            nextBoundaryPage = potentialNext.pageIndex
                            break
                        }
                    }

                    // End page is inclusive for extraction, so we take nextBoundaryPage - 1
                    // Ensure end >= start
                    val end = if (nextBoundaryPage > start) nextBoundaryPage - 1 else start

                    sections.add(
                        AlxPdfSection(
                            title = current.title,
                            startPage = start,
                            endPage = end,
                            level = current.level
                        )
                    )
                }
                sections
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Extracts text from specific pages or the entire document.
     * @param uri Document URI
     * @param pageRange Optional list of 0-indexed page indices. If null, extracts all pages.
     */
    suspend fun extractText(uri: Uri, pageRange: List<Int>? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdDocument = PDDocument.load(inputStream)
                val maxPages = pdDocument.numberOfPages
                val targetIndices = pageRange ?: (0 until maxPages).toList()

                val stripper = PDFTextStripper()
                val textBuilder = StringBuilder()
                for (index in targetIndices) {
                    if (index < maxPages) {
                        stripper.startPage = index + 1
                        stripper.endPage = index + 1
                        if (targetIndices.size > 1) {
                            textBuilder.append("--- Page ${index + 1} ---\n")
                        }
                        textBuilder.append(stripper.getText(pdDocument))
                        textBuilder.append("\n\n")
                    }
                }
                pdDocument.close()
                Result.success(textBuilder.toString())
            } ?: Result.failure(Exception("Could not open stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts images from specific pages or the entire document.
     * @param uri Document URI
     * @param pageRange Optional list of 0-indexed page indices. If null, extracts all pages.
     */
    suspend fun extractImages(uri: Uri, pageRange: List<Int>? = null): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdDocument = PDDocument.load(inputStream)
                val maxPages = pdDocument.numberOfPages
                val targetIndices = pageRange ?: (0 until maxPages).toList()

                val images = mutableListOf<Bitmap>()
                for (index in targetIndices) {
                    if (index < maxPages) {
                        val page = pdDocument.getPage(index)
                        val resources = page.resources
                        for (name in resources.xObjectNames) {
                            val xObject = resources.getXObject(name)
                            if (xObject is PDImageXObject) {
                                images.add(xObject.image)
                            }
                        }
                    }
                }
                pdDocument.close()
                Result.success(images)
            } ?: Result.failure(Exception("Could not open stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractFullContent(uri: Uri, pageRange: List<Int>? = null): AlxExtractionResult = withContext(Dispatchers.IO) {
        val textResult = extractText(uri, pageRange)
        val imageResult = extractImages(uri, pageRange)

        AlxExtractionResult(
            text = textResult.getOrDefault(""),
            images = imageResult.getOrDefault(emptyList()),
            textError = textResult.exceptionOrNull()?.let { "Text extraction failed: ${it.message}" },
            imageError = imageResult.exceptionOrNull()?.let { "Image extraction failed: ${it.message}" },
            generalError = if (textResult.isFailure && imageResult.isFailure) "Both extraction processes failed." else null
        )
    }

    private fun flattenToc(items: List<AlxTocItem>): List<AlxTocItem> {
        val result = mutableListOf<AlxTocItem>()
        for (item in items) {
            result.add(item)
            result.addAll(flattenToc(item.children))
        }
        return result
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
