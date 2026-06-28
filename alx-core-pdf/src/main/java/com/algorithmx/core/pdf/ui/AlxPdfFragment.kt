package com.algorithmx.core.pdf.ui

import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfWriteHandle
import androidx.pdf.ink.EditablePdfViewerFragment
import androidx.pdf.view.PdfView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
open class AlxPdfFragment : EditablePdfViewerFragment() {

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
        Toast.makeText(requireContext(), "Document saved successfully", Toast.LENGTH_SHORT).show()
        isEditModeEnabled = false
    }

    open fun onSaveError(e: Exception) {
        Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onApplyEditsFailed(error: Throwable) {
        onSaveError(Exception(error))
    }
}
