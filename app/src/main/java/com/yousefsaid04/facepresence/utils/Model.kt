package com.yousefsaid04.facepresence.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

val INPUTSIZE = 112
val OUTPUTSIZE = 192

class Model(context: Context) {
    private val interpreter: Interpreter

    init {
        val options = Interpreter.Options()
        options.setNumThreads(4)
        interpreter = Interpreter(loadModelFile(context), options)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("mobilefacenet.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun embedding(bitmap: Bitmap): FloatArray {
        val inputSize = INPUTSIZE
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = preprocessBitmap(resizedBitmap)

        val output = Array(1) { FloatArray(OUTPUTSIZE) }

        interpreter.run(input, output)

        return output[0]
    }

    private fun preprocessBitmap(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val inputSize = INPUTSIZE
        val pixelValues = Array(1) {
            Array(inputSize) {
                Array(inputSize) {
                    FloatArray(3)
                }
            }
        }
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resizedBitmap.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                pixelValues[0][y][x][0] = r
                pixelValues[0][y][x][1] = g
                pixelValues[0][y][x][2] = b
            }
        }
        return pixelValues
    }
}