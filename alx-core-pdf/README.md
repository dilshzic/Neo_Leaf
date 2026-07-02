# alx-core-pdf Library

`alx-core-pdf` is a powerful, reusable Android library module designed for high-performance PDF viewing, annotation, and data extraction. It leverages the modern **Jetpack PDF library** for rendering and **PDFBox-Android** for advanced document processing.

## 🚀 Features

- **Immersive Viewing**: Built-in `AlxPdfViewer` Compose component.
- **Native Annotations**: Support for high-fidelity ink, highlights, and persistent saving.
- **Standalone Extraction APIs**: Independent functions for text and image mining.
- **Structure-Aware**: Automatic TOC extraction and intelligent section range calculation.
- **Format Support**: Includes specialized decoders for **JPEG 2000 (JP2)** and **JBIG2**.
- **Fault-Tolerant**: Isolated processing ensures one corrupt element doesn't break the entire pipeline.

---

## 📦 Installation

Add the library to your project's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":alx-core-pdf"))
}
```

Ensure your `settings.gradle.kts` includes **JitPack** for image decoders:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

---

## 🛠 Usage

### 1. Initializing PDFBox
Before using any extraction features, initialize the resource loader in your `Application` class or `MainActivity`:

```kotlin
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

// ... in onCreate
PDFBoxResourceLoader.init(applicationContext)
```

### 2. PDF Viewing (Compose)
Use the `AlxPdfViewer` component to display a PDF with full annotation support:

```kotlin
import com.algorithmx.core.pdf.ui.AlxPdfViewer

@Composable
fun ReaderScreen(docUri: Uri) {
    AlxPdfViewer(
        uri = docUri,
        modifier = Modifier.fillMaxSize(),
        // Optional: Control saving and navigation via flows
        saveCommands = viewModel.saveCommands,
        jumpToPageCommands = viewModel.jumpToPageCommands
    )
}
```

### 3. Standalone Extraction
The `AlxExtractionEngine` provides standalone, thread-safe functions for mining document data.

#### Extract Text
```kotlin
val engine = AlxExtractionEngine(context)
val result = engine.extractText(uri, pageRange = listOf(0, 1, 2))

result.onSuccess { text ->
    println("Extracted: $text")
}.onFailure { error ->
    // Handle error
}
```

#### Extract Images
```kotlin
val imagesResult = engine.extractImages(uri, pageRange = null) // All pages

imagesResult.onSuccess { bitmaps ->
    // Display or share bitmaps
}
```

### 4. TOC & Structural Analysis
Extract the document outline and automatically calculated chapter ranges:

```kotlin
// Get raw TOC items
val toc = engine.extractToc(uri)

// Get intelligent sections (with start/end page ranges)
val sections = engine.extractSections(uri)
sections.forEach { section ->
    println("${section.title}: Pages ${section.startPage + 1} - ${section.endPage + 1}")
}
```

---

## 🏗 Architecture

- **`com.algorithmx.core.pdf.ui`**: Contains the `AlxPdfViewer` (Compose wrapper) and `AlxPdfFragment` (Fragment logic).
- **`com.algorithmx.core.pdf.engine`**: The `AlxExtractionEngine` which handles all background document processing.
- **`com.algorithmx.core.pdf.models`**: Unified models for extraction results and document structure.

---

## 🛡 Fault Tolerance
The library is designed for resilience. The `extractFullContent` convenience API returns an `AlxExtractionResult` containing individual error fields for text and images, allowing your app to display partial results even if some components fail.
