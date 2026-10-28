package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductCardItem(
    item: ProductItem,
    onNameChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    onDownloadImage: (imageUrl: String, barcode: String, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editableName by remember(item.name) { mutableStateOf(item.name) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("product_card_${item.barcode}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Thumbnail, Barcode, Pasillo & Bultos, Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.thumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = item.thumbnail,
                            contentDescription = "Miniatura ${item.barcode}",
                            modifier = Modifier.size(54.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else if (item.isProcessing) {
                        CircularProgressIndicator(
                            color = AccentGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Sin imagen",
                            tint = GrayMuted,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Barcode number (Monospace bold)
                    Text(
                        text = item.barcode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tags: Pasillo & Bultos
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Color(0xFF00381C),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.category,
                                color = AccentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFF00363A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${item.unitsPerBulk} u./bulto",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("remove_product_${item.barcode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar de la lista",
                        tint = GrayMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Editable Name Field (TOTALMENTE EDITABLE)
            Text(
                text = "NOMBRE DEL PRODUCTO (GEMINI / EDITABLE)",
                fontSize = 10.sp,
                color = GraySecondary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = editableName,
                onValueChange = {
                    editableName = it
                    onNameChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editable_name_input_${item.barcode}"),
                textStyle = TextStyle(
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar nombre",
                        tint = AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = if (item.isProcessing) "Buscando nombre en Gemini..." else "Escribe el nombre comercial",
                        color = GrayMuted,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = AccentGreen
                ),
                maxLines = 2
            )

            // Alternative Google / Gemini names (up to 6)
            if (item.alternativeNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "NOMBRES ALTERNATIVOS (GOOGLE / GEMINI)",
                    fontSize = 10.sp,
                    color = GraySecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.alternativeNames.take(6).forEach { altName ->
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.clickable {
                                editableName = altName
                                onNameChange(altName)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = GraySecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = altName,
                                    color = GrayLight,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 6-Image Backup Grid with Floating Download Buttons
            if (item.backupImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "IMÁGENES DE RESPALDO (DESCARGA AUTOMÁTICA .JPG)",
                    fontSize = 10.sp,
                    color = GraySecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Grid of up to 6 images (2 rows of 3 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.backupImages.take(3).forEachIndexed { idx, url ->
                        BackupImageTile(
                            imageUrl = url,
                            barcode = item.barcode,
                            index = idx,
                            onDownload = onDownloadImage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (item.backupImages.size > 3) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.backupImages.drop(3).take(3).forEachIndexed { idx, url ->
                            BackupImageTile(
                                imageUrl = url,
                                barcode = item.barcode,
                                index = idx + 3,
                                onDownload = onDownloadImage,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact tile with floating download button to save file as {barcode}.jpg
 */
@Composable
private fun BackupImageTile(
    imageUrl: String,
    barcode: String,
    index: Int,
    onDownload: (imageUrl: String, barcode: String, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDownloaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Imagen respaldo $index de $barcode",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        // Floating download button in top-right corner
        IconButton(
            onClick = {
                onDownload(imageUrl, barcode, index)
                isDownloaded = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(if (isDownloaded) AccentGreen else Color(0xCC000000), CircleShape)
                .border(1.dp, if (isDownloaded) AccentGreen else PureWhite, CircleShape)
                .testTag("download_image_${barcode}_$index")
        ) {
            Icon(
                imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                contentDescription = "Descargar $barcode.jpg",
                tint = if (isDownloaded) PureBlack else PureWhite,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
