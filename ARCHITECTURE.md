# Architecture Documentation

## Overview

DocScanLite follows **Clean Architecture** principles with clear separation between layers. This document explains the architectural decisions, design patterns, and data flow.

---

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│              (Jetpack Compose + ViewModel)              │
├─────────────────────────────────────────────────────────┤
│                      Domain Layer                       │
│                  (Pure Kotlin Models)                   │
├─────────────────────────────────────────────────────────┤
│                       Data Layer                        │
│              (Repository + Room Database)               │
├─────────────────────────────────────────────────────────┤
│                    Business Logic                       │
│        (OpenCV, CameraX, PDF, Image Processing)         │
└─────────────────────────────────────────────────────────┘
```

### 1. Presentation Layer (`ui/`)
**Responsibility:** User interface and user interaction

- **Technology:** Jetpack Compose (declarative UI)
- **Pattern:** MVVM (Model-View-ViewModel)
- **State Management:** StateFlow for reactive updates
- **Navigation:** Compose Navigation

**Screens:**
- `CameraScreen` - Live preview with auto-detection toggle
- `CropScreen` - 4-corner adjustment and filter selection
- `PagesScreen` - Multi-page management with drag-to-reorder
- `GalleryScreen` - Saved documents list
- `ViewerScreen` - Document viewer with annotation support

**ViewModel:**
- `DocumentViewModel` - Single source of truth for UI state
- Orchestrates business logic (camera, detection, storage)
- Exposes StateFlow for reactive UI updates
- Handles all user actions (capture, crop, save, export)

### 2. Domain Layer (`domain/`)
**Responsibility:** Business entities (pure Kotlin, framework-agnostic)

**Models:**
- `Page` - Represents a single scanned page (in-memory)
- `DocumentPage` - Persistent page entity with annotations
- `SavedDocument` - Document metadata (title, page count, timestamp)
- `ImageFilter` - Enum for filter types (ORIGINAL, GRAYSCALE, BW)

**Why separate domain models?**
- Decouples business logic from database implementation
- Easier to test (no Android dependencies)
- Can change persistence strategy without affecting core logic

### 3. Data Layer (`data/`)
**Responsibility:** Data persistence and access

**Repository Pattern:**
- `DocumentRepository` - Abstracts data source (Room database)
- Provides clean API for ViewModel (no direct DAO access)
- Converts between domain models and database entities

**Room Database:**
- `SavedDocumentEntity` - Document table
- `DocumentPageEntity` - Page table (1:N relationship with documents)
- `BitmapTypeConverter` - Serializes bitmaps to ByteArray for storage

**Why Repository Pattern?**
- Single point of truth for data access
- Easy to swap implementations (Room → Firebase, etc.)
- Simplifies testing (mock repository instead of entire database)

### 4. Business Logic (`core/`)
**Responsibility:** Application-specific logic and external integrations

**Modules:**

#### `core/camera/`
**CameraManager:**
- CameraX integration (preview + image capture)
- Camera lifecycle management
- Frame-by-frame analysis for live detection

**Key Challenges Solved:**
- Memory management (bounded executor with limited threads)
- Camera rotation handling
- Frame throttling (avoid overloading detection pipeline)

#### `core/cv/` (Computer Vision)
**DocumentDetector:**
- OpenCV integration for edge detection
- Multi-stage pipeline: Grayscale → Blur → Canny → Contours
- Quadrilateral selection (largest 4-point contour >15% of image)
- Point ordering algorithm (consistent TL→TR→BR→BL)

**Performance Optimizations:**
- Downscale to 800px for preview analysis (95% memory reduction)
- Reuse Mat objects where possible
- Proper resource cleanup (Mat.release())

#### `core/image/`
**ImageProcessor:**
- Bitmap downscaling (memory optimization)
- Filter application (grayscale, B&W threshold)
- Image rotation (90° increments)

**Filter Implementations:**
- Grayscale: Luminance-weighted (0.299R + 0.587G + 0.114B)
- B&W: Threshold-based binary conversion (threshold=128)

#### `core/pdf/`
**PdfExporter:**
- Android PdfDocument API integration
- Multi-page PDF generation
- Bitmap rendering to PDF canvas

#### `core/storage/`
**StorageHelper:**
- Storage Access Framework (SAF) integration
- User-selected save location
- PDF sharing intent

---

## Data Flow

### Scanning Workflow

```
User Action                ViewModel                    Core Logic
───────────                ─────────                    ──────────
[Capture] ────────────────> onImageCaptured()
                            │
                            ├─> ImageProcessor.downscale()
                            │
                            └─> runDetection() ────────> DocumentDetector.detect()
                                                         │
                                                         ├─> bitmapToMat()
                                                         ├─> cvtColor (grayscale)
                                                         ├─> GaussianBlur
                                                         ├─> Canny (edge detect)
                                                         ├─> findContours
                                                         ├─> selectLargestQuad
                                                         └─> orderPoints

                            Update _detectedCorners StateFlow
                                      │
                                      └───> UI observes & renders corner handles


[Add Page] ────────────────> addPage(bitmap)
                            │
                            └─> pages.value += Page(id, bitmap)
                                      │
                                      └───> UI shows thumbnail in gallery


[Export PDF] ──────────────> exportPdf(uri)
                            │
                            └─> PdfExporter.export() ───> PdfDocument.writeTo(stream)
```

### Database Persistence

```
User Action                ViewModel                    Data Layer
───────────                ─────────                    ──────────
[Save] ────────────────────> saveDocumentToGallery()
                            │
                            └─> repository.saveDocument(title, bitmaps)
                                      │
                                      ├─> Insert SavedDocumentEntity
                                      │   (generate ID, timestamp)
                                      │
                                      └─> Insert DocumentPageEntity × N
                                          (serialize bitmaps)

                            Clear in-memory pages

                            Repository.getAllDocuments()
                                      │
                                      └───> Flow<List<SavedDocument>>
                                                  │
                                                  └───> UI observes & displays gallery
```

---

## Design Patterns

### 1. MVVM (Model-View-ViewModel)
**Why:** Separates UI logic from business logic
- **Model:** Domain models + Repository
- **View:** Composable functions (screens)
- **ViewModel:** State management + orchestration

**Benefits:**
- Testable (ViewModel is pure Kotlin)
- Survives configuration changes (ViewModel lifecycle)
- Reactive UI updates (StateFlow)

### 2. Repository Pattern
**Why:** Abstract data access layer
- Hides implementation details (Room, SharedPreferences, network)
- Single source of truth
- Easy to test (mock repository)

### 3. Singleton Objects
**Why:** Stateless utility functions (no need for DI complexity)
- `DocumentDetector`, `ImageProcessor`, `PdfExporter`
- Thread-safe (Kotlin object initialization)
- Simple to use (no instance creation)

### 4. StateFlow (Reactive Streams)
**Why:** Reactive UI updates
- ViewModel exposes StateFlow
- Composables collect and react to state changes
- Lifecycle-aware (no memory leaks)

---

## Key Architectural Decisions

### Decision 1: Single ViewModel vs Multiple ViewModels
**Choice:** Single `DocumentViewModel` for entire app

**Rationale:**
- Shared state across screens (pages list, captured bitmap)
- Simpler navigation (no need to pass data between ViewModels)
- Smaller codebase (less boilerplate)

**Trade-off:**
- ViewModel grows large (~220 lines)
- Could be split later if complexity increases

### Decision 2: Room Database for Local Storage
**Choice:** Room over SharedPreferences/File storage

**Rationale:**
- Structured queries (get documents by ID, timestamp)
- Type-safe DAO
- Built-in LiveData/Flow support
- Easier to add features (search, filtering)

**Trade-off:**
- Bitmap serialization overhead (ByteArray conversion)
- Database size can grow large

### Decision 3: Bitmap Storage in Database
**Choice:** Store bitmaps as ByteArray in Room (not file paths)

**Rationale:**
- Atomic operations (delete document = delete all pages)
- No orphaned files
- Simpler permission handling (no external storage access)

**Trade-off:**
- Database size (10 pages × 500KB = 5MB per document)
- Slower queries for large documents

**Mitigation:**
- Could migrate to hybrid approach (store paths for large documents)

### Decision 4: No Dependency Injection
**Choice:** Manual dependency passing (no Hilt/Koin)

**Rationale:**
- Portfolio project (simpler for recruiters to understand)
- Small codebase (only 1 ViewModel, 1 Repository)
- Easier to test (simple constructor injection)

**Trade-off:**
- Manual instantiation in MainActivity
- Would not scale to large apps

---

## Performance Considerations

### Memory Management
**Challenge:** Camera captures are 12MP+ (48MB in memory)

**Solutions:**
1. **Aggressive downscaling:**
   - Preview analysis: 800px (95% reduction)
   - Final processing: 1600px (80% reduction)

2. **OpenCV Mat cleanup:**
   - Explicit `mat.release()` after every operation
   - Prevents native memory leaks

3. **Bitmap recycling:**
   - Recycle intermediate bitmaps (e.g., after filter application)

### Real-Time Detection Performance
**Challenge:** Run OpenCV pipeline at 15-30fps on camera preview

**Solutions:**
1. **Frame throttling:**
   - Analyze every 3rd frame (not every frame)
   - Skip analysis if previous detection still running

2. **Background processing:**
   - Run detection on background thread (coroutines)
   - Never block main thread

3. **Early termination:**
   - Skip small contours immediately (area < 15%)
   - Reduces unnecessary polygon approximation

---

## Testing Strategy

### Unit Tests
**What to test:**
- Pure functions in `core/` (DocumentDetector, ImageProcessor)
- Corner ordering algorithm (deterministic)
- Quad selection logic

**Example:**
```kotlin
@Test
fun `orderPoints should return TL-TR-BR-BL sequence`() {
    val unordered = listOf(
        Point(100.0, 100.0), // BR
        Point(0.0, 0.0),     // TL
        Point(100.0, 0.0),   // TR
        Point(0.0, 100.0)    // BL
    )
    val ordered = DocumentDetector.orderPoints(unordered)
    assertEquals(Point(0.0, 0.0), ordered[0]) // TL
}
```

### Integration Tests (TODO)
**What to test:**
- Repository + Room database
- ViewModel state transitions
- Camera capture flow

### UI Tests (TODO)
**What to test:**
- Screen navigation
- Permission handling
- PDF export flow

---

## Future Improvements

### Architecture Enhancements
1. **Modularization:**
   - Extract `core/` into separate Gradle module
   - Faster build times + reusability

2. **Dependency Injection:**
   - Add Hilt when ViewModel count grows
   - Better testability

3. **Use Cases Layer:**
   - Extract complex logic from ViewModel (ScanDocumentUseCase, ExportPdfUseCase)
   - Single Responsibility Principle

4. **Error Handling:**
   - Sealed class for Result types (Success, Error, Loading)
   - Propagate errors to UI gracefully

### Performance Enhancements
1. **Hybrid Storage:**
   - Store small bitmaps in Room
   - Store large bitmaps as files (with DB references)

2. **Background PDF Generation:**
   - Use WorkManager for long-running exports
   - Show notification when complete

3. **Caching:**
   - Cache filtered bitmaps (avoid re-processing on rotation)
   - LRU cache for thumbnails

---

## Conclusion

This architecture prioritizes:
- **Clarity** - Easy for portfolio reviewers to understand
- **Testability** - Pure functions and repository pattern
- **Performance** - Memory optimization and background processing
- **Maintainability** - Clear separation of concerns

The design is intentionally simple (no over-engineering) while demonstrating
production-ready Android development practices.
