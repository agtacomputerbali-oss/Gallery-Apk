package com.gallery.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Videocam
import com.gallery.app.domain.model.MediaTypeFilter

enum class DockDestination {
    GALLERY, ALBUMS, TRASH, VAULT, SETTINGS
}

@Composable
fun FloatingDockContainer(
    currentDestination: DockDestination?,
    isVisible: Boolean = true,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.ALL,
    onFilterChange: ((MediaTypeFilter) -> Unit)? = null,
    onNavigate: (DockDestination) -> Unit = {},
    onShare: () -> Unit = {},
    onHideToVault: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopyTo: (() -> Unit)? = null,
    onMoveTo: (() -> Unit)? = null,
    onCancelSelection: () -> Unit = {},
    onToggleSelectAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarPadding + 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .animateContentSize()
            ) {
                if (isSelectionMode) {
                    FloatingDockActionBar(
                        selectedCount = selectedCount,
                        onShare = onShare,
                        onHideToVault = onHideToVault,
                        onDelete = onDelete,
                        onCopyTo = onCopyTo,
                        onMoveTo = onMoveTo,
                        onCancelSelection = onCancelSelection,
                        onToggleSelectAll = onToggleSelectAll
                    )
                } else {
                    FloatingDockNav(
                        currentDestination = currentDestination,
                        mediaTypeFilter = mediaTypeFilter,
                        onFilterChange = onFilterChange,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingDockNav(
    currentDestination: DockDestination?,
    mediaTypeFilter: MediaTypeFilter,
    onFilterChange: ((MediaTypeFilter) -> Unit)?,
    onNavigate: (DockDestination) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val (filterIcon, filterLabel) = when (mediaTypeFilter) {
        MediaTypeFilter.ALL -> Icons.Default.Collections to "Semua"
        MediaTypeFilter.PHOTOS_ONLY -> Icons.Default.Image to "Foto"
        MediaTypeFilter.VIDEOS_ONLY -> Icons.Default.Videocam to "Video"
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockNavItem(
            icon = filterIcon,
            label = filterLabel,
            isSelected = currentDestination == DockDestination.GALLERY,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val nextFilter = when (mediaTypeFilter) {
                    MediaTypeFilter.ALL -> MediaTypeFilter.PHOTOS_ONLY
                    MediaTypeFilter.PHOTOS_ONLY -> MediaTypeFilter.VIDEOS_ONLY
                    MediaTypeFilter.VIDEOS_ONLY -> MediaTypeFilter.ALL
                }
                if (onFilterChange != null) {
                    onFilterChange(nextFilter)
                }
                if (currentDestination != DockDestination.GALLERY && onFilterChange == null) {
                    onNavigate(DockDestination.GALLERY)
                }
            }
        )

        DockNavItem(
            icon = Icons.Default.Folder,
            label = "Album",
            isSelected = currentDestination == DockDestination.ALBUMS,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigate(DockDestination.ALBUMS)
            }
        )

        DockNavItem(
            icon = Icons.Default.Delete,
            label = "Sampah",
            isSelected = currentDestination == DockDestination.TRASH,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigate(DockDestination.TRASH)
            }
        )

        DockNavItem(
            icon = Icons.Default.Lock,
            label = "Vault",
            isSelected = currentDestination == DockDestination.VAULT,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigate(DockDestination.VAULT)
            }
        )

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigate(DockDestination.SETTINGS)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = if (currentDestination == DockDestination.SETTINGS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun DockNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun FloatingDockActionBar(
    selectedCount: Int,
    onShare: () -> Unit,
    onHideToVault: () -> Unit,
    onDelete: () -> Unit,
    onCopyTo: (() -> Unit)? = null,
    onMoveTo: (() -> Unit)? = null,
    onCancelSelection: () -> Unit,
    onToggleSelectAll: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCancelSelection()
        }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Batal Seleksi"
            )
        }

        Text(
            text = "$selectedCount Dipilih",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp)
        )

        if (onToggleSelectAll != null) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleSelectAll()
            }) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Pilih Semua / Batal Pilih"
                )
            }
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onShare()
            },
            enabled = selectedCount > 0
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Bagikan"
            )
        }

        if (onCopyTo != null) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCopyTo()
                },
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Salin ke..."
                )
            }
        }

        if (onMoveTo != null) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMoveTo()
                },
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = "Pindahkan ke..."
                )
            }
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onHideToVault()
            },
            enabled = selectedCount > 0
        ) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Sembunyikan ke Vault"
            )
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            },
            enabled = selectedCount > 0
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
