package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

/**
 * Dropdown selectors for batch configuration:
 * 1. Location selector ("pasillo-1", "pasillo-2") -> maps to "category"
 * 2. Units selector (1, 2, 4, 6, 8, 10, 12, 18, 24, 30, 36, 48) -> maps to "unitsPerBulk"
 */
@Composable
fun BatchConfigSelectors(
    selectedLocation: String,
    onLocationSelected: (String) -> Unit,
    selectedUnits: Int,
    onUnitsSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationOptions = listOf("pasillo-1", "pasillo-2")
    val unitsOptions = listOf(1, 2, 3, 4, 6, 8, 10, 12, 18, 24, 30, 36, 48)

    var locationExpanded by remember { mutableStateOf(false) }
    var unitsExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Location Dropdown (Selector de Ubicación)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .clickable { locationExpanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("location_dropdown_selector")
        ) {
            Column {
                Text(
                    text = "UBICACIÓN (CATEGORÍA)",
                    fontSize = 10.sp,
                    color = GraySecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLocation,
                        fontSize = 14.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Desplegar ubicaciones",
                        tint = GraySecondary
                    )
                }
            }

            DropdownMenu(
                expanded = locationExpanded,
                onDismissRequest = { locationExpanded = false },
                modifier = Modifier
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            ) {
                locationOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedLocation) AccentGreen else PureWhite,
                                fontWeight = if (option == selectedLocation) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onLocationSelected(option)
                            locationExpanded = false
                        },
                        modifier = Modifier.testTag("location_option_$option")
                    )
                }
            }
        }

        // Units Dropdown (Selector de Unidades por Bulto)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .clickable { unitsExpanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("units_dropdown_selector")
        ) {
            Column {
                Text(
                    text = "UNIDADES / BULTO",
                    fontSize = 10.sp,
                    color = GraySecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedUnits u.",
                        fontSize = 14.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Desplegar unidades",
                        tint = GraySecondary
                    )
                }
            }

            DropdownMenu(
                expanded = unitsExpanded,
                onDismissRequest = { unitsExpanded = false },
                modifier = Modifier
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            ) {
                unitsOptions.forEach { units ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "$units unidades",
                                color = if (units == selectedUnits) AccentCyan else PureWhite,
                                fontWeight = if (units == selectedUnits) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onUnitsSelected(units)
                            unitsExpanded = false
                        },
                        modifier = Modifier.testTag("units_option_$units")
                    )
                }
            }
        }
    }
}
