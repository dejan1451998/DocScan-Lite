# DocScan Lite

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

DocScan Lite is a lightweight Android document scanner that detects paper edges, corrects perspective, and exports clean multi-page PDFs. It’s built as a portfolio project to demonstrate modern Android development and practical computer vision.

**Tech Stack:** Jetpack Compose • CameraX • OpenCV • Room • Material 3 • MVVM • Coroutines

---

## Features

| Feature | Status | Description |
|---------|--------|-------------|
| **Live edge detection** | ✅ | Real-time document boundary detection during camera preview |
| **Auto perspective correction** | ✅ | Automatic deskewing using 4-point perspective transform |
| **Manual crop adjustment** | ✅ | Draggable corner handles for precise edge refinement |
| **Image filters** | ✅ | Original (color) / Grayscale / Black & White threshold |
| **Multi-page documents** | ✅ | Combine multiple scans into a single document |
| **Page reordering** | ✅ | Drag-and-drop to rearrange pages |
| **PDF export** | ✅ | Export to user-selected location via Storage Access Framework |
| **Local gallery** | ✅ | View saved documents with thumbnails & metadata |
| **Annotations** | ✅ | Draw on scanned pages with color selection |
| **Dark mode** | ✅ | Material 3 dynamic theming with dark mode support |
| **OCR text extraction** | ⏳ | Planned (ML Kit Text Recognition integration) |
| **Import from gallery** | ⏳ | Planned (process existing photos) |
| **Cloud backup** | ❌ | Not planned (focus on local-first privacy) |

---

## Why this project

This sample focuses on the parts of Android development that matter in production:
- Camera lifecycle, permissions, and device variability
- Real-time processing constraints (performance and memory)
- A real computer-vision pipeline (OpenCV)
- Reliable export using the Storage Access Framework + PDF generation
- Clean architecture, readable code, and predictable state management

---

## How it works (high level)

### Live preview (best effort)
1. Convert frame to grayscale
2. Blur to reduce noise
3. Canny edge detection
4. Find contours and select the best 4-point candidate (document)
5. Render corner/outline overlay on top of the preview

### After capture
1. Use detected (or manually adjusted) 4 points
2. Apply perspective transform (`warpPerspective`) to deskew
3. Apply selected enhancement filter
4. Save the page and generate a PDF on export

---

## Screens & flow

1) **Camera**
- Live preview
- Auto-detect toggle
- Capture

2) **Crop & Enhance**
- Adjustable 4 corners (always available)
- Auto-fit (use detected points)
- Rotate 90°
- Filters

3) **Pages**
- Thumbnails
- Reorder / delete
- Export PDF

4) **Gallery**
- View saved documents (local only)

---

## Tech stack

- **Jetpack Compose + Material 3** — UI
- **CameraX** — camera preview and capture
- **OpenCV** — document detection and perspective correction
- **Room** — local persistence (documents/pages metadata)
- **Coroutines + Flow/StateFlow** — async work and UI state
- **Storage Access Framework (SAF)** — user-selected save location
- **MVVM** — separation of concerns and predictable UI state

---

## Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
com.dldev.docscanlite/
├── core/                    # Business Logic Layer (Framework-agnostic)
│   ├── camera/              # CameraX integration (lifecycle, capture, preview)
│   ├── cv/                  # OpenCV document detection + corner ordering
│   ├── image/               # Image processing (filters, rotation, scaling)
│   ├── pdf/                 # PDF generation (PdfDocument API)
│   └── storage/             # Storage Access Framework helpers
│
├── data/                    # Data Layer
│   ├── local/               # Room database (entities, DAO, converters)
│   └── repository/          # Repository pattern (abstracts data sources)
│
├── domain/                  # Domain Layer (Pure Kotlin)
│   └── model/               # Domain models (Page, Document, ImageFilter)
│
└── ui/                      # Presentation Layer (Jetpack Compose)
    ├── camera/              # Camera preview screen with live detection
    ├── crop/                # Crop & enhancement screen
    ├── pages/               # Multi-page document management
    ├── gallery/             # Saved documents gallery
    ├── viewer/              # Document viewer with annotations
    └── theme/               # Material 3 theming

MainActivity.kt              # Navigation host & app entry point
DocumentViewModel.kt         # Central ViewModel (MVVM pattern)
```

### Key Design Patterns
- **MVVM** - ViewModel manages UI state via StateFlow
- **Repository Pattern** - Abstract data access (Room database)
- **Singleton** - Stateless utility classes (DocumentDetector, ImageProcessor, PdfExporter)
- **Reactive Streams** - Kotlin Flow for reactive data updates


---

## Installation

### Option 1: Download APK (Recommended for Quick Testing)
> **TODO:** [Download latest release APK](https://github.com/dejan1451998/DocScanLite/releases) *(coming soon)*

1. Download the APK from releases
2. Enable "Install from Unknown Sources" in Android settings
3. Open the APK file and install
4. Grant camera permission when prompted

### Option 2: Build from Source

**Requirements:**
- Android Studio (latest stable)
- JDK 17 or higher
- Android SDK with API 36
- Physical Android device recommended (camera + real performance)

**Steps:**
1. Clone the repository:
   ```bash
   git clone https://github.com/dejan1451998/DocScanLite.git
   cd DocScanLite
   ```

2. Open the project in Android Studio and wait for Gradle sync.

3. Connect your Android device via USB (enable Developer Mode + USB Debugging)

4. Run the app:
   - Click "Run" (Shift+F10) in Android Studio, OR
   - Build APK manually:
     ```bash
     ./gradlew assembleDebug
     ```
     APK location: `app/build/outputs/apk/debug/app-debug.apk`

5. Grant camera permission when the app launches.


## Performance

Benchmarked on mid-range Android device (2023, Snapdragon 7 Gen 1):

| Operation | Average Time | Notes |
|-----------|-------------|-------|
| **Document detection** | ~150ms | Full OpenCV pipeline (grayscale → blur → Canny → contours) |
| **Perspective transform** | ~80ms | warpPerspective on 1600px image |
| **Bitmap downscaling** | ~50ms | 4000x3000 → 1600x1200 |
| **Filter application** | ~30ms | Grayscale or B&W threshold |
| **PDF export (5 pages)** | ~2s | Includes bitmap rendering to PDF canvas |

**Memory Optimization:**
- Camera captures downscaled from 12MP+ to 1.92MP (1600px max) = **~80% memory reduction**
- Preview analysis uses 800px max for real-time performance = **~95% reduction**
- No memory leaks (Mat objects properly released after OpenCV operations)

**Reliability Notes:**
- Detection runs off the main thread (no UI freezing)
- Best-effort algorithm (may fail on low contrast, reflective surfaces, or complex backgrounds)
- Manual corner adjustment always available as fallback
- Tested on devices from Android 8.0 (SDK 26) to Android 15 (SDK 36)

## Roadmap & Future Enhancements

### Planned Features
- [ ] **OCR text extraction** - ML Kit Text Recognition for searchable PDFs
- [ ] **Import from gallery** - Process existing photos from device storage
- [ ] **Advanced filters** - Adaptive thresholding, shadow removal, color correction
- [ ] **Batch processing** - Scan multiple pages with auto-capture
- [ ] **Document templates** - Presets for receipts, business cards, whiteboards

### Known Limitations
- **Detection accuracy**: Best-effort algorithm; may fail on:
  - Low contrast documents (white paper on white background)
  - Reflective/glossy surfaces
  - Complex backgrounds with multiple edges
  - Very small documents (<15% of frame)
- **No cloud sync**: Intentionally local-first for privacy
- **No search**: Without OCR, documents are not searchable by content

## Tests

```bash
./gradlew test
```

Unit tests focus on deterministic logic:
- Corner point ordering
- Candidate selection logic (pure functions)

---

## Contributing

This is a portfolio project, but feedback and suggestions are welcome!

If you find a bug or have a feature request:
1. Open an issue with detailed description
2. For code contributions, fork the repo and submit a pull request

Please maintain the existing code style and architecture patterns.

---

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

You are free to use this code for learning, portfolio projects, or commercial applications.

---

## Contact

**Dejan Lazarov**
- 💼 LinkedIn: [linkedin.com/in/dejan-l-a9a5b519b/](https://www.linkedin.com/in/dejan-l-a9a5b519b/)
- 📧 Email: dejanlazarov98@gmail.com
- 🐙 GitHub: [github.com/dejan1451998](https://github.com/dejan1451998)

---

<p align="center">⭐ If this project helped you learn Android development, consider giving it a star!</p>