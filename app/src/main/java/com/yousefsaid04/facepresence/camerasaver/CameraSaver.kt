package com.yousefsaid04.facepresence.camerasaver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
import com.yousefsaid04.facepresence.utils.Model
import com.yousefsaid04.facepresence.utils.convertImageProxyToBitmap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias EmbeddingListener = (FloatArray, Bitmap) -> Unit

val cameraThreadExecutor: ExecutorService = Executors.newCachedThreadPool()
val faceDetectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(PERFORMANCE_MODE_ACCURATE)
    .build()

class Recognizer(context: Context, private val embeddingListener: EmbeddingListener) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(faceDetectorOptions)
    private val model = Model(context)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    try {
                        val bitmap = convertImageProxyToBitmap(imageProxy)
                        val rotationMatrix = Matrix()
                        rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        val rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height, rotationMatrix, true)
                        faces.forEach { face ->
                            val bounds: Rect = face.boundingBox
                            val left = bounds.left
                            val top = bounds.top
                            val right = bounds.right
                            val bottom = bounds.bottom
                            val croppedImage = Bitmap.createBitmap(rotatedImage, left, top, right - left, bottom - top)
                            val embeddings = model.embedding(croppedImage)
                            embeddingListener(embeddings, croppedImage)
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                    finally {
                        imageProxy.close()
                    }
                }


        }
    }
}


@Composable
fun CameraCollector(active: Boolean, embeddingPort: (FloatArray, Bitmap)->Unit) {
    val lensFacing = CameraSelector.LENS_FACING_FRONT
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    Log.d("CameraController", "Start")

    val imageAnalyzer = ImageAnalysis.Builder()
        .build()
        .also {
            it.setAnalyzer(cameraThreadExecutor, Recognizer(context) { embeddings, face ->
                embeddingPort(embeddings, face)
                Log.d("CollecterEmbeddings", embeddings.joinToString(","))
            })
        }


    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    LaunchedEffect(lensFacing, active) {
        val cameraProvider = context.getCameraProvider()
        if (active)
        {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, imageAnalyzer)
        }
        else {
            cameraProvider.unbindAll()
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }