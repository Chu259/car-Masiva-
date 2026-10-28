package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OLEDDarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF00381C),
    onPrimaryContainer = AccentGreen,
    secondary = AccentCyan,
    onSecondary = PureBlack,
    secondaryContainer = Color(0xFF00363A),
    onSecondaryContainer = AccentCyan,
    background = PureBlack,
    onBackground = PureWhite,
    surface = SurfaceDark,
    onSurface = PureWhite,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = GraySecondary,
    outline = BorderDark,
    error = AccentRed,
    onError = PureWhite
)

@Composable
fun ScanStockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OLEDDarkColorScheme,
        typography = Typography,
        content = content
    )
}
