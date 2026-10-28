package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FileStorageHelper {
    private const val TAG = "FileStorageHelper"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Saves JSON string to device Downloads directory and provides share intent.
     */
    suspend fun saveJsonToDownloads(context: Context, jsonContent: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "inventario_lote_$timeStamp.json"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: return@withContext Result.failure(Exception("No se pudo crear el archivo en Descargas."))

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    Result.success("Guardado en Descargas: $fileName")
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { output ->
                        output.write(jsonContent.toByteArray(Charsets.UTF_8))
                        output.flush()
                    }
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                    Result.success("Guardado en Descargas: $fileName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando archivo JSON", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Shares or exports JSON file via Intent chooser
     */
    fun shareJsonContent(context: Context, jsonContent: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, jsonContent)
                type = "application/json"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Compartir JSON de Inventario")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error compartiendo JSON", e)
        }
    }

    /**
     * Downloads an image from imageUrl and saves it in the device with the exact barcode filename
     * Example: 7794000006164.jpg or 7794000006164_1.jpg
     */
    suspend fun downloadImageAsBarcode(
        context: Context,
        imageUrl: String,
        barcode: String,
        index: Int = 0
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = if (index == 0) "$barcode.jpg" else "${barcode}_$index.jpg"
                val request = Request.Builder().url(imageUrl).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error descargando imagen: HTTP ${response.code}"))
                }

                val imageBytes = response.body?.bytes()
                    ?: return@withContext Result.failure(Exception("Cuerpo de imagen vacío"))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScanStock")
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: return@withContext Result.failure(Exception("No se pudo registrar la imagen en la galería."))

                    resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                        stream.write(imageBytes)
                        stream.flush()
                    }
                    Result.success("Descargado: $fileName")
                } else {
                    @Suppress("DEPRECATION")
                    val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ScanStock")
                    if (!picturesDir.exists()) picturesDir.mkdirs()
                    val file = File(picturesDir, fileName)
                    FileOutputStream(file).use { out ->
                        out.write(imageBytes)
                        out.flush()
                    }
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
                    Result.success("Descargado: $fileName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando imagen $imageUrl", e)
                Result.failure(e)
            }
        }
    }
}
