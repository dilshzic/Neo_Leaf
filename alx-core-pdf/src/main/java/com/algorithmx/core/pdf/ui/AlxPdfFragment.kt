package com.algorithmx.core.pdf.ui

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfWriteHandle
import androidx.pdf.ink.EditablePdfViewerFragment
import androidx.pdf.view.PdfView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
open class AlxPdfFragment : EditablePdfViewerFragment() {

    var onDocumentLoaded: (() -> Unit)? = null
    private var isLoadCallbackTriggered = false

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pollForLoadCompletion()
    }

    private fun pollForLoadCompletion() {
        lifecycleScope.launch {
            // We poll for the underlying PdfView to be present and initialized.
            // This is a reliable way to detect when scrollToPage will actually work.
            while (!isLoadCallbackTriggered) {
                val pdfView = findPdfView(view)
                if (pdfView != null) {
                    // Small additional delay to ensure the internal renderer is truly ready
                    delay(100)
                    isLoadCallbackTriggered = true
                    onDocumentLoaded?.invoke()
                    break
                }
                delay(100)
            }
        }
    }

    fun scrollToPage(pageIndex: Int) {
        val pdfView = findPdfView(view)
        pdfView?.scrollToPage(pageIndex)
    }

    private fun findPdfView(view: View?): PdfView? {
        if (view is PdfView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findPdfView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    override fun onApplyEditsSuccess(handle: PdfWriteHandle) {
        val uri = documentUri ?: return
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pfd = requireContext().contentResolver.openFileDescriptor(uri, "rw")
                    pfd?.use { descriptor ->
                        handle.writeTo(descriptor)
                    }
                }
                withContext(Dispatchers.Main) {
                    onSaveSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onSaveError(e)
                }
            } finally {
                handle.close()
            }
        }
    }

    open fun onSaveSuccess() {
        if (isAdded) {
            Toast.makeText(requireContext(), "Document saved successfully", Toast.LENGTH_SHORT).show()
        }
        isEditModeEnabled = false
    }

    open fun onSaveError(e: Exception) {
        if (isAdded) {
            Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onApplyEditsFailed(error: Throwable) {
        onSaveError(Exception(error))
    }
}
