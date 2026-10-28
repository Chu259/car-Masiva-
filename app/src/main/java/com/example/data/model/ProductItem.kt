package com.example.data.model

import java.util.UUID

data class ProductItem(
    val id: String = UUID.randomUUID().toString(),
    val barcode: String,
    val name: String,
    val category: String, // "pasillo-1" or "pasillo-2"
    val unitsPerBulk: Int, // numeric
    val scannedAt: String,
    val thumbnail: String? = null,
    val alternativeNames: List<String> = emptyList(),
    val backupImages: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
