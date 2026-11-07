package com.example.cloroapp.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Conversión de fotografías capturadas con la cámara nativa a formato Base64
 * para persistirlas directamente en las tablas SQLite de Room (NivelCloro y AccionCorrectiva).
 */
object ImagenUtils {

    fun bitmapABase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        val bytes = output.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun base64ABitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

