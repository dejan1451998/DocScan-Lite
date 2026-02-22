package com.dldev.docscanlite.ui.crop

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dldev.docscanlite.DocumentViewModel
import com.dldev.docscanlite.core.cv.PerspectiveTransformer
import com.dldev.docscanlite.core.image.ImageProcessor
import com.dldev.docscanlite.domain.model.ImageFilter
import org.opencv.core.Point

data class CropPoint(val x: Float, val y: Float)

@Composable
fun CropEnhanceScreen(
    viewModel: DocumentViewModel,
    onNavigateToPages: () -> Unit
) {
    val bitmap by viewModel.capturedBitmap.collectAsState()
    val detectedCorners by viewModel.detectedCorners.collectAsState()

    if (bitmap == null) return

    var compositeSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedFilter by remember { mutableStateOf(ImageFilter.ORIGINAL) }

    val initialCorners: List<CropPoint> = remember(detectedCorners, bitmap) {
        if (detectedCorners != null && bitmap != null) {
            detectedCorners!!.map {
                CropPoint(
                    x = (it.x / bitmap!!.width.toDouble()).toFloat().coerceIn(0f, 1f),
                    y = (it.y / bitmap!!.height.toDouble()).toFloat().coerceIn(0f, 1f)
                )
            }
        } else {
            listOf(
                CropPoint(0.1f, 0.1f),
                CropPoint(0.9f, 0.1f),
                CropPoint(0.9f, 0.9f),
                CropPoint(0.1f, 0.9f)
            )
        }
    }

    var corners by remember(initialCorners) { mutableStateOf(initialCorners) }

    val displayedBitmap: Bitmap = remember(bitmap, selectedFilter) {
        ImageProcessor.applyFilter(bitmap!!, selectedFilter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { compositeSize = it },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = displayedBitmap.asImageBitmap(),
                contentDescription = "Captured document",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            if (compositeSize.width > 0 && compositeSize.height > 0) {
                CropOverlay(
                    corners = corners,
                    containerSize = compositeSize,
                    imageBitmapSize = IntSize(displayedBitmap.width, displayedBitmap.height),
                    onCornersUpdated = { corners = it }
                )
            }
        }

        ControlBar(
            currentFilter = selectedFilter,
            onFilterChange = { selectedFilter = it },
            onRotate = {
                val rotated = ImageProcessor.rotate90(bitmap!!)
                viewModel.onImageCaptured(rotated)
            },
            onAutoFit = {
                if (detectedCorners != null && bitmap != null) {
                    corners = detectedCorners!!.map {
                        CropPoint(
                            x = (it.x / bitmap!!.width.toDouble()).toFloat().coerceIn(0f, 1f),
                            y = (it.y / bitmap!!.height.toDouble()).toFloat().coerceIn(0f, 1f)
                        )
                    }
                }
            },
            onApply = {
                val imageCorners = corners.map {
                    Point(it.x.toDouble() * bitmap!!.width, it.y.toDouble() * bitmap!!.height)
                }
                val cropped = PerspectiveTransformer.warp(displayedBitmap, imageCorners)
                viewModel.addPage(cropped)
                onNavigateToPages()
            }
        )
    }
}

@Composable
private fun CropOverlay(
    corners: List<CropPoint>,
    containerSize: IntSize,
    imageBitmapSize: IntSize,
    onCornersUpdated: (List<CropPoint>) -> Unit
) {
    val containerW = containerSize.width.toFloat()
    val containerH = containerSize.height.toFloat()
    val imgAspect = imageBitmapSize.width.toFloat() / imageBitmapSize.height.toFloat()
    val containerAspect = containerW / containerH

    val displayedW: Float
    val displayedH: Float
    val offsetX: Float
    val offsetY: Float
    if (containerAspect > imgAspect) {
        displayedH = containerH
        displayedW = displayedH * imgAspect
        offsetX = (containerW - displayedW) / 2f
        offsetY = 0f
    } else {
        displayedW = containerW
        displayedH = displayedW / imgAspect
        offsetX = 0f
        offsetY = (containerH - displayedH) / 2f
    }

    var dragIdx by remember { mutableStateOf(-1) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos ->
                        val threshold = 40f
                        dragIdx = corners.indices.minByOrNull { i ->
                            val px = offsetX + corners[i].x * displayedW
                            val py = offsetY + corners[i].y * displayedH
                            val dx = startPos.x - px
                            val dy = startPos.y - py
                            dx * dx + dy * dy
                        } ?: -1
                        if (dragIdx >= 0) {
                            val px = offsetX + corners[dragIdx].x * displayedW
                            val py = offsetY + corners[dragIdx].y * displayedH
                            val dist = Math.sqrt(((startPos.x - px) * (startPos.x - px) + (startPos.y - py) * (startPos.y - py)).toDouble())
                            if (dist > threshold) dragIdx = -1
                        }
                    },
                    onDrag = { _, delta ->
                        if (dragIdx in 0..3) {
                            val updated = corners.toMutableList()
                            val newX = (updated[dragIdx].x + delta.x / displayedW).coerceIn(0f, 1f)
                            val newY = (updated[dragIdx].y + delta.y / displayedH).coerceIn(0f, 1f)
                            updated[dragIdx] = CropPoint(newX, newY)
                            onCornersUpdated(updated)
                        }
                    },
                    onDragEnd = { dragIdx = -1 }
                )
            }
    ) {
        val ptsX = corners.map { offsetX + it.x * displayedW }
        val ptsY = corners.map { offsetY + it.y * displayedH }

        val edgePaint = Paint().apply {
            color = android.graphics.Color.argb(255, 0, 229, 255)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }

        val fillWhite = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }

        val fillCyan = Paint().apply {
            color = android.graphics.Color.argb(255, 0, 229, 255)
            style = Paint.Style.FILL
        }

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas

            for (i in 0..3) {
                val j = (i + 1) % 4
                nc.drawLine(ptsX[i], ptsY[i], ptsX[j], ptsY[j], edgePaint)
            }

            for (i in 0..3) {
                nc.drawCircle(ptsX[i], ptsY[i], 12f, fillWhite)
                nc.drawCircle(ptsX[i], ptsY[i], 8f, fillCyan)
            }
        }
    }
}

@Composable
private fun ControlBar(
    currentFilter: ImageFilter,
    onFilterChange: (ImageFilter) -> Unit,
    onRotate: () -> Unit,
    onAutoFit: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onRotate) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rotate 90")
            }

            Button(onClick = onAutoFit) {
                Text("Auto-fit")
            }

            FilterSelector(current = currentFilter, onSelect = onFilterChange)
        }

        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(48.dp)
        ) {
            Text("Apply & Add Page")
        }
    }
}

@Composable
private fun FilterSelector(current: ImageFilter, onSelect: (ImageFilter) -> Unit) {
    Row {
        for (filter in ImageFilter.values()) {
            val selected = filter == current
            Text(
                text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                color = if (selected) Color(0xFF6200EE) else Color.Gray,
                modifier = Modifier
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
            )
        }
    }
}
