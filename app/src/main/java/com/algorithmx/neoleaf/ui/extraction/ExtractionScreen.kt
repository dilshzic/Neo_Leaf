package com.algorithmx.neoleaf.ui.extraction

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionScreen(
    onBack: () -> Unit,
    viewModel: ExtractionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.extractData(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extraction Pipeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (uiState.isExtracting) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Extracting contents...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (uiState.text.isEmpty() && uiState.images.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Select pages to extract",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (uiState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = uiState.pageRangeInput,
                            onValueChange = { viewModel.updatePageRange(it) },
                            label = { Text("Page Range (Optional)") },
                            placeholder = { Text("e.g. 1, 3, 5-10") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("Leave blank for all pages. Use commas and dashes.")
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { launcher.launch(arrayOf("application/pdf")) },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select PDF & Extract")
                        }
                    }
                }
            } else {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Text", modifier = Modifier.padding(vertical = 12.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Images (${uiState.images.size})", modifier = Modifier.padding(vertical = 12.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    TextExtractionView(uiState.text)
                } else {
                    ImageExtractionView(uiState.images)
                }
            }
        }
    }
}

@Composable
fun TextExtractionView(text: String) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Text"))
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Share Text")
            }
            
            FloatingActionButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Extracted PDF Text", text)
                    clipboard.setPrimaryClip(clip)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Text")
            }
        }
    }
}

@Composable
fun ImageExtractionView(images: List<Bitmap>) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { bitmap ->
            Card(
                modifier = Modifier.aspectRatio(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Box {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Extracted Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { shareImage(context, bitmap) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Share Image",
                            tint = Color.White,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

private fun shareImage(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/image.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imageFile = File(cachePath, "image.png")
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(contentUri, context.contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            context.startActivity(Intent.createChooser(shareIntent, "Share image"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
