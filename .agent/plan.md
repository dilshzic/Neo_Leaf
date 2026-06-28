# Project Plan

Refactor the Neo Leaf PDF functionality into a reusable library module named "alx-core-pdf" that provides viewing, navigation, and extraction tools.

## Project Brief

# alx-core-pdf Library Brief

`alx-core-pdf` is a reusable Android library module designed to provide comprehensive PDF viewing and data extraction capabilities across multiple applications.

## Core Features
- **Jetpack PDF Viewer**: A ready-to-use Compose component wrapping `androidx.pdf` for high-fidelity document viewing.
- **Programmatic Navigation**: API to navigate to specific pages within the viewer.
- **Advanced Extraction**: A standalone engine using `pdfbox-android` to extract text (entire doc, specific pages, or ranges) and images from PDF documents.
- **Document Metadata**: Extract hierarchical Table of Contents (TOC) and generate page thumbnails.

## Technical Foundation
- **Jetpack PDF Library**: Core rendering and native annotations.
- **PDFBox-Android**: Advanced extraction and metadata parsing.
- **Jetpack Compose**: Modern UI components for the viewer.
- **Kotlin Coroutines**: Non-blocking document processing.

## Module Architecture
The library will be decoupled from the application logic, exposing clean interfaces for document loading, UI rendering, and data extraction.

## Implementation Steps

### Task_1_Foundations_SAF: Setup project architecture, Material 3 theme, and integrate Storage Access Framework (SAF) for PDF file selection. Add the necessary androidx.pdf dependencies to build.gradle.kts.
- **Status:** COMPLETED
- **Updates:** I have completed the foundational setup for Neo Leaf. Key accomplishments:
- **Acceptance Criteria:**
  - Project builds successfully
  - Material 3 color scheme (light/dark) implemented
  - PDF file picker launches and returns a URI
  - androidx.pdf dependency integrated

### Task_2_PDF_Viewer_Adaptive_UI: Implement the core PDF viewer using the Jetpack PDF library (PdfViewerFragment or equivalent). Build an adaptive UI with a sidebar/rail for document navigation (TOC/Thumbnails) using Compose Adaptive components.
- **Status:** COMPLETED
- **Updates:** Implemented core PDF viewer and adaptive UI:
- **Acceptance Criteria:**
  - PDF renders smoothly with scrolling and zoom support
  - Adaptive sidebar/rail responds to screen size changes
  - Table of Contents and Thumbnails are functional
  - Edge-to-edge display active

### Task_3_Annotations_Markup: Develop the annotation engine to support text highlighting, underlining, and adding sticky notes to the PDF. Ensure annotation data is persisted.
- **Status:** COMPLETED
- **Updates:** Implemented the annotation and markup engine:
- **Acceptance Criteria:**
  - User can highlight and underline text selections
  - Sticky notes can be placed and edited
  - Annotations are saved and visible upon reopening the document

### Task_4_Polishing_Icon_Verification: Create and integrate an adaptive app icon. Refine M3 UI components and styling. Perform a final stability run and verify the app against the project brief.
- **Status:** COMPLETED
- **Updates:** Final verification successful for saving and navigation:
- **Acceptance Criteria:**
  - Adaptive app icon implemented
  - Consistent Material 3 vibrant aesthetic applied
  - App does not crash during standard usage
  - Critic verifies stability and requirement alignment

### Task_5_Extraction_TOC: Integrate PDFBox-Android to implement the extraction pipeline (batch text and image extraction) and extract hierarchical document outlines (TOC).
- **Status:** COMPLETED
- **Updates:** Completed the Extraction Pipeline:
- **Acceptance Criteria:**
  - PDFBox-Android dependency added
  - Text and image extraction pipeline functional
  - Hierarchical TOC extracted and displayed

### Task_6_Library_Module_Migration: Create the 'alx-core-pdf' Android library module. Migrate the Jetpack PDF viewer, PDFBox extraction engine, and TOC/Thumbnail extraction logic into the module, exposing them via clean internal and public APIs.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - 'alx-core-pdf' module created and configured
  - PDF viewing and extraction logic migrated successfully
  - Public APIs for document loading and data extraction defined
  - Library module builds independently
- **StartTime:** 2026-06-28 19:10:45 IST

### Task_7_App_Refactor_Verification: Refactor the Neo Leaf app to consume the 'alx-core-pdf' library. Implement the programmatic navigation API within the library and update the app to use it. Conduct final verification for stability and feature parity.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Neo Leaf app successfully integrates 'alx-core-pdf'
  - Programmatic page navigation functional via library API
  - All extraction and viewing features operational
  - App builds and does not crash
  - Make sure all existing tests pass

