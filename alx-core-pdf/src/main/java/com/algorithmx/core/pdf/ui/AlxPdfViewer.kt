package com.algorithmx.core.pdf.ui

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@Composable
fun AlxPdfViewer(
    uri: Uri,
    modifier: Modifier = Modifier,
    saveCommands: SharedFlow<Unit>? = null,
    jumpToPageCommands: SharedFlow<Int>? = null
) {
    val fragmentState = remember { mutableStateOf<AlxPdfFragment?>(null) }

    AndroidFragment<AlxPdfFragment>(
        modifier = modifier.fillMaxSize(),
        onUpdate = { fragment ->
            fragmentState.value = fragment
            if (fragment.isAdded && fragment.documentUri != uri) {
                fragment.documentUri = uri
            }
        }
    )

    if (saveCommands != null) {
        LaunchedEffect(saveCommands) {
            saveCommands.collectLatest {
                fragmentState.value?.let {
                    if (it.isAdded) {
                        it.applyDraftEdits()
                    }
                }
            }
        }
    }

    if (jumpToPageCommands != null) {
        LaunchedEffect(jumpToPageCommands) {
            jumpToPageCommands.collectLatest { pageIndex ->
                fragmentState.value?.let {
                    if (it.isAdded) {
                        it.scrollToPage(pageIndex)
                    }
                }
            }
        }
    }
}
