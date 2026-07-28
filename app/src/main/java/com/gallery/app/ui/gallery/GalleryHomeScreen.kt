package com.gallery.app.ui.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.ui.components.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onPhotoClick: (Int) -> Unit = {},
    onAlbumClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lazyPagingItems: LazyPagingItems<PhotoItem> = viewModel.photosState.collectAsLazyPagingItems()
    val multiSelectState by viewModel.multiSelectState.collectAsState()

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val isSuccess = result.resultCode == Activity.RESULT_OK
        viewModel.onDeleteCompleted(isSuccess)
        if (isSuccess) {
            lazyPagingItems.refresh()
        }
    }

    Scaffold(
        topBar = {
            if (multiSelectState.isSelectionMode) {
                TopAppBar(
                    title = { Text(text = "${multiSelectState.selectedCount} Dipilih") },
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
                                val shareIntent = viewModel.getShareIntent()
                                if (shareIntent != null) {
                                    context.startActivity(shareIntent)
                                }
                            },
                            enabled = multiSelectState.selectedCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bagikan"
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.hideSelectedPhotos {
                                    lazyPagingItems.refresh()
                                }
                            },
                            enabled = multiSelectState.selectedCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Sembunyikan ke Vault"
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.deleteSelectedPhotos(
                                    onIntentSenderReady = { intentSender ->
                                        deleteLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    },
                                    onDeleteSuccess = {
                                        lazyPagingItems.refresh()
                                    }
                                )
                            },
                            enabled = multiSelectState.selectedCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(text = "Foto") },
                    actions = {
                        IconButton(onClick = onAlbumClick) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Album"
                            )
                        }
                        IconButton(onClick = onTrashClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sampah"
                            )
                        }
                        IconButton(onClick = onVaultClick) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Hidden Vault"
                            )
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan"
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val refreshState = lazyPagingItems.loadState.refresh

            when {
                refreshState is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                refreshState is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Gagal memuat foto: ${(refreshState as LoadState.Error).error.localizedMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { lazyPagingItems.retry() }) {
                            Text(text = "Coba Lagi")
                        }
                    }
                }

                refreshState is LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum Ada Foto",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Foto dari penyimpanan perangkat Anda akan muncul di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index ->
                                lazyPagingItems[index]?.id ?: index
                            }
                        ) { index ->
                            val photo = lazyPagingItems[index]
                            if (photo != null) {
                                PhotoThumbnail(
                                    photo = photo,
                                    isSelectionMode = multiSelectState.isSelectionMode,
                                    isSelected = multiSelectState.selectedPhotos.contains(photo),
                                    onClick = {
                                        if (multiSelectState.isSelectionMode) {
                                            viewModel.togglePhotoSelection(photo)
                                        } else {
                                            onPhotoClick(index)
                                        }
                                    },
                                    onLongClick = {
                                        if (!multiSelectState.isSelectionMode) {
                                            viewModel.enterSelectionMode(photo)
                                        } else {
                                            viewModel.togglePhotoSelection(photo)
                                        }
                                    }
                                )
                            }
                        }

                        if (lazyPagingItems.loadState.append is LoadState.Loading) {
                            item(span = { GridItemSpan(3) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp)
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

