package com.dldev.docscanlite.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    fun bindCamera(previewView: PreviewView, onFrameAnalyzed: (Bitmap) -> Unit = {}) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider

        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        imageCapture = ImageCapture.Builder().build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val bitmap = imageProxy.toBitmap()
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val rotated = rotateBitmap(bitmap, rotationDegrees)
                        onFrameAnalyzed(rotated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner, 
            CameraSelector.DEFAULT_BACK_CAMERA, 
            preview, 
            imageCapture!!, 
            imageAnalysis
        )
    }

    suspend fun captureImage(): Bitmap {
        val capture = imageCapture ?: throw IllegalStateException("Camera not bound")

        return suspendCoroutine { continuation ->
            capture.takePicture(
                context.mainExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                        val bitmap = imageProxy.toBitmap()
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        imageProxy.close()
                        continuation.resume(rotateBitmap(bitmap, rotationDegrees))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    private fun rotateBitmap(source: Bitmap, rotation: Int): Bitmap {
        if (rotation == 0) return source
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
