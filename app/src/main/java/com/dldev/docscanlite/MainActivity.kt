package com.dldev.docscanlite

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dldev.docscanlite.core.storage.ExportManager
import com.dldev.docscanlite.ui.camera.CameraScreen
import com.dldev.docscanlite.ui.crop.CropEnhanceScreen
import com.dldev.docscanlite.ui.pages.PagesScreen
import com.dldev.docscanlite.ui.gallery.GalleryScreen
import com.dldev.docscanlite.ui.viewer.DocumentViewerScreen
import com.dldev.docscanlite.ui.theme.DocScanLiteTheme
import org.opencv.android.OpenCVLoader
import com.dldev.docscanlite.data.local.AppDatabase
import com.dldev.docscanlite.data.repository.DocumentRepository

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DocumentViewModel

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            val pages = viewModel.pages.value
            if (pages.isNotEmpty()) {
                ExportManager.writePdf(this, uri, pages)
                viewModel.setExportUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!OpenCVLoader.initLocal()) {
            throw RuntimeException("OpenCV initialization failed")
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DocumentRepository(
            savedDocumentDao = database.savedDocumentDao(),
            documentPageDao = database.documentPageDao()
        )
        viewModel = DocumentViewModel(repository)
        
        enableEdgeToEdge()
        setContent {
            DocScanLiteTheme {
                AppNavigation(
                    viewModel = viewModel,
                    onRequestExportUri = { exportLauncher.launch("DocScanLite_Export.pdf") }
                )
            }
        }
    }
}

private enum class Screen { CAMERA, CROP, PAGES, GALLERY, VIEWER }

@Composable
private fun AppNavigation(
    viewModel: DocumentViewModel,
    onRequestExportUri: () -> Unit
) {
    val pages by viewModel.pages.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()

    var currentScreen by remember {
        mutableIntStateOf(if (pages.isEmpty()) Screen.CAMERA.ordinal else Screen.PAGES.ordinal)
    }
    
    var selectedDocumentId by remember { mutableStateOf(0L) }

    val screen = Screen.values()[currentScreen]

    BackHandler(enabled = screen != Screen.CAMERA) {
        currentScreen = when (screen) {
            Screen.CROP -> Screen.CAMERA.ordinal
            Screen.PAGES -> Screen.CAMERA.ordinal
            Screen.GALLERY -> Screen.CAMERA.ordinal
            Screen.VIEWER -> Screen.GALLERY.ordinal
            Screen.CAMERA -> Screen.CAMERA.ordinal
        }
    }

    when (screen) {
        Screen.CAMERA -> CameraScreen(
            viewModel = viewModel,
            onNavigateToCrop = { currentScreen = Screen.CROP.ordinal },
            onNavigateToGallery = { currentScreen = Screen.GALLERY.ordinal }
        )
        Screen.CROP -> {
            if (capturedBitmap == null) {
                currentScreen = Screen.CAMERA.ordinal
            } else {
                CropEnhanceScreen(
                    viewModel = viewModel,
                    onNavigateToPages = { currentScreen = Screen.PAGES.ordinal }
                )
            }
        }
        Screen.PAGES -> PagesScreen(
            viewModel = viewModel,
            onNavigateToCamera = { currentScreen = Screen.CAMERA.ordinal },
            onRequestExportUri = onRequestExportUri,
            onNavigateToGallery = { currentScreen = Screen.GALLERY.ordinal }
        )
        Screen.GALLERY -> GalleryScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.CAMERA.ordinal },
            onDocumentClick = { documentId ->
                selectedDocumentId = documentId
                currentScreen = Screen.VIEWER.ordinal
            }
        )
        Screen.VIEWER -> DocumentViewerScreen(
            viewModel = viewModel,
            documentId = selectedDocumentId,
            onNavigateBack = { currentScreen = Screen.GALLERY.ordinal }
        )
    }
}
