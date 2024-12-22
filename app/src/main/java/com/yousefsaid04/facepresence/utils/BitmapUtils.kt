package com.yousefsaid04.facepresence.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


@OptIn(ExperimentalGetImage::class)
fun convertImageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val image = imageProxy.image
    if (image != null)
    {
        val ib = ByteBuffer.allocate(image.height * image.width * 2)

        val y: ByteBuffer = image.planes[0].buffer
        val cr: ByteBuffer = image.planes[1].buffer
        val cb: ByteBuffer = image.planes[2].buffer
        ib.put(y)
        ib.put(cb)
        ib.put(cr)

        val yuvImage = YuvImage(
            ib.array(),
            ImageFormat.NV21, image.width, image.height, null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(
                0, 0,
                image.width, image.height
            ), 50, out
        )
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        return bitmap
    }
    return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

}