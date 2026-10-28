package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scanner.CameraScannerOverlay
import com.example.ui.components.BatchConfigSelectors
import com.example.ui.components.ProductCardItem
import com.example.ui.components.TopBarTools
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ScanStockTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScanStockTheme {
                InventoryApp()
            }
        }
    }
}

@Composable
fun InventoryApp(
    viewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val selectedLocation by viewModel.selectedLocation.collectAsStateWithLifecycle()
    val selectedUnits by viewModel.selectedUnitsPerBulk.collectAsStateWithLifecycle()
    val isCameraOpen by viewModel.isCameraScannerOpen.collectAsStateWithLifecycle()
    val batchStatus by viewModel.batchStatus.collectAsStateWithLifecycle()

    // Listen to user feedback events for snackbar
    LaunchedEffect(Unit) {
        viewModel.userFeedback.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openCameraScanner()
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo picker launcher for batch images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.processBatchImages(context, uris)
        }
    }

    // Handle back button when camera is open
    BackHandler(enabled = isCameraOpen) {
        viewModel.closeCameraScanner()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        containerColor = PureBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Requirement 6: Botón inferior fijo de exportación
            FixedExportBottomBar(
                productCount = products.size,
                onExportClick = {
                    viewModel.generateAndExportJson(context)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Requirement 2: Top Toolbar with pure icons (barcode & gallery)
                TopBarTools(
                    onScanClick = {
                        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permission == PackageManager.PERMISSION_GRANTED) {
                            viewModel.openCameraScanner()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onGalleryClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // Requirement 3: Configuración del Lote (Dropdowns: Pasillo y Unidades)
                BatchConfigSelectors(
                    selectedLocation = selectedLocation,
                    onLocationSelected = { viewModel.setLocation(it) },
                    selectedUnits = selectedUnits,
                    onUnitsSelected = { viewModel.setUnitsPerBulk(it) }
                )

                // Batch processing progress banner if active
                if (!batchStatus.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .background(SurfaceDark, RoundedCornerShape(8.dp))
                            .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AccentCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = batchStatus ?: "",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                color = AccentCyan,
                                trackColor = Color(0xFF00363A),
                                modifier = Modifier.fillMaxWidth().height(3.dp)
                            )
                        }
                    }
                }

                // Requirement 5: Lista de productos procesados (Diseño vertical de celular)
                if (products.isEmpty()) {
                    // Estado inicial COMPLETAMENTE EN BLANCO
                    EmptyInventoryState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("products_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = products,
                            key = { it.id }
                        ) { product ->
                            ProductCardItem(
                                item = product,
                                onNameChange = { newName ->
                                    viewModel.updateProductName(product.id, newName)
                                },
                                onRemoveClick = {
                                    viewModel.removeProduct(product.id)
                                },
                                onDownloadImage = { url, barcode, index ->
                                    viewModel.downloadImage(context, url, barcode, index)
                                }
                            )
                        }
                    }
                }
            }

            // High-resolution continuous CameraX scanner overlay
            if (isCameraOpen) {
                CameraScannerOverlay(
                    onBarcodeDetected = { barcode ->
                        viewModel.onBarcodeScanned(barcode)
                    },
                    onClose = {
                        viewModel.closeCameraScanner()
                    }
                )
            }
        }
    }
}

/**
 * Initial empty inventory state (app starts completely blank)
 */
@Composable
fun EmptyInventoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Inventario vacío",
                        tint = GraySecondary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Inventario en blanco",
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Presiona el ícono de código de barras para escanear en tiempo real o el de galería para cargar imágenes en lote.",
                color = GraySecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Requirement 6: Fixed bottom export button
 * Text: "GENERAR Y DESCARGAR ARCHIVO .JSON"
 */
@Composable
fun FixedExportBottomBar(
    productCount: Int,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PureBlack,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = onExportClick,
                enabled = productCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_download_json_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = PureBlack,
                    disabledContainerColor = SurfaceDark,
                    disabledContentColor = GrayMuted
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "GENERAR Y DESCARGAR ARCHIVO .JSON",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
