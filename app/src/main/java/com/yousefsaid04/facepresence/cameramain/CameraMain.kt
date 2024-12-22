package com.yousefsaid04.facepresence.cameramain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
import com.yousefsaid04.facepresence.db.AppDatabase
import com.yousefsaid04.facepresence.db.Student
import com.yousefsaid04.facepresence.utils.Model
import com.yousefsaid04.facepresence.utils.convertImageProxyToBitmap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.sqrt

typealias FaceListener = (List<Face>, Pair<Int, Int>, List<FloatArray>) -> Unit

val cameraThreadExecutor: ExecutorService = Executors.newCachedThreadPool()
val faceDetectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(PERFORMANCE_MODE_ACCURATE)
    .build()

class Recognizer(context: Context, private val active: Boolean,  private val faceListener: FaceListener) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(faceDetectorOptions)
    private val model = Model(context)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)


            val resolution: Pair<Int, Int> = Pair(imageProxy.width, imageProxy.height)

            if (active) {
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        try {
                            val bitmap = convertImageProxyToBitmap(
                                    imageProxy
                                )
                            val faceEmbeddings = mutableListOf<FloatArray>()
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
                                faceEmbeddings += embeddings
                            }
                            rotatedImage.recycle()
                            faceListener(faces, resolution, faceEmbeddings)
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
}

fun cosineSimilarity(array1: FloatArray, array2: FloatArray): Float {

    val dotProduct = array1.zip(array2) { a, b -> a * b }.sum()

    val magnitude1 = sqrt(array1.fold(0f) { acc, value -> acc + value * value })
    val magnitude2 = sqrt(array2.fold(0f) { acc, value -> acc + value * value })

    return if (magnitude1 == 0f || magnitude2 == 0f) {
        0f
    } else {
        dotProduct / (magnitude1 * magnitude2)
    }
}



@Composable
fun CameraScreen(active: Boolean, db: AppDatabase) {
    val detectedFaces = remember { mutableStateOf<List<Face>>(emptyList()) }
    val resolution = remember { mutableStateOf(Pair(1, 1)) }
    val rotation = remember { mutableStateOf(0) }
    val students = remember { mutableStateOf<List<Student>>(emptyList()) }
    val studentEmbs = remember { mutableStateOf(emptyList<FloatArray>()) }

    LaunchedEffect(true) {
        students.value = db.studentDao().getAll()
         studentEmbs.value = students.value.map { student ->
            student.embeddings.split(",").map { it.trim().toFloat() }.toFloatArray()
        }
    }

    val lensFacing = CameraSelector.LENS_FACING_BACK
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val preview = Preview.Builder().build()
    val imageAnalyzer = ImageAnalysis.Builder()
        .build()
        .also {
            it.setAnalyzer(cameraThreadExecutor, Recognizer(context, active) { faces, imageResolution, embeddings ->
                detectedFaces.value = faces
                resolution.value = imageResolution
                rotation.value = preview.targetRotation


                for (embedding in embeddings) {
                    var highestSimilarity = 0F
                    var mostSimilarStudent: Student? = null
                    var i = 0
                    for (studentEmbedding in studentEmbs.value) {
                        val similarity = cosineSimilarity(embedding, studentEmbedding)
                        println("Similarity -----------> $similarity")
                        if (similarity > highestSimilarity) {
                            highestSimilarity = similarity
                            mostSimilarStudent = students.value[i]
                            i += 1
                        }
                    }
                    if (mostSimilarStudent != null)
                    {
                        Log.d("Similarity", "Student: ${mostSimilarStudent.name}, Similarity: ${highestSimilarity}")
                        if (highestSimilarity > 0.75 && mostSimilarStudent.attended == false) {
                            mostSimilarStudent.attended = true
                            db.studentDao().updateStudent(mostSimilarStudent)
                            Toast.makeText(context, "${mostSimilarStudent.name} attendence is recorded", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            })
        }


    val previewView = remember {
        PreviewView(context)
    }
    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    LaunchedEffect(lensFacing, active) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageAnalyzer)
        preview.surfaceProvider = previewView.surfaceProvider
    }
    Box (
        modifier = Modifier.fillMaxSize()
    ){
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        FaceOverlay(
            detectedFaces.value,
            resolution.value,
            previewView,
            rotation.value,
            active,
        )
    }
}

@Composable
fun FaceOverlay(detectedFaces: List<Face>, resolution: Pair<Int, Int>, previewView: PreviewView, rotation: Int, active: Boolean) {
    if (!active) {
        return
    }
    Canvas(modifier = Modifier.fillMaxSize()) {

        val imageWidth: Int
        val imageHeight: Int

        if (rotation == 0) {
            imageHeight = resolution.first
            imageWidth = resolution.second
        }
        else {
            imageWidth = resolution.first
            imageHeight = resolution.second
        }

        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()

        val scaleX =  previewWidth / imageWidth
        val scaleY = previewHeight / imageHeight
        val scale = maxOf(scaleX, scaleY)
        val offsetX = (previewWidth - imageWidth * scale) / 2
        val offsetY = (previewHeight - imageHeight * scale) / 2

        detectedFaces.forEach { face ->
            val bounds: Rect = face.boundingBox
            val left = bounds.left * scale + offsetX
            val top = bounds.top * scale + offsetY
            val right = bounds.right * scale + offsetX
            val bottom = bounds.bottom * scale + offsetY

            drawRect(
                color = Color.Red,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f) // Corrected stroke
            )
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