package com.algorithmx.neoleaf.ui.reader

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.core.pdf.engine.AlxExtractionEngine
import com.algorithmx.core.pdf.models.AlxPdfThumbnail
import com.algorithmx.core.pdf.models.AlxTocItem
import com.algorithmx.neoleaf.data.bookmark.AppDatabase
import com.algorithmx.neoleaf.data.bookmark.Bookmark
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias TocItem = AlxTocItem
typealias PdfPageThumbnail = AlxPdfThumbnail

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val bookmarkDao = AppDatabase.getDatabase(application).bookmarkDao()
    private val extractionEngine = AlxExtractionEngine(application)

    private val _thumbnails = MutableStateFlow<List<PdfPageThumbnail>>(emptyList())
    val thumbnails: StateFlow<List<PdfPageThumbnail>> = _thumbnails

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount

    private val _toc = MutableStateFlow<List<TocItem>>(emptyList())
    val toc: StateFlow<List<TocItem>> = _toc

    private val _bookmarks = MutableStateFlow<List<Int>>(emptyList())
    val bookmarks: StateFlow<List<Int>> = _bookmarks

    private val _saveCommands = MutableSharedFlow<Unit>()
    val saveCommands: SharedFlow<Unit> = _saveCommands

    private val _jumpToPageCommands = MutableSharedFlow<Int>()
    val jumpToPageCommands: SharedFlow<Int> = _jumpToPageCommands

    private val mutex = Mutex()
    private var currentLoadingUri: Uri? = null

    fun triggerSave() {
        viewModelScope.launch {
            _saveCommands.emit(Unit)
        }
    }

    fun jumpToPage(pageIndex: Int) {
        viewModelScope.launch {
            _jumpToPageCommands.emit(pageIndex)
        }
    }

    fun toggleBookmark(uri: Uri, pageIndex: Int) {
        viewModelScope.launch {
            if (_bookmarks.value.contains(pageIndex)) {
                bookmarkDao.deleteByPage(uri.toString(), pageIndex)
            } else {
                bookmarkDao.insert(Bookmark(uri.toString(), pageIndex))
            }
        }
    }

    fun loadDocumentData(context: Context, uri: Uri) {
        viewModelScope.launch {
            mutex.withLock {
                if (currentLoadingUri == uri) return@withLock
                currentLoadingUri = uri
                _thumbnails.value = emptyList()
                _pageCount.value = 0
                _toc.value = emptyList()
            }

            // Observe bookmarks
            viewModelScope.launch {
                bookmarkDao.getBookmarksForUri(uri.toString()).collectLatest { bookmarkList ->
                    _bookmarks.value = bookmarkList.map { it.pageIndex }.sorted()
                }
            }

            // Extract TOC
            viewModelScope.launch {
                val tocItems = extractionEngine.extractToc(uri)
                _toc.value = tocItems
            }

            // Generate Thumbnails
            viewModelScope.launch {
                extractionEngine.generateThumbnails(uri) { batch ->
                    val currentList = _thumbnails.value.toMutableList()
                    currentList.addAll(batch)
                    _thumbnails.value = currentList
                    _pageCount.value = currentList.size.coerceAtLeast(_pageCount.value)
                }
            }
        }
    }
}
