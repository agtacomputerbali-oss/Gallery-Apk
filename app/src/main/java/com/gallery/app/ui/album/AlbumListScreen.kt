package com.gallery.app.ui.album

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.domain.model.Album
import com.gallery.app.domain.model.SmartAlbum
import com.gallery.app.domain.model.SmartAlbumType
import com.gallery.app.ui.components.DockDestination
import com.gallery.app.ui.components.FloatingDockContainer
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel = hiltViewModel(),
    onAlbumClick: (Album) -> Unit = {},
    onSmartAlbumClick: (SmartAlbum) -> Unit = {},
    onBackClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val multiSelectState by viewModel.multiSelectState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val hideLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val isSuccess = result.resultCode == Activity.RESULT_OK
        viewModel.onHideCompleted(isSuccess)
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val isSuccess = result.resultCode == Activity.RESULT_OK
        viewModel.onDeleteCompleted(isSuccess)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AlbumListUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AlbumListUiEvent.LaunchHideConfirmation -> {
                    hideLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is AlbumListUiEvent.LaunchDeleteConfirmation -> {
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is AlbumListUiEvent.RefreshAlbums -> {
                    // StateFlow automatically refreshes
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    if (multiSelectState.isSelectionMode) {
                        Text(text = "${multiSelectState.selectedCount} Album Dipilih")
                    } else {
                        Text(text = "Album")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (multiSelectState.isSelectionMode) {
                            viewModel.exitSelectionMode()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (!multiSelectState.isSelectionMode) {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Buat Folder Baru"
                            )
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is AlbumListUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is AlbumListUiState.Error -> {
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
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    is AlbumListUiState.Success -> {
                        if (state.albums.isEmpty() && state.smartAlbums.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Belum Ada Album",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Smart Albums Section Header
                                if (state.smartAlbums.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Smart Albums (Otomatis)",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    items(
                                        items = state.smartAlbums,
                                        key = { smartAlbum -> "smart_${smartAlbum.type.name}" }
                                    ) { smartAlbum ->
                                        SmartAlbumCard(
                                            smartAlbum = smartAlbum,
                                            onClick = {
                                                if (!multiSelectState.isSelectionMode) {
                                                    onSmartAlbumClick(smartAlbum)
                                                }
                                            }
                                        )
                                    }

                                    item(span = { GridItemSpan(2) }) {
                                        Text(
                                            text = "Album Perangkat",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                        )
                                    }
                                }

                                // Manual Albums Section
                                items(
                                    items = state.albums,
                                    key = { album -> album.id }
                                ) { album ->
                                    val isSelected = multiSelectState.selectedAlbumIds.contains(album.id)
                                    AlbumCard(
                                        album = album,
                                        isSelectionMode = multiSelectState.isSelectionMode,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (multiSelectState.isSelectionMode) {
                                                viewModel.toggleAlbumSelection(album.id)
                                            } else {
                                                onAlbumClick(album)
                                            }
                                        },
                                        onLongClick = {
                                            if (!multiSelectState.isSelectionMode) {
                                                viewModel.enterSelectionMode(album.id)
                                            } else {
                                                viewModel.toggleAlbumSelection(album.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingDockContainer(
            currentDestination = DockDestination.ALBUMS,
            isVisible = true,
            isSelectionMode = multiSelectState.isSelectionMode,
            selectedCount = multiSelectState.selectedCount,
            onNavigate = { destination ->
                when (destination) {
                    DockDestination.GALLERY -> onGalleryClick()
                    DockDestination.ALBUMS -> {}
                    DockDestination.TRASH -> onTrashClick()
                    DockDestination.VAULT -> onVaultClick()
                    DockDestination.SETTINGS -> onSettingsClick()
                }
            },
            onHideToVault = {
                viewModel.hideSelectedAlbums()
            },
            onDelete = {
                viewModel.deleteSelectedAlbums()
            },
            onCancelSelection = {
                viewModel.exitSelectionMode()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }

    if (showCreateFolderDialog) {
        com.gallery.app.ui.components.CreateFolderAlertDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                showCreateFolderDialog = false
                viewModel.createNewFolder(folderName)
            }
        )
    }
}

@Composable
private fun SmartAlbumCard(
    smartAlbum: SmartAlbum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (smartAlbum.type) {
        SmartAlbumType.VIDEOS -> Icons.Default.PlayCircle
        SmartAlbumType.SCREENSHOTS -> Icons.Default.Smartphone
        SmartAlbumType.HAS_LOCATION -> Icons.Default.LocationOn
        SmartAlbumType.SELFIES -> Icons.Default.CameraFront
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (smartAlbum.coverUri != null) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(smartAlbum.coverUri)
                            .size(256)
                            .crossfade(true)
                            .build(),
                        contentDescription = smartAlbum.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = smartAlbum.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Text(
                text = "${smartAlbum.photoCount} item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(
    album: Album,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (album.coverUri != null) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(album.coverUri)
                            .size(256)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Text(
                text = "${album.photoCount} foto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
