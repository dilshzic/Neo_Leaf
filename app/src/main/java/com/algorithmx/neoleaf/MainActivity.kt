package com.algorithmx.neoleaf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.algorithmx.core.pdf.ui.AlxPdfViewer
import com.algorithmx.neoleaf.ui.extraction.ExtractionScreen
import com.algorithmx.neoleaf.ui.reader.ReaderViewModel
import com.algorithmx.neoleaf.ui.reader.TocItem
import com.algorithmx.neoleaf.ui.toc_extraction.TocExtractionScreen
import com.algorithmx.neoleaf.ui.theme.NeoLeafTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    data object Picker : Route()
    @Serializable
    data class Sidebar(val uriString: String) : Route()
    @Serializable
    data class Viewer(val uriString: String) : Route()
    @Serializable
    data object Extraction : Route()
    @Serializable
    data object TocExtraction : Route()
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            NeoLeafTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 18) {
                    MainContent()
                } else {
                    UnsupportedDeviceScreen()
                }
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    @OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val readerViewModel: ReaderViewModel = viewModel()
        val backStack = remember { mutableStateListOf<Route>(Route.Picker) }
        val windowAdaptiveInfo = currentWindowAdaptiveInfo()
        val directive = remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
        val listDetailStrategy = rememberListDetailSceneStrategy<Route>(directive = directive)

        val pageCount by readerViewModel.pageCount.collectAsState()
        var showGoToPageDialog by remember { mutableStateOf(false) }

        if (showGoToPageDialog) {
            GoToPageDialog(
                pageCount = pageCount,
                onDismiss = { showGoToPageDialog = false },
                onConfirm = { page ->
                    readerViewModel.jumpToPage(page - 1) // 0-indexed
                    showGoToPageDialog = false
                }
            )
        }

        NavDisplay(
            backStack = backStack,
            onBack = { 
                if (backStack.size > 1 && backStack.last() is Route.Viewer) {
                    val hasSidebar = backStack.any { it is Route.Sidebar }
                    if (hasSidebar) {
                        backStack.removeAll { it is Route.Sidebar || it is Route.Viewer }
                    } else {
                        backStack.removeLast()
                    }
                } else {
                    backStack.removeLastOrNull()
                }
            },
            sceneStrategy = listDetailStrategy,
            entryProvider = entryProvider {
                entry<Route.Picker> {
                    PdfPickerScreen(
                        onPdfSelected = { uri ->
                            backStack.add(Route.Viewer(uri.toString()))
                        },
                        onOpenExtraction = {
                            backStack.add(Route.Extraction)
                        },
                        onOpenTocExtraction = {
                            backStack.add(Route.TocExtraction)
                        }
                    )
                }
                entry<Route.Sidebar>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) { key ->
                    SidebarContent(uri = Uri.parse(key.uriString), viewModel = readerViewModel)
                }
                entry<Route.Viewer>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { key ->
                    val uri = Uri.parse(key.uriString)
                    
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Reader", style = MaterialTheme.typography.titleMedium) },
                                actions = {
                                    IconButton(onClick = { 
                                        val hasSidebar = backStack.any { it is Route.Sidebar }
                                        if (hasSidebar) {
                                            backStack.removeAll { it is Route.Sidebar }
                                        } else {
                                            // Add Sidebar BEFORE Viewer for side-by-side on large screens
                                            val viewer = backStack.last()
                                            backStack.removeLast()
                                            backStack.add(Route.Sidebar(key.uriString))
                                            backStack.add(viewer)
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "Toggle Sidebar")
                                    }
                                    IconButton(onClick = { showGoToPageDialog = true }) {
                                        Icon(Icons.Rounded.FindInPage, contentDescription = "Go to Page")
                                    }
                                    IconButton(onClick = { 
                                        readerViewModel.triggerSave()
                                    }) {
                                        Icon(Icons.Rounded.Save, contentDescription = "Save")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    ) { innerPadding ->
                        AlxPdfViewer(
                            uri = uri,
                            saveCommands = readerViewModel.saveCommands,
                            jumpToPageCommands = readerViewModel.jumpToPageCommands,
                            onDocumentLoaded = { readerViewModel.onDocumentLoaded() },
                            modifier = Modifier.padding(innerPadding).fillMaxSize()
                        )
                    }
                }
                entry<Route.Extraction> {
                    ExtractionScreen(onBack = { backStack.removeLastOrNull() })
                }
                entry<Route.TocExtraction> {
                    TocExtractionScreen(onBack = { backStack.removeLastOrNull() })
                }
            }
        )
    }
}

@Composable
fun SidebarContent(
    uri: Uri,
    viewModel: ReaderViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pages", "Contents", "Shelved")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            Column {
                Text(
                    text = "Document",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ThumbnailListScreen(uri = uri, viewModel = viewModel, showBookmarkAction = true)
                1 -> TocListScreen(viewModel = viewModel)
                2 -> ShelvedListScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TocListScreen(viewModel: ReaderViewModel) {
    val toc by viewModel.toc.collectAsState()
    
    if (toc.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No Table of Contents available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val flattenedToc = flattenToc(toc)
            items(flattenedToc) { item ->
                TocItemRow(item) {
                    viewModel.jumpToPage(item.pageIndex)
                }
            }
        }
    }
}

@Composable
fun ShelvedListScreen(viewModel: ReaderViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()
    
    if (bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No bookmarked pages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bookmarks) { pageIndex ->
                val thumb = thumbnails.find { it.pageIndex == pageIndex }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.jumpToPage(pageIndex) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.height(60.dp).width(45.dp),
                                contentScale = ContentScale.FillBounds
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = "Page ${pageIndex + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TocItemRow(item: TocItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = (item.level * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (item.level == 0) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = (item.pageIndex + 1).toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

fun flattenToc(items: List<TocItem>): List<TocItem> {
    val result = mutableListOf<TocItem>()
    for (item in items) {
        result.add(item)
        result.addAll(flattenToc(item.children))
    }
    return result
}

@Composable
fun GoToPageDialog(
    pageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pageText by remember { mutableStateOf("") }
    val pageNum = pageText.toIntOrNull()
    val isValid = pageNum != null && pageNum in 1..pageCount
    val isError = pageText.isNotEmpty() && !isValid
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Page") },
        text = {
            Column {
                Text(
                    text = "Total pages: $pageCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) pageText = it },
                    label = { Text("Enter page number") },
                    placeholder = { Text("1 - $pageCount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a number between 1 and $pageCount")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    pageNum?.let { onConfirm(it) }
                },
                enabled = isValid
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UnsupportedDeviceScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.height(100.dp).fillMaxWidth(0.3f),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Unsupported Device",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Neo Leaf requires Android 12 (API 31) and SDK Extension 18 to unlock its full PDF editing potential.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PdfPickerScreen(
    onPdfSelected: (Uri) -> Unit,
    onOpenExtraction: () -> Unit,
    onOpenTocExtraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onPdfSelected(it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.height(140.dp).fillMaxWidth(0.4f),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Neo Leaf",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Your intelligent document suite",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { launcher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth(0.7f),
            contentPadding = PaddingValues(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Open PDF Reader", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onOpenExtraction,
            modifier = Modifier.fillMaxWidth(0.7f),
            contentPadding = PaddingValues(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Rounded.SettingsEthernet, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Extraction Pipeline", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onOpenTocExtraction,
            modifier = Modifier.fillMaxWidth(0.7f),
            contentPadding = PaddingValues(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Rounded.AccountTree, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("TOC Extraction", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ThumbnailListScreen(
    uri: Uri,
    viewModel: ReaderViewModel,
    showBookmarkAction: Boolean = false
) {
    val context = LocalContext.current
    val thumbnails by viewModel.thumbnails.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    LaunchedEffect(uri) {
        viewModel.loadDocumentData(context, uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(thumbnails) { thumb ->
            val isBookmarked = bookmarks.contains(thumb.pageIndex)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.jumpToPage(thumb.pageIndex) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    Box {
                        Image(
                            bitmap = thumb.bitmap.asImageBitmap(),
                            contentDescription = "Page ${thumb.pageIndex + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                        if (showBookmarkAction) {
                            IconButton(
                                onClick = { viewModel.toggleBookmark(uri, thumb.pageIndex) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = "Bookmark"
                                )
                            }
                        }
                    }
                    Text(
                        text = "Page ${thumb.pageIndex + 1}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
