package com.dldev.docscanlite.core.cv

import android.graphics.Bitmap
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object PerspectiveTransformer {

    fun warp(source: Bitmap, corners: List<Point>): Bitmap {
        val src = bitmapToMat(source)

        val width = maxOf(
            distance(corners[0], corners[1]),
            distance(corners[2], corners[3])
        ).toInt()
        val height = maxOf(
            distance(corners[1], corners[2]),
            distance(corners[3], corners[0])
        ).toInt()

        val srcPoints = MatOfPoint2f(*corners.toTypedArray())
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        )

        val matrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        Imgproc.warpPerspective(src, warped, matrix, Size(width.toDouble(), height.toDouble()))

        val result = matToBitmap(warped)
        src.release(); warped.release(); matrix.release()
        return result
    }

    private fun distance(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return Math.sqrt(dx * dx + dy * dy)
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val bytes = ByteArray(pixels.size * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            bytes[i * 4]     = ((pixel shr 16) and 0xFF).toByte()
            bytes[i * 4 + 1] = ((pixel shr 8) and 0xFF).toByte()
            bytes[i * 4 + 2] = (pixel and 0xFF).toByte()
            bytes[i * 4 + 3] = ((pixel shr 24) and 0xFF).toByte()
        }
        mat.put(0, 0, bytes)
        return mat
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val cols = mat.cols()
        val rows = mat.rows()
        val bytes = ByteArray(rows * cols * 4)
        mat.get(0, 0, bytes)

        val pixels = IntArray(rows * cols)
        for (i in pixels.indices) {
            val r = bytes[i * 4].toInt() and 0xFF
            val g = bytes[i * 4 + 1].toInt() and 0xFF
            val b = bytes[i * 4 + 2].toInt() and 0xFF
            val a = bytes[i * 4 + 3].toInt() and 0xFF
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val bitmap = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, cols, 0, 0, cols, rows)
        return bitmap
    }
}
