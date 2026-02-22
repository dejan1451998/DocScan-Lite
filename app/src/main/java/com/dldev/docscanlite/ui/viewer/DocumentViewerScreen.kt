package com.dldev.docscanlite.ui.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.dldev.docscanlite.DocumentViewModel
import kotlinx.coroutines.launch

data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float = 8f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    viewModel: DocumentViewModel,
    documentId: Long,
    onNavigateBack: () -> Unit
) {
    val pages by viewModel.currentDocumentPages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val drawingPaths by viewModel.drawingPaths.collectAsState()
    val selectedColor by viewModel.selectedDrawingColor.collectAsState()
    val isDrawingMode by viewModel.isDrawingMode.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(documentId) {
        viewModel.loadDocumentPages(documentId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Page ${currentPageIndex + 1}/${pages.size}")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (drawingPaths.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.clearDrawings()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            viewModel.saveCurrentPageAnnotations()
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val currentPage = pages[currentPageIndex]
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    ZoomablePannableImage(
                        bitmap = currentPage.annotatedImage ?: currentPage.originalImage,
                        drawingPaths = drawingPaths,
                        isDrawingMode = isDrawingMode,
                        onDrawPath = { path ->
                            viewModel.addDrawingPath(
                                DrawingPath(
                                    points = path,
                                    color = selectedColor
                                )
                            )
                        }
                    )
                }
                
                BottomToolbar(
                    isDrawingMode = isDrawingMode,
                    selectedColor = selectedColor,
                    onToggleDrawing = { viewModel.toggleDrawingMode() },
                    onColorSelected = { viewModel.setDrawingColor(it) },
                    onPreviousPage = { 
                        if (currentPageIndex > 0) {
                            viewModel.setCurrentPageIndex(currentPageIndex - 1)
                        }
                    },
                    onNextPage = {
                        if (currentPageIndex < pages.size - 1) {
                            viewModel.setCurrentPageIndex(currentPageIndex + 1)
                        }
                    },
                    hasPrevious = currentPageIndex > 0,
                    hasNext = currentPageIndex < pages.size - 1
                )
            }
        }
    }
}

@Composable
private fun ZoomablePannableImage(
    bitmap: Bitmap,
    drawingPaths: List<DrawingPath>,
    isDrawingMode: Boolean,
    onDrawPath: (List<Offset>) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var currentDrawingPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isDrawingMode) {
                    if (isDrawingMode) {
                        detectDragGestures(
                            onDragStart = { start ->
                                currentDrawingPath = listOf(start)
                            },
                            onDrag = { change, _ ->
                                currentDrawingPath = currentDrawingPath + change.position
                            },
                            onDragEnd = {
                                if (currentDrawingPath.isNotEmpty()) {
                                    onDrawPath(currentDrawingPath)
                                    currentDrawingPath = emptyList()
                                }
                            }
                        )
                    } else {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            
                            val maxX = (containerWidth * scale - containerWidth).coerceAtLeast(0f) / 2
                            val maxY = (containerHeight * scale - containerHeight).coerceAtLeast(0f) / 2
                            
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Document page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawingPaths.forEach { path ->
                    if (path.points.size >= 2) {
                        for (i in 0 until path.points.size - 1) {
                            drawLine(
                                color = path.color,
                                start = path.points[i],
                                end = path.points[i + 1],
                                strokeWidth = path.strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
                
                if (currentDrawingPath.size >= 2) {
                    for (i in 0 until currentDrawingPath.size - 1) {
                        drawLine(
                            color = Color.Red,
                            start = currentDrawingPath[i],
                            end = currentDrawingPath[i + 1],
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomToolbar(
    isDrawingMode: Boolean,
    selectedColor: Color,
    onToggleDrawing: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean
) {
    val availableColors = listOf(
        Color.Red,
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFFEB3B),
        Color(0xFF9C27B0),
        Color.Black
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPreviousPage,
                    enabled = hasPrevious
                ) {
                    Text("Previous")
                }
                
                Button(
                    onClick = onToggleDrawing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(if (isDrawingMode) "Drawing: ON" else "Drawing: OFF")
                }
                
                Button(
                    onClick = onNextPage,
                    enabled = hasNext
                ) {
                    Text("Next")
                }
            }
            
            if (isDrawingMode) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    availableColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = color,
                                    shape = MaterialTheme.shapes.small
                                )
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.padding(4.dp)
                                    } else Modifier
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        onColorSelected(color)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
