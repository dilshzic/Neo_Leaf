package com.algorithmx.neoleaf.ui.reader

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class NeoLeafPdfViewerFragment : EditablePdfViewerFragment() {

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
                    Toast.makeText(requireContext(), "Annotations saved!", Toast.LENGTH_SHORT).show()
                    isEditModeEnabled = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to save annotations", Toast.LENGTH_SHORT).show()
                }
            } finally {
                handle.close()
            }
        }
    }

    override fun onApplyEditsFailed(error: Throwable) {
        Toast.makeText(requireContext(), "Failed to apply edits: ${error.message}", Toast.LENGTH_SHORT).show()
    }
}
