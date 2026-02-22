package com.dldev.docscanlite.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.dldev.docscanlite.domain.model.ImageFilter

/**
 * Image processing utilities for document enhancement and memory optimization.
 *
 * This singleton provides essential bitmap operations for the scanning workflow:
 * - **Downscaling** for memory-efficient processing (captures from modern cameras are 12-108MP)
 * - **Filters** for document enhancement (grayscale, black & white threshold)
 * - **Rotation** for manual orientation correction
 *
 * **Memory Management Philosophy:**
 * Mobile cameras produce very high-resolution images (e.g., 4000x3000 = 12MP).
 * Processing such large bitmaps can cause OutOfMemoryError on budget devices.
 * Downscaling to 1600px max dimension reduces memory footprint by ~80% while preserving
 * sufficient detail for document scanning (typical A4 scan quality).
 */
object ImageProcessor {

    private const val MAX_DIMENSION = 1600
    private const val PREVIEW_ANALYSIS_DIMENSION = 800

    /**
     * Downscales a bitmap to maximum 1600px on the longest edge.
     *
     * Used for final processing and export. Maintains aspect ratio.
     *
     * **Performance:** ~50ms for a 4000x3000 bitmap on mid-range devices.
     *
     * @param source Original bitmap (potentially very large from camera)
     * @return Downscaled bitmap if needed, or original if already smaller than threshold
     */
    fun downscale(source: Bitmap): Bitmap {
        val maxDim = maxOf(source.width, source.height)
        if (maxDim <= MAX_DIMENSION) return source

        val scale = MAX_DIMENSION.toFloat() / maxDim
        val newW = (source.width * scale).toInt()
        val newH = (source.height * scale).toInt()
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    /**
     * Downscales a bitmap to maximum 800px for real-time preview analysis.
     *
     * Used for live camera preview processing where speed is critical.
     * The smaller resolution allows OpenCV detection to run at ~15-30fps.
     *
     * **Performance:** ~20ms for a 4000x3000 bitmap.
     *
     * @param source Original camera frame bitmap
     * @return Aggressively downscaled bitmap optimized for real-time processing
     */
    fun downscaleForPreview(source: Bitmap): Bitmap {
        val maxDim = maxOf(source.width, source.height)
        if (maxDim <= PREVIEW_ANALYSIS_DIMENSION) return source

        val scale = PREVIEW_ANALYSIS_DIMENSION.toFloat() / maxDim
        val newW = (source.width * scale).toInt()
        val newH = (source.height * scale).toInt()
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    /**
     * Rotates the bitmap 90 degrees clockwise.
     *
     * Used for manual orientation correction when camera orientation detection
     * fails or user scanned the document in portrait mode.
     *
     * @param source Input bitmap
     * @return New bitmap rotated 90° clockwise
     */
    fun rotate90(source: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Applies document enhancement filter to improve readability.
     *
     * **Available filters:**
     * - **ORIGINAL**: No processing (full color preservation)
     * - **GRAYSCALE**: Standard luminance-weighted conversion (0.299R + 0.587G + 0.114B)
     * - **BW (Black & White)**: Threshold-based binary conversion for high-contrast documents
     *
     * @param source Input bitmap
     * @param filter The filter type to apply
     * @return Filtered bitmap (creates new instance, original unchanged)
     */
    fun applyFilter(source: Bitmap, filter: ImageFilter): Bitmap = when (filter) {
        ImageFilter.ORIGINAL -> source
        ImageFilter.GRAYSCALE -> applyColorMatrix(source, grayscaleMatrix())
        ImageFilter.BW -> applyThreshold(source)
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun grayscaleMatrix(): ColorMatrix {
        val values = floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(values)
    }

    private fun applyThreshold(source: Bitmap): Bitmap {
        val gray = applyFilter(source, ImageFilter.GRAYSCALE)
        val pixels = IntArray(gray.width * gray.height)
        gray.getPixels(pixels, 0, gray.width, 0, 0, gray.width, gray.height)

        for (i in pixels.indices) {
            val luminance = pixels[i] and 0xFF
            pixels[i] = if (luminance > 128) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }

        val result = Bitmap.createBitmap(gray.width, gray.height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        gray.recycle()
        return result
    }
}
