package com.algorithmx.neoleaf.ui.toc_extraction

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.core.pdf.engine.AlxExtractionEngine
import com.algorithmx.core.pdf.models.AlxExtractionResult
import com.algorithmx.core.pdf.models.AlxPdfSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TocExtractionState(
    val isLoading: Boolean = false,
    val sections: List<AlxPdfSection> = emptyList(),
    val result: AlxExtractionResult? = null,
    val isExtracting: Boolean = false,
    val error: String? = null
)

class TocExtractionViewModel(application: Application) : AndroidViewModel(application) {
    private val extractionEngine = AlxExtractionEngine(application)
    private val _uiState = MutableStateFlow(TocExtractionState())
    val uiState: StateFlow<TocExtractionState> = _uiState

    fun loadSections(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            withContext(Dispatchers.IO) {
                val sections = extractionEngine.extractSections(uri)
                withContext(Dispatchers.Main) {
                    if (sections.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No Table of Contents found in this document."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            sections = sections
                        )
                    }
                }
            }
        }
    }

    fun extractSection(uri: Uri, section: AlxPdfSection) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExtracting = true, result = null)
            val range = (section.startPage..section.endPage).toList()
            withContext(Dispatchers.IO) {
                val result = extractionEngine.extractFullContent(uri, range)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isExtracting = false,
                        result = result
                    )
                }
            }
        }
    }
    
    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }
}
