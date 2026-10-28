package com.example.scanner

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object BarcodeBatchProcessor {
    private const val TAG = "BarcodeBatchProcessor"

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Processes a list of image Uris sequentially, returning all detected unique barcodes in order
     */
    suspend fun processImageBatch(
        context: Context,
        uris: List<Uri>,
        onProgress: (current: Int, total: Int, foundBarcode: String?) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        val detectedBarcodes = mutableListOf<String>()
        val total = uris.size

        for ((index, uri) in uris.withIndex()) {
            val step = index + 1
            var barcodeForThisImage: String? = null
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val results = scanner.process(inputImage).await()
                for (b in results) {
                    val raw = b.rawValue
                    if (!raw.isNullOrBlank()) {
                        val trimmed = raw.trim()
                        barcodeForThisImage = trimmed
                        if (!detectedBarcodes.contains(trimmed)) {
                            detectedBarcodes.add(trimmed)
                        }
                        break // Take primary barcode per image
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decodificando imagen $uri", e)
            }
            onProgress(step, total, barcodeForThisImage)
        }

        detectedBarcodes
    }
}
