package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.InventoryExportItem
import com.example.data.model.ProductItem
import com.example.data.repository.ProductLookupRepository
import com.example.scanner.BarcodeBatchProcessor
import com.example.util.FileStorageHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID

class InventoryViewModel(
    private val repository: ProductLookupRepository = ProductLookupRepository()
) : ViewModel() {

    private val TAG = "InventoryViewModel"

    // The inventory starts COMPLETELY EMPTY as requested
    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products: StateFlow<List<ProductItem>> = _products.asStateFlow()

    // Configuration selectors
    private val _selectedLocation = MutableStateFlow("pasillo-1")
    val selectedLocation: StateFlow<String> = _selectedLocation.asStateFlow()

    private val _selectedUnitsPerBulk = MutableStateFlow(6)
    val selectedUnitsPerBulk: StateFlow<Int> = _selectedUnitsPerBulk.asStateFlow()

    // Scanner state
    private val _isCameraScannerOpen = MutableStateFlow(false)
    val isCameraScannerOpen: StateFlow<Boolean> = _isCameraScannerOpen.asStateFlow()

    private val _batchStatus = MutableStateFlow<String?>(null)
    val batchStatus: StateFlow<String?> = _batchStatus.asStateFlow()

    private val _userFeedback = MutableSharedFlow<String>()
    val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

    // Anti-duplicate validation set for the current session
    private val scannedBarcodesSet = Collections.synchronizedSet(mutableSetOf<String>())

    fun setLocation(location: String) {
        _selectedLocation.value = location
    }

    fun setUnitsPerBulk(units: Int) {
        _selectedUnitsPerBulk.value = units
    }

    fun openCameraScanner() {
        _isCameraScannerOpen.value = true
    }

    fun closeCameraScanner() {
        _isCameraScannerOpen.value = false
    }

    /**
     * Anti-duplicate validation logic:
     * Verifies the barcode before processing.
     * If the barcode was already scanned in the current session, it is ignored automatically.
     */
    fun onBarcodeScanned(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        synchronized(scannedBarcodesSet) {
            if (scannedBarcodesSet.contains(cleanBarcode)) {
                Log.d(TAG, "Código duplicado ignorado: $cleanBarcode")
                viewModelScope.launch {
                    _userFeedback.emit("Código $cleanBarcode ya escaneado (ignorado)")
                }
                return
            }
            scannedBarcodesSet.add(cleanBarcode)
        }

        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        val newItem = ProductItem(
            id = UUID.randomUUID().toString(),
            barcode = cleanBarcode,
            name = "Buscando en Gemini...",
            category = _selectedLocation.value,
            unitsPerBulk = _selectedUnitsPerBulk.value,
            scannedAt = currentTime,
            isProcessing = true
        )

        _products.value = listOf(newItem) + _products.value

        // Fetch details from Gemini + Open Food Facts
        viewModelScope.launch {
            try {
                val result = repository.lookupBarcode(cleanBarcode)
                _products.value = _products.value.map { item ->
                    if (item.id == newItem.id) {
                        item.copy(
                            name = result.productName,
                            alternativeNames = result.alternativeNames,
                            thumbnail = result.thumbnail,
                            backupImages = result.backupImages,
                            isProcessing = false
                        )
                    } else {
                        item
                    }
                }
                _userFeedback.emit("Detectado: ${result.productName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando producto $cleanBarcode", e)
                _products.value = _products.value.map { item ->
                    if (item.id == newItem.id) {
                        item.copy(
                            name = "Producto $cleanBarcode",
                            isProcessing = false,
                            errorMessage = e.message
                        )
                    } else {
                        item
                    }
                }
            }
        }
    }

    /**
     * Processes a batch of selected gallery images sequentially one by one
     */
    fun processBatchImages(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _batchStatus.value = "Iniciando procesamiento de lote (${uris.size} imágenes)..."
            var newlyAddedCount = 0

            val detectedBarcodes = BarcodeBatchProcessor.processImageBatch(context, uris) { current, total, barcode ->
                _batchStatus.value = "Analizando imagen $current de $total..."
            }

            for (barcode in detectedBarcodes) {
                var isDuplicate = false
                synchronized(scannedBarcodesSet) {
                    if (scannedBarcodesSet.contains(barcode)) {
                        isDuplicate = true
                    }
                }
                if (!isDuplicate) {
                    newlyAddedCount++
                    onBarcodeScanned(barcode)
                }
            }

            _batchStatus.value = null
            _userFeedback.emit("Lote procesado: $newlyAddedCount productos nuevos agregados.")
        }
    }

    /**
     * Updates the product commercial name from user's manual text edit
     */
    fun updateProductName(productId: String, newName: String) {
        _products.value = _products.value.map { item ->
            if (item.id == productId) {
                item.copy(name = newName)
            } else {
                item
            }
        }
    }

    fun removeProduct(productId: String) {
        _products.value = _products.value.filter { it.id != productId }
    }

    /**
     * Downloads a backup image to the phone storage with the exact barcode filename
     * e.g. 7794000006164.jpg
     */
    fun downloadImage(context: Context, imageUrl: String, barcode: String, index: Int) {
        viewModelScope.launch {
            _userFeedback.emit("Descargando imagen para $barcode...")
            val result = FileStorageHelper.downloadImageAsBarcode(context, imageUrl, barcode, index)
            result.onSuccess { msg ->
                _userFeedback.emit(msg)
            }.onFailure { err ->
                _userFeedback.emit("Error al descargar: ${err.localizedMessage}")
            }
        }
    }

    /**
     * Generates and downloads the .JSON file with dynamic edited fields and structural empty/0 fields
     */
    fun generateAndExportJson(context: Context) {
        val currentItems = _products.value
        if (currentItems.isEmpty()) {
            viewModelScope.launch {
                _userFeedback.emit("El inventario está vacío. Escanea productos primero.")
            }
            return
        }

        viewModelScope.launch {
            val jsonString = InventoryExportItem.toJsonArrayString(currentItems)
            val result = FileStorageHelper.saveJsonToDownloads(context, jsonString)
            result.onSuccess { msg ->
                _userFeedback.emit(msg)
                FileStorageHelper.shareJsonContent(context, jsonString)
            }.onFailure { err ->
                _userFeedback.emit("Error guardando JSON: ${err.localizedMessage}")
            }
        }
    }
}
