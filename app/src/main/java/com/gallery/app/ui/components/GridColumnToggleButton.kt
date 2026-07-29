package com.gallery.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun GridColumnToggleButton(
    currentColumns: Int,
    onColumnChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxColumns: Int = 6
) {
    val safeMax = maxColumns.coerceAtLeast(3)
    val nextCount = if (currentColumns >= safeMax) 3 else currentColumns + 1

    IconButton(
        onClick = { onColumnChange(nextCount) },
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = "$currentColumns",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Ubah Kolom Grid (Saat ini: $currentColumns)",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
