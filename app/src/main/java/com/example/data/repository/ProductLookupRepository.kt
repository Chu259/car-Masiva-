package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ProductLookupResult(
    val productName: String,
    val alternativeNames: List<String>,
    val thumbnail: String?,
    val backupImages: List<String>
)

class ProductLookupRepository {
    private val TAG = "ProductLookupRepo"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun lookupBarcode(barcode: String): ProductLookupResult = withContext(Dispatchers.IO) {
        var geminiName: String? = null
        var geminiAltNames = mutableListOf<String>()

        // 1. Fetch from Gemini API if API key is provided
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Eres un asistente experto en gestión de inventarios de supermercados y tiendas.
                    Tu tarea es recibir el código de barras "$barcode", identificar el producto correspondiente y responder con un objeto JSON válido con la siguiente estructura:
                    {
                      "productName": "Nombre comercial completo del producto en español (incluyendo marca, producto, variedad y tamaño/peso)",
                      "alternativeNames": [
                        "variante o nombre comercial alternativo 1",
                        "variante o nombre comercial alternativo 2",
                        "variante o nombre comercial alternativo 3",
                        "variante o nombre comercial alternativo 4",
                        "variante o nombre comercial alternativo 5",
                        "variante o nombre comercial alternativo 6"
                      ]
                    }
                    Devuelve ÚNICAMENTE el objeto JSON sin texto adicional ni bloques markdown.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        }
                        put(partObj)
                    }
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                }

                val geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(geminiUrl)
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            try {
                                val parsed = JSONObject(text.trim())
                                val pName = parsed.optString("productName")
                                if (pName.isNotBlank()) {
                                    geminiName = pName
                                }
                                val altArray = parsed.optJSONArray("alternativeNames")
                                if (altArray != null) {
                                    for (i in 0 until altArray.length()) {
                                        val name = altArray.optString(i)
                                        if (name.isNotBlank() && !geminiAltNames.contains(name)) {
                                            geminiAltNames.add(name)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Parsing direct Gemini JSON failed, using raw response", e)
                                geminiName = text.trim()
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Gemini API returned code ${response.code}: $responseBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking Gemini API", e)
            }
        }

        // 2. Query Open Food Facts for real product metadata and packaging photos
        var offName: String? = null
        var primaryThumbnail: String? = null
        val foundImages = mutableListOf<String>()
        val offAltNames = mutableListOf<String>()

        try {
            val offUrl = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
            val offRequest = Request.Builder()
                .url(offUrl)
                .header("User-Agent", "ScanStock-Android-App/1.0 (inventory-assistant)")
                .build()

            val offResponse = httpClient.newCall(offRequest).execute()
            val offBody = offResponse.body?.string()
            if (offResponse.isSuccessful && !offBody.isNullOrBlank()) {
                val offJson = JSONObject(offBody)
                val status = offJson.optInt("status", 0)
                if (status == 1) {
                    val product = offJson.optJSONObject("product")
                    if (product != null) {
                        val nameEs = product.optString("product_name_es")
                        val nameGeneric = product.optString("product_name")
                        val brand = product.optString("brands")
                        val quantity = product.optString("quantity")

                        offName = when {
                            nameEs.isNotBlank() && brand.isNotBlank() -> "$brand $nameEs $quantity".trim()
                            nameEs.isNotBlank() -> "$nameEs $quantity".trim()
                            nameGeneric.isNotBlank() && brand.isNotBlank() -> "$brand $nameGeneric $quantity".trim()
                            nameGeneric.isNotBlank() -> nameGeneric
                            else -> null
                        }

                        // Collect alternative names
                        if (brand.isNotBlank() && nameGeneric.isNotBlank()) {
                            offAltNames.add("$brand $nameGeneric")
                        }
                        val genericName = product.optString("generic_name_es")
                        if (genericName.isNotBlank()) offAltNames.add(genericName)
                        val categories = product.optString("categories")
                        if (categories.isNotBlank()) {
                            categories.split(",").take(2).forEach {
                                val cat = it.trim()
                                if (cat.isNotBlank()) offAltNames.add(cat)
                            }
                        }

                        // Images
                        val imgFront = product.optString("image_front_url")
                        val imgNormal = product.optString("image_url")
                        val imgThumb = product.optString("image_thumb_url")
                        val imgIngredients = product.optString("image_ingredients_url")
                        val imgNutrition = product.optString("image_nutrition_url")

                        if (imgFront.isNotBlank()) {
                            primaryThumbnail = imgFront
                            foundImages.add(imgFront)
                        } else if (imgNormal.isNotBlank()) {
                            primaryThumbnail = imgNormal
                            foundImages.add(imgNormal)
                        }

                        if (imgThumb.isNotBlank() && !foundImages.contains(imgThumb)) foundImages.add(imgThumb)
                        if (imgIngredients.isNotBlank() && !foundImages.contains(imgIngredients)) foundImages.add(imgIngredients)
                        if (imgNutrition.isNotBlank() && !foundImages.contains(imgNutrition)) foundImages.add(imgNutrition)

                        // Check selected_images
                        val selectedImages = product.optJSONObject("selected_images")
                        if (selectedImages != null) {
                            val keys = listOf("front", "ingredients", "nutrition", "packaging")
                            for (key in keys) {
                                val section = selectedImages.optJSONObject(key)
                                val display = section?.optJSONObject("display")
                                if (display != null) {
                                    val langKeys = display.keys()
                                    while (langKeys.hasNext()) {
                                        val lk = langKeys.next()
                                        val url = display.optString(lk)
                                        if (url.isNotBlank() && !foundImages.contains(url)) {
                                            foundImages.add(url)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Open Food Facts lookup error: ${e.message}")
        }

        // Finalize product commercial name
        val finalName = when {
            !geminiName.isNullOrBlank() -> geminiName!!
            !offName.isNullOrBlank() -> offName!!
            else -> "Producto $barcode"
        }

        // Finalize alternative names (up to 6)
        val combinedAlt = mutableListOf<String>()
        combinedAlt.addAll(geminiAltNames)
        offAltNames.forEach { if (!combinedAlt.contains(it)) combinedAlt.add(it) }

        if (combinedAlt.isEmpty()) {
            combinedAlt.add("$finalName (Estándar)")
            combinedAlt.add("Código $barcode")
            combinedAlt.add("Artículo Supermercado")
            combinedAlt.add("Unidad Comercial")
            combinedAlt.add("Presentación Original")
            combinedAlt.add("Pack Mayorista")
        }
        val finalAltList = combinedAlt.take(6)

        // Finalize backup images (exactly 6 for the grid)
        val backupImagesList = mutableListOf<String>()
        backupImagesList.addAll(foundImages)

        // If fewer than 6 images, add curated product / supermarket placeholder images
        val defaultStockImages = listOf(
            "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&q=80", // Supermarket shelf
            "https://images.unsplash.com/photo-1588964895597-cfccd6e2dbf9?w=400&q=80", // Grocery cart
            "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=400&q=80", // Food packaging
            "https://images.unsplash.com/photo-1534723452862-4c874018d66d?w=400&q=80", // Grocery aisle
            "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=400&q=80", // Retail product
            "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=400&q=80"  // Market display
        )

        var stockIdx = 0
        while (backupImagesList.size < 6 && stockIdx < defaultStockImages.size) {
            val stockImg = defaultStockImages[stockIdx]
            if (!backupImagesList.contains(stockImg)) {
                backupImagesList.add(stockImg)
            }
            stockIdx++
        }

        val finalThumb = primaryThumbnail ?: backupImagesList.firstOrNull()

        ProductLookupResult(
            productName = finalName,
            alternativeNames = finalAltList,
            thumbnail = finalThumb,
            backupImages = backupImagesList.take(6)
        )
    }
}
