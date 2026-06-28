package com.algorithmx.neoleaf.ui.reader

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
import kotlinx.coroutines.flow.collectLatest

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@Composable
fun PdfViewerFragmentHost(
    uri: Uri,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val fragmentState = remember { mutableStateOf<NeoLeafPdfViewerFragment?>(null) }

    AndroidFragment<NeoLeafPdfViewerFragment>(
        modifier = modifier.fillMaxSize(),
        onUpdate = { fragment ->
            fragmentState.value = fragment
            if (fragment.isAdded && fragment.documentUri != uri) {
                fragment.documentUri = uri
            }
        }
    )

    LaunchedEffect(viewModel) {
        viewModel.saveCommands.collectLatest {
            fragmentState.value?.let {
                if (it.isAdded) {
                    it.applyDraftEdits()
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.jumpToPageCommands.collectLatest { pageIndex ->
            fragmentState.value?.let {
                if (it.isAdded) {
                    it.scrollToPage(pageIndex)
                }
            }
        }
    }
}
