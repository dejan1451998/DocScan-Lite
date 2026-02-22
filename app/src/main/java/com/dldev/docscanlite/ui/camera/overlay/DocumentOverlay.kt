package com.dldev.docscanlite.ui.camera.overlay

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.layout.fillMaxSize
import org.opencv.core.Point

@Composable
fun DocumentOverlay(corners: List<Point>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val maxX = corners.maxOf { it.x }.coerceAtLeast(1.0)
        val maxY = corners.maxOf { it.y }.coerceAtLeast(1.0)

        val screenX = corners.map { ((it.x / maxX).coerceIn(0.0, 1.0) * w).toFloat() }
        val screenY = corners.map { ((it.y / maxY).coerceIn(0.0, 1.0) * h).toFloat() }

        val edgePaint = Paint().apply {
            color = android.graphics.Color.argb(204, 0, 229, 255)
            strokeWidth = 3f
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

            for (i in screenX.indices) {
                val j = (i + 1) % screenX.size
                nc.drawLine(screenX[i], screenY[i], screenX[j], screenY[j], edgePaint)
            }

            for (i in screenX.indices) {
                nc.drawCircle(screenX[i], screenY[i], 10f, fillWhite)
                nc.drawCircle(screenX[i], screenY[i], 6f, fillCyan)
            }
        }
    }
}
