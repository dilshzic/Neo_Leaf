package com.algorithmx.neoleaf.ui.extraction

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.core.pdf.engine.AlxExtractionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExtractionState(
    val isExtracting: Boolean = false,
    val text: String = "",
    val images: List<Bitmap> = emptyList(),
    val error: String? = null,
    val textError: String? = null,
    val imageError: String? = null,
    val pageRangeInput: String = ""
)

class ExtractionViewModel(application: Application) : AndroidViewModel(application) {
    private val extractionEngine = AlxExtractionEngine(application)
    private val _uiState = MutableStateFlow(ExtractionState())
    val uiState: StateFlow<ExtractionState> = _uiState

    fun updatePageRange(input: String) {
        _uiState.value = _uiState.value.copy(pageRangeInput = input)
    }

    private fun parsePageRange(input: String): List<Int>? {
        if (input.isBlank()) return null
        
        val indices = mutableSetOf<Int>()
        val parts = input.split(",").map { it.trim() }
        
        try {
            for (part in parts) {
                if (part.contains("-")) {
                    val rangeParts = part.split("-").map { it.trim().toInt() }
                    if (rangeParts.size == 2) {
                        val start = (rangeParts[0] - 1).coerceAtLeast(0)
                        val end = (rangeParts[1] - 1).coerceAtLeast(0)
                        if (start <= end) {
                            for (i in start..end) indices.add(i)
                        } else {
                            for (i in end..start) indices.add(i)
                        }
                    }
                } else {
                    val page = part.toInt()
                    indices.add((page - 1).coerceAtLeast(0))
                }
            }
        } catch (e: Exception) {
            return null
        }
        
        return indices.sorted()
    }

    fun extractData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val currentRangeInput = _uiState.value.pageRangeInput
            val pageIndices = if (currentRangeInput.isNotEmpty()) {
                 parsePageRange(currentRangeInput) ?: run {
                     _uiState.value = _uiState.value.copy(
                         error = "Invalid page range format. Use e.g., '1, 3, 5-10'"
                     )
                     return@launch
                 }
            } else null

            _uiState.value = _uiState.value.copy(
                isExtracting = true, 
                error = null, 
                textError = null, 
                imageError = null
            )
            
            withContext(Dispatchers.IO) {
                val result = extractionEngine.extractFullContent(uri, pageIndices)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isExtracting = false,
                        text = result.text,
                        images = result.images,
                        textError = result.textError,
                        imageError = result.imageError,
                        error = result.generalError
                    )
                }
            }
        }
    }
}
