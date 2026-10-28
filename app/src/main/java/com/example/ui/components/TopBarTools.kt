package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceDark

/**
 * Top Toolbar with two pure icon buttons (no descriptive text)
 * Left: Barcode vector icon (activates continuous camera scan)
 * Right: Gallery vector icon (opens batch image picker)
 */
@Composable
fun TopBarTools(
    onScanClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("top_bar_tools")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Barcode vector icon (Pure icon, no text)
            IconButton(
                onClick = onScanClick,
                modifier = Modifier
                    .size(52.dp)
                    .background(PureBlack, CircleShape)
                    .border(1.5.dp, AccentGreen, CircleShape)
                    .testTag("barcode_scan_button")
            ) {
                BarcodeVectorIcon(
                    tint = AccentGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Right Button: Gallery vector icon (Pure icon, no text)
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .size(52.dp)
                    .background(PureBlack, CircleShape)
                    .border(1.5.dp, AccentCyan, CircleShape)
                    .testTag("gallery_batch_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Cargar imágenes de galería",
                    tint = AccentCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Clean vector barcode drawing with red scanner line
 */
@Composable
fun BarcodeVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Barcode stripes
        val barWidths = listOf(0.08f, 0.05f, 0.12f, 0.06f, 0.10f, 0.05f, 0.09f, 0.07f, 0.11f)
        val barGaps = listOf(0.04f, 0.06f, 0.03f, 0.05f, 0.04f, 0.06f, 0.04f, 0.03f)

        var currentX = 0.05f * w
        val barHeight = 0.70f * h
        val barTop = 0.15f * h

        for (i in barWidths.indices) {
            val barW = barWidths[i] * w
            drawRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(currentX, barTop),
                size = androidx.compose.ui.geometry.Size(barW, barHeight)
            )
            currentX += barW
            if (i < barGaps.size) {
                currentX += barGaps[i] * w
            }
        }

        // Horizontal red laser scan line
        drawLine(
            color = Color(0xFFFF1744),
            start = androidx.compose.ui.geometry.Offset(0f, h * 0.5f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.5f),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}
