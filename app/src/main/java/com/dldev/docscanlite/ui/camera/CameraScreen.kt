package com.dldev.docscanlite.ui.camera

import android.Manifest
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dldev.docscanlite.DocumentViewModel
import com.dldev.docscanlite.core.camera.CameraManager
import com.dldev.docscanlite.ui.camera.overlay.DocumentOverlay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: DocumentViewModel,
    onNavigateToCrop: () -> Unit,
    onNavigateToGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val autoDetect by viewModel.autoDetectEnabled.collectAsState()
    val detectedCorners by viewModel.detectedCorners.collectAsState()
    
    var frameCount by remember { mutableStateOf(0) }

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner)
    }

    var hasPermission by remember { mutableStateOf(false) }
    var shouldShowRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            shouldShowRationale = true
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        PermissionDeniedScreen(shouldShowRationale)
        return
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    cameraManager.bindCamera(preview) { bitmap ->
                        if (autoDetect) {
                            frameCount++
                            if (frameCount % 5 == 0) {
                                val downscaled = com.dldev.docscanlite.core.image.ImageProcessor.downscaleForPreview(bitmap)
                                viewModel.runDetection(downscaled)
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (autoDetect && detectedCorners != null) {
            DocumentOverlay(corners = detectedCorners!!)
        }

        CornerGuideOverlay()

        BottomControls(
            autoDetect = autoDetect,
            onToggleAutoDetect = { viewModel.toggleAutoDetect() },
            onCapture = {
                coroutineScope.launch {
                    val bitmap = cameraManager.captureImage()
                    viewModel.onImageCaptured(bitmap)
                    onNavigateToCrop()
                }
            },
            onOpenGallery = onNavigateToGallery
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.unbind()
        }
    }
}

@Composable
private fun PermissionDeniedScreen(shouldShowRationale: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (shouldShowRationale) {
                "Camera permission is required to scan documents.\nPlease grant it in app settings."
            } else {
                "Requesting camera permission..."
            },
            color = Color.White,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun CornerGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val insetX = w * 0.08f
        val insetY = h * 0.12f
        val segLen = 40f

        val paint = Paint().apply {
            color = android.graphics.Color.argb(178, 255, 255, 255)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // top-left
            nativeCanvas.drawLine(insetX, insetY, insetX + segLen, insetY, paint)
            nativeCanvas.drawLine(insetX, insetY, insetX, insetY + segLen, paint)
            // top-right
            nativeCanvas.drawLine(w - insetX - segLen, insetY, w - insetX, insetY, paint)
            nativeCanvas.drawLine(w - insetX, insetY, w - insetX, insetY + segLen, paint)
            // bottom-left
            nativeCanvas.drawLine(insetX, h - insetY - segLen, insetX, h - insetY, paint)
            nativeCanvas.drawLine(insetX, h - insetY, insetX + segLen, h - insetY, paint)
            // bottom-right
            nativeCanvas.drawLine(w - insetX, h - insetY - segLen, w - insetX, h - insetY, paint)
            nativeCanvas.drawLine(w - insetX - segLen, h - insetY, w - insetX, h - insetY, paint)
        }
    }
}

@Composable
private fun BottomControls(
    autoDetect: Boolean,
    onToggleAutoDetect: () -> Unit,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = onOpenGallery,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 48.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Text("📁", style = MaterialTheme.typography.headlineMedium)
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    "Auto-detect",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = autoDetect,
                    onCheckedChange = { onToggleAutoDetect() },
                    modifier = Modifier.padding(start = 8.dp),
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable { onCapture() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
