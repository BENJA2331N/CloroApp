package com.example.cloroapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestión de archivos de imágenes y URIs seguros con FileProvider (IE 2.4.1).
 * Guarda fotos físicas en el almacenamiento seguro de la app y devuelve la ruta/URI persistente
 * para guardarla en Room (SQLite) sin perderse al cerrar la app o cambiar de pestaña.
 */
object ImagenUtils {

    /**
     * Crea un archivo temporal físico y genera un Uri seguro a través de FileProvider para pasar a TakePicture.
     */
    fun crearUriFotoTemporal(context: Context): Pair<Uri, File> {
        val dirFotos = File(context.filesDir, "fotos").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val archivo = File.createTempFile("FOTO_${timeStamp}_", ".jpg", dirFotos)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
        return uri to archivo
    }

    /**
     * Carga un Bitmap redimensionado de forma segura desde un URI o ruta local sin sobrecargar la memoria.
     */
    fun cargarBitmapDesdeUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val opciones = BitmapFactory.Options().apply {
                    inSampleSize = 2 // escala ligera para preview fluido
                }
                BitmapFactory.decodeStream(input, null, opciones)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Carga un Bitmap desde la ruta absoluta de un archivo guardado.
     */
    fun cargarBitmapDesdeRuta(rutaArchivo: String?): Bitmap? {
        if (rutaArchivo.isNullOrBlank()) return null
        val archivo = File(rutaArchivo)
        if (!archivo.exists()) return null
        return try {
            val opciones = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            BitmapFactory.decodeFile(archivo.absolutePath, opciones)
        } catch (_: Exception) {
            null
        }
    }
}
