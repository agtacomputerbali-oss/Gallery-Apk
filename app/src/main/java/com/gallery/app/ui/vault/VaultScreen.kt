package com.gallery.app.ui.vault

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.gallery.app.ui.components.gridPinchToZoomGesture
import com.gallery.app.ui.components.gridDragToSelectGesture
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.domain.model.SortOption
import com.gallery.app.domain.model.VaultItem
import com.gallery.app.ui.components.DockDestination
import com.gallery.app.ui.components.FloatingDockContainer
import com.gallery.app.ui.components.GridColumnToggleButton
import com.gallery.app.ui.components.GridSectionHeader
import com.gallery.app.ui.components.SortBottomSheet
import com.gallery.app.ui.gallery.rememberMaxGridColumns
import com.gallery.app.ui.vault.components.VaultLockScreen
import com.gallery.app.ui.vault.VaultFolder
import com.gallery.app.ui.vault.VaultTab
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(
    onBackClick: () -> Unit,
    onGalleryClick: () -> Unit = {},
    onAlbumClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vaultCols by viewModel.vaultGridColumns.collectAsStateWithLifecycle()
    val vaultImageLoader = viewModel.vaultImageLoader

    var showSortSheet by remember { mutableStateOf(false) }
    val maxCols = rememberMaxGridColumns()
    val fragmentActivity = rememberFragmentActivity(context)

    if (!uiState.isUnlocked) {
        VaultLockScreen(
            isPinConfigured = uiState.isPinConfigured,
            isBiometricAvailable = uiState.isBiometricAvailable,
            onPinSubmitted = { pin -> viewModel.submitPin(pin) },
            onBiometricClick = {
                if (fragmentActivity != null) {
                    viewModel.authenticateWithBiometric(fragmentActivity)
                }
            },
            errorMessage = uiState.errorMessage
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isSelectionMode) {
                    TopAppBar(
                        title = { Text(text = "${uiState.selectedCount} Dipilih") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Batal Seleksi"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    viewModel.restoreSelectedItems {
                                        Toast.makeText(context, "Foto dipulihkan ke galeri", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = uiState.selectedCount > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestoreFromTrash,
                                    contentDescription = "Pulihkan ke Galeri"
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteSelectedItemsPermanently {
                                        Toast.makeText(context, "Foto dihapus permanen dari Vault", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = uiState.selectedCount > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Hapus Permanen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                } else if (uiState.selectedFolderName != null) {
                    val folderTitle = uiState.selectedFolderName ?: ""
                    TopAppBar(
                        title = { Text(text = folderTitle) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.closeFolder() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Folder Vault"
                                )
                            }
                        },
                        actions = {
                            GridColumnToggleButton(
                                currentColumns = vaultCols,
                                maxColumns = maxCols,
                                onColumnChange = { viewModel.setGridColumns(it) }
                            )

                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Urutkan Foto"
                                )
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(text = "Hidden Vault") },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.lockVault()
                                    onBackClick()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali"
                                )
                            }
                        },
                        actions = {
                            GridColumnToggleButton(
                                currentColumns = vaultCols,
                                maxColumns = maxCols,
                                onColumnChange = { viewModel.setGridColumns(it) }
                            )

                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Urutkan Foto"
                                )
                            }
                        }
                    )
                }

                // Tab Switcher (jika tidak sedang di dalam folder spesifik dan tidak dalam selection mode)
                if (!uiState.isSelectionMode && uiState.selectedFolderName == null && uiState.vaultItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.currentTab == VaultTab.ALL_PHOTOS,
                            onClick = { viewModel.setTab(VaultTab.ALL_PHOTOS) },
                            label = { Text("Semua Foto (${uiState.vaultItems.size})") }
                        )

                        FilterChip(
                            selected = uiState.currentTab == VaultTab.FOLDERS,
                            onClick = { viewModel.setTab(VaultTab.FOLDERS) },
                            label = { Text("Folder (${uiState.vaultFolders.size})") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        uiState.vaultItems.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Vault Kosong",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Foto yang Anda sembunyikan akan tersimpan dengan aman di sini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        uiState.selectedFolderName == null && uiState.currentTab == VaultTab.FOLDERS -> {
                            // Tampilan Grid Folder Vault
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.vaultFolders,
                                    key = { it.folderName }
                                ) { folder ->
                                    val isSelected = uiState.selectedFolderNames.contains(folder.folderName)
                                    VaultFolderCard(
                                        folder = folder,
                                        imageLoader = vaultImageLoader,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleFolderSelection(folder.folderName)
                                            } else {
                                                viewModel.openFolder(folder.folderName)
                                            }
                                        },
                                        onLongClick = {
                                            if (!uiState.isSelectionMode) {
                                                viewModel.enterFolderSelectionMode(folder.folderName)
                                            } else {
                                                viewModel.toggleFolderSelection(folder.folderName)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        else -> {
                            // Tampilan Grid Foto Ber-Seksi (Semua Foto atau Dalam Folder Spesifik)
                            val groupedItems = uiState.groupedCurrentFolderItems
                            val safeCols = vaultCols.coerceIn(3, maxCols)
                            val haptic = LocalHapticFeedback.current
                            val vaultGridState = rememberLazyGridState()

                            LazyVerticalGrid(
                                state = vaultGridState,
                                columns = GridCells.Fixed(safeCols),
                                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .gridPinchToZoomGesture(
                                        currentColumns = safeCols,
                                        maxColumns = maxCols,
                                        haptic = haptic,
                                        onColumnChange = { viewModel.setGridColumns(it) }
                                    )
                                    .gridDragToSelectGesture(
                                        gridState = vaultGridState,
                                        haptic = haptic,
                                        onItemHit = { index ->
                                            val flatItems = uiState.currentFolderItems
                                            if (index in flatItems.indices) {
                                                val item = flatItems[index]
                                                if (!uiState.isSelectionMode) {
                                                    viewModel.enterSelectionMode(item)
                                                } else {
                                                    viewModel.toggleItemSelection(item)
                                                }
                                            }
                                        }
                                    )
                            ) {
                                groupedItems.forEach { (sectionTitle, sectionItems) ->
                                    val isSectionAllSelected = uiState.selectedItems.containsAll(sectionItems) && sectionItems.isNotEmpty()

                                    // Render Section Header
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        GridSectionHeader(
                                            title = sectionTitle,
                                            count = sectionItems.size,
                                            isAllSelected = isSectionAllSelected,
                                            onSectionSelectToggle = {
                                                viewModel.toggleSectionSelection(sectionItems)
                                            }
                                        )
                                    }

                                    // Render Items di Seksi Tersebut
                                    items(
                                        items = sectionItems,
                                        key = { it.id }
                                    ) { item ->
                                        val isSelected = uiState.selectedItems.contains(item)

                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .then(
                                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
                                                    else Modifier
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        if (uiState.isSelectionMode) {
                                                            viewModel.toggleItemSelection(item)
                                                        } else {
                                                            viewModel.enterSelectionMode(item)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!uiState.isSelectionMode) {
                                                            viewModel.enterSelectionMode(item)
                                                        } else {
                                                            viewModel.toggleItemSelection(item)
                                                        }
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(File(item.vaultFilePath))
                                                    .crossfade(false)
                                                    .size(256)
                                                    .precision(coil.size.Precision.INEXACT)
                                                    .build(),
                                                imageLoader = vaultImageLoader,
                                                contentDescription = item.originalName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            if (uiState.isSelectionMode) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
                                                )

                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingDockContainer(
                currentDestination = DockDestination.VAULT,
                isVisible = true,
                mediaTypeFilter = uiState.mediaTypeFilter,
                onFilterChange = { viewModel.setMediaTypeFilter(it) },
                onNavigate = { destination ->
                    when (destination) {
                        DockDestination.GALLERY -> {
                            viewModel.lockVault()
                            onGalleryClick()
                        }
                        DockDestination.ALBUMS -> {
                            viewModel.lockVault()
                            onAlbumClick()
                        }
                        DockDestination.TRASH -> {
                            viewModel.lockVault()
                            onTrashClick()
                        }
                        DockDestination.VAULT -> {}
                        DockDestination.SETTINGS -> {
                            viewModel.lockVault()
                            onSettingsClick()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showSortSheet) {
                SortBottomSheet(
                    currentSortOption = uiState.sortOption,
                    onSortOptionSelected = { viewModel.setSortOption(it) },
                    onDismissRequest = { showSortSheet = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFolderCard(
    folder: VaultFolder,
    imageLoader: coil.ImageLoader,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (folder.coverFilePath.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(folder.coverFilePath))
                            .size(256)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = folder.folderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${folder.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${folder.count} foto tersembunyi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun rememberFragmentActivity(context: Context): FragmentActivity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
