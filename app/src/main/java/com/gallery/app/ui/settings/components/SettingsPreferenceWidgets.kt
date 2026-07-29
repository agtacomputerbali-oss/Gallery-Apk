package com.gallery.app.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.domain.model.AccentColor
import com.gallery.app.ui.theme.AppTheme

@Composable
fun PreferenceCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
fun PreferenceGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
fun PreferenceTile(
    title: String,
    icon: ImageVector? = null,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!summary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

@Composable
fun PreferenceSwitchTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    summary: String? = null
) {
    PreferenceTile(
        title = title,
        icon = icon,
        summary = summary,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeSegmentedRow(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = AppTheme.entries
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        options.forEachIndexed { index, theme ->
            SegmentedButton(
                selected = selectedTheme == theme,
                onClick = { onThemeSelected(theme) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(text = theme.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ColorPickerPreferenceRow(
    selectedColor: AccentColor,
    onColorSelected: (AccentColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentColor.entries.forEach { colorItem ->
            val colorVal = Color(colorItem.primaryColorHex)
            val isSelected = selectedColor == colorItem

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onColorSelected(colorItem) }
                    .padding(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorVal)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else Modifier
                        )
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = colorItem.displayName.split(" ").firstOrNull() ?: colorItem.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    getItemLabel: (T) -> String
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = getItemLabel(option),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Tutup")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridColumnPickerDialog(
    galleryCols: Int,
    albumCols: Int,
    vaultCols: Int,
    onGalleryColsChange: (Int) -> Unit,
    onAlbumColsChange: (Int) -> Unit,
    onVaultColsChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Pengaturan Jumlah Kolom Grid", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Galeri Utama
                Column {
                    Text(
                        text = "Galeri Utama ($galleryCols Kolom)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        (3..6).forEachIndexed { index, cols ->
                            SegmentedButton(
                                selected = galleryCols == cols,
                                onClick = { onGalleryColsChange(cols) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                            ) {
                                Text("$cols")
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Detail Album
                Column {
                    Text(
                        text = "Detail Album ($albumCols Kolom)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        (3..6).forEachIndexed { index, cols ->
                            SegmentedButton(
                                selected = albumCols == cols,
                                onClick = { onAlbumColsChange(cols) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                            ) {
                                Text("$cols")
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Hidden Vault
                Column {
                    Text(
                        text = "Hidden Vault ($vaultCols Kolom)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        (3..6).forEachIndexed { index, cols ->
                            SegmentedButton(
                                selected = vaultCols == cols,
                                onClick = { onVaultColsChange(cols) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                            ) {
                                Text("$cols")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Selesai")
            }
        }
    )
}
