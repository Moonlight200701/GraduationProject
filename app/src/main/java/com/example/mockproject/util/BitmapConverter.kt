package com.example.mockproject.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

class BitmapConverter {
    //For the image taken by the camera

    //Encode the avatar img into a string
    fun encodeBase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b: ByteArray = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    //Decode the avatar bitmap string to an image
    fun decodeBase64(input: String?): Bitmap? {
        val decodedByte = Base64.decode(input, 0)
        return BitmapFactory
            .decodeByteArray(decodedByte, 0, decodedByte.size)
    }
}
