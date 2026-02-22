package com.dldev.docscanlite

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dldev.docscanlite.core.cv.DocumentDetector
import com.dldev.docscanlite.core.image.ImageProcessor
import com.dldev.docscanlite.data.repository.DocumentRepository
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.dldev.docscanlite.domain.model.DocumentPage
import com.dldev.docscanlite.domain.model.ImageFilter
import com.dldev.docscanlite.domain.model.Page
import com.dldev.docscanlite.domain.model.SavedDocument
import com.dldev.docscanlite.ui.viewer.DrawingPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.core.Point
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint

/**
 * ViewModel responsible for managing the complete document scanning workflow.
 *
 * This ViewModel orchestrates the following features:
 * - **Camera capture** and automatic document edge detection
 * - **Page management** (add, delete, reorder) for multi-page documents
 * - **Document gallery** persistence using Room database
 * - **Annotation system** for drawing on scanned pages
 * - **PDF export** functionality
 *
 * The ViewModel follows MVVM architecture pattern and uses StateFlow for reactive UI updates.
 * All I/O operations are executed on background threads via coroutines.
 *
 * @property repository Data layer abstraction for database operations
 */
class DocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _detectedCorners = MutableStateFlow<List<Point>?>(null)
    val detectedCorners: StateFlow<List<Point>?> = _detectedCorners.asStateFlow()

    private val _autoDetectEnabled = MutableStateFlow(true)
    val autoDetectEnabled: StateFlow<Boolean> = _autoDetectEnabled.asStateFlow()

    private val _exportUri = MutableStateFlow<android.net.Uri?>(null)
    val exportUri: StateFlow<android.net.Uri?> = _exportUri.asStateFlow()

    private var nextPageId = 1

    /**
     * Toggles automatic document edge detection on/off during camera preview.
     *
     * When enabled, the app continuously runs OpenCV contour detection on camera frames.
     * When disabled, users must manually adjust crop corners after capture.
     */
    fun toggleAutoDetect() {
        _autoDetectEnabled.value = !_autoDetectEnabled.value
    }

    /**
     * Processes a newly captured image from the camera.
     *
     * **Processing pipeline:**
     * 1. Downscales the raw bitmap to max 1600px (memory optimization)
     * 2. Stores the processed bitmap in state
     * 3. If auto-detect is enabled, triggers document edge detection
     *
     * @param raw The captured image bitmap (typically high resolution from camera)
     */
    fun onImageCaptured(raw: Bitmap) {
        val downscaled = ImageProcessor.downscale(raw)
        _capturedBitmap.value = downscaled
        _detectedCorners.value = null

        if (_autoDetectEnabled.value) {
            runDetection(downscaled)
        }
    }

    /**
     * Executes OpenCV document detection algorithm on the given bitmap.
     *
     * The detection uses a multi-stage pipeline:
     * - Grayscale conversion → Gaussian blur → Canny edge detection → Contour finding
     *
     * Results are stored in [detectedCorners] StateFlow and can be null if no valid
     * document contour is found.
     *
     * @param bitmap The image to analyze for document edges
     */
    fun runDetection(bitmap: Bitmap) {
        val result = DocumentDetector.detect(bitmap)
        _detectedCorners.value = result?.corners
    }

    /**
     * Adds a processed page to the current multi-page document.
     *
     * The page is assigned a unique incremental ID and appended to the page list.
     * After adding, the capture state is cleared to allow scanning the next page.
     *
     * @param bitmap The final processed bitmap (after crop, perspective correction, filters)
     */
    fun addPage(bitmap: Bitmap) {
        val page = Page(id = nextPageId++, bitmap = bitmap)
        _pages.value = _pages.value + page
        _capturedBitmap.value = null
        _detectedCorners.value = null
    }

    /**
     * Removes a page from the current document by its ID.
     *
     * @param pageId The unique identifier of the page to delete
     */
    fun deletePage(pageId: Int) {
        _pages.value = _pages.value.filter { it.id != pageId }
    }

    /**
     * Reorders pages within the current document via drag-and-drop.
     *
     * @param from The current index position of the page
     * @param to The target index position
     */
    fun reorderPages(from: Int, to: Int) {
        val list = _pages.value.toMutableList()
        val moved = list.removeAt(from)
        list.add(to, moved)
        _pages.value = list
    }

    fun setExportUri(uri: android.net.Uri?) {
        _exportUri.value = uri
    }

    /**
     * Clears the current capture state (bitmap and detected corners).
     *
     * Typically called when navigating back to camera from crop screen.
     */
    fun clearCapture() {
        _capturedBitmap.value = null
        _detectedCorners.value = null
    }

    /**
     * Persists the current multi-page document to the local Room database.
     *
     * **Behavior:**
     * - Generates a timestamped title ("Scan YYYY-MM-DD HH:mm")
     * - Saves all page bitmaps to database via repository
     * - Clears the in-memory page list after successful save
     *
     * This operation runs asynchronously on a background coroutine.
     * No-op if the page list is empty.
     */
    fun saveDocumentToGallery() {
        viewModelScope.launch {
            val pageBitmaps = _pages.value.map { it.bitmap }
            if (pageBitmaps.isEmpty()) return@launch

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val title = "Scan ${dateFormat.format(Date())}"

            repository.saveDocument(title, pageBitmaps)

            _pages.value = emptyList()
            nextPageId = 1
        }
    }
    
    val savedDocuments: StateFlow<List<SavedDocument>> = 
        MutableStateFlow(emptyList())
    
    private val _currentDocumentPages = MutableStateFlow<List<DocumentPage>>(emptyList())
    val currentDocumentPages: StateFlow<List<DocumentPage>> = _currentDocumentPages.asStateFlow()
    
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()
    
    private val _drawingPaths = MutableStateFlow<List<DrawingPath>>(emptyList())
    val drawingPaths: StateFlow<List<DrawingPath>> = _drawingPaths.asStateFlow()
    
    private val _selectedDrawingColor = MutableStateFlow(Color.Red)
    val selectedDrawingColor: StateFlow<Color> = _selectedDrawingColor.asStateFlow()
    
    private val _isDrawingMode = MutableStateFlow(false)
    val isDrawingMode: StateFlow<Boolean> = _isDrawingMode.asStateFlow()
    
    private var currentDocumentId: Long = 0
    
    init {
        viewModelScope.launch {
            repository.getAllDocuments().collect { documents ->
                (savedDocuments as MutableStateFlow).value = documents
            }
        }
    }
    
    fun loadDocumentPages(documentId: Long) {
        currentDocumentId = documentId
        viewModelScope.launch {
            repository.getPagesByDocumentId(documentId).collect { pages ->
                _currentDocumentPages.value = pages
                if (pages.isNotEmpty() && _currentPageIndex.value >= pages.size) {
                    _currentPageIndex.value = 0
                }
            }
        }
    }
    
    fun setCurrentPageIndex(index: Int) {
        _currentPageIndex.value = index
        _drawingPaths.value = emptyList()
    }
    
    fun toggleDrawingMode() {
        _isDrawingMode.value = !_isDrawingMode.value
    }
    
    fun setDrawingColor(color: Color) {
        _selectedDrawingColor.value = color
    }
    
    fun addDrawingPath(path: DrawingPath) {
        _drawingPaths.value = _drawingPaths.value + path
    }
    
    fun clearDrawings() {
        _drawingPaths.value = emptyList()
    }
    
    fun saveCurrentPageAnnotations() {
        viewModelScope.launch {
            val pages = _currentDocumentPages.value
            if (pages.isEmpty()) return@launch
            
            val currentPage = pages[_currentPageIndex.value]
            val paths = _drawingPaths.value
            
            if (paths.isEmpty()) return@launch
            
            val annotatedBitmap = createAnnotatedBitmap(
                currentPage.annotatedImage ?: currentPage.originalImage,
                paths
            )
            
            repository.updatePageAnnotation(currentPage.id, annotatedBitmap)
            _drawingPaths.value = emptyList()
        }
    }
    
    private fun createAnnotatedBitmap(baseBitmap: Bitmap, paths: List<DrawingPath>): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(mutableBitmap)
        
        paths.forEach { path ->
            val paint = AndroidPaint().apply {
                color = android.graphics.Color.argb(
                    (path.color.alpha * 255).toInt(),
                    (path.color.red * 255).toInt(),
                    (path.color.green * 255).toInt(),
                    (path.color.blue * 255).toInt()
                )
                strokeWidth = path.strokeWidth
                style = AndroidPaint.Style.STROKE
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
                isAntiAlias = true
            }
            
            if (path.points.size >= 2) {
                for (i in 0 until path.points.size - 1) {
                    canvas.drawLine(
                        path.points[i].x,
                        path.points[i].y,
                        path.points[i + 1].x,
                        path.points[i + 1].y,
                        paint
                    )
                }
            }
        }
        
        return mutableBitmap
    }
}
