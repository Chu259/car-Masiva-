package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class InventoryExportItem(
    val barcode: String,
    val name: String,
    val category: String,
    val unitsPerBulk: Int,
    val scannedAt: String,
    val stock: Int = 0,
    val cost: Double = 0.0,
    val price: Double = 0.0,
    val supplier: String = "",
    val taxRate: Double = 0.0,
    val sku: String = "",
    val notes: String = "",
    val alternativeNames: List<String> = emptyList(),
    val imageUrl: String = ""
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("barcode", barcode)
        json.put("name", name)
        json.put("category", category)
        json.put("unitsPerBulk", unitsPerBulk)
        json.put("scannedAt", scannedAt)
        json.put("stock", stock)
        json.put("cost", cost)
        json.put("price", price)
        json.put("supplier", supplier)
        json.put("taxRate", taxRate)
        json.put("sku", sku)
        json.put("notes", notes)
        val altArray = JSONArray()
        alternativeNames.forEach { altArray.put(it) }
        json.put("alternativeNames", altArray)
        json.put("imageUrl", imageUrl)
        return json
    }

    companion object {
        fun fromProductItem(item: ProductItem): InventoryExportItem {
            return InventoryExportItem(
                barcode = item.barcode,
                name = item.name,
                category = item.category,
                unitsPerBulk = item.unitsPerBulk,
                scannedAt = item.scannedAt,
                stock = 0,
                cost = 0.0,
                price = 0.0,
                supplier = "",
                taxRate = 0.0,
                sku = "",
                notes = "",
                alternativeNames = item.alternativeNames,
                imageUrl = item.thumbnail.orEmpty()
            )
        }

        fun toJsonArrayString(items: List<ProductItem>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                jsonArray.put(fromProductItem(item).toJsonObject())
            }
            return jsonArray.toString(2)
        }
    }
}
