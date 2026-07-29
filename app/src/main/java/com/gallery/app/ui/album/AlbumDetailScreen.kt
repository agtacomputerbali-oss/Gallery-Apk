package com.gallery.app.ui.album

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.gallery.app.domain.model.PhotoItem
import com.gallery.app.ui.components.DockDestination
import com.gallery.app.ui.components.FloatingDockContainer
import com.gallery.app.ui.components.GridColumnToggleButton
import com.gallery.app.ui.components.PhotoThumbnail
import com.gallery.app.ui.components.SortBottomSheet
import com.gallery.app.ui.gallery.rememberMaxGridColumns
import com.gallery.app.ui.gallery.GalleryUiEvent
import com.gallery.app.ui.gallery.rememberFloatingDockVisibility
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.ui.platform.LocalHapticFeedback
import com.gallery.app.ui.components.gridPinchToZoomGesture
import com.gallery.app.ui.components.gridDragToSelectGesture
import com.gallery.app.ui.gallery.model.GalleryItemModel
import com.gallery.app.ui.components.GridSectionHeader
import com.gallery.app.ui.gallery.getSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    onPhotoClick: (PhotoItem) -> Unit = {},
    onBackClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onAlbumClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lazyPagingItems: LazyPagingItems<GalleryItemModel> = viewModel.photosState.collectAsLazyPagingItems()
    val multiSelectState by viewModel.multiSelectState.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val albumCols by viewModel.albumGridColumns.collectAsStateWithLifecycle()
    val mediaTypeFilter by viewModel.mediaTypeFilter.collectAsStateWithLifecycle()


    var showSortSheet by remember { mutableStateOf(false) }
    var showCopyFolderPicker by remember { mutableStateOf(false) }
    var showMoveFolderPicker by remember { mutableStateOf(false) }

    val maxCols = rememberMaxGridColumns()
    val safeCols = albumCols.coerceIn(3, maxCols)
    val gridState = rememberLazyGridState()
    val isDockVisible by rememberFloatingDockVisibility(gridState)

    val snackbarHostState = remember { SnackbarHostState() }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val isSuccess = result.resultCode == Activity.RESULT_OK
        viewModel.onDeleteCompleted(isSuccess)
        if (isSuccess) {
            lazyPagingItems.refresh()
        }
    }

    val hideLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val isSuccess = result.resultCode == Activity.RESULT_OK
        viewModel.onHideCompleted(isSuccess)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is GalleryUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is GalleryUiEvent.LaunchHideConfirmation -> {
                    hideLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is GalleryUiEvent.LaunchDeleteConfirmation -> {
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is GalleryUiEvent.RefreshMedia -> {
                    lazyPagingItems.refresh()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = viewModel.bucketName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    GridColumnToggleButton(
                        currentColumns = albumCols,
                        maxColumns = maxCols,
                        onColumnChange = { viewModel.setGridColumns(it) }
                    )
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Urutkan"
                        )
                    }
                    IconButton(onClick = { viewModel.enterSelectionModeDirect() }) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Seleksi Foto"
                        )
                    }
                }
            )
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
                            text = "Gagal memuat foto album: ${refreshState.error.localizedMessage}",
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
                            text = "Album Ini Kosong",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                else -> {
                    val haptic = LocalHapticFeedback.current
                    val pullToRefreshState = rememberPullToRefreshState()

                    if (pullToRefreshState.isRefreshing) {
                        LaunchedEffect(true) {
                            lazyPagingItems.refresh()
                            pullToRefreshState.endRefresh()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(pullToRefreshState.nestedScrollConnection)
                    ) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(safeCols),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 100.dp, start = 2.dp, end = 2.dp),
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
                                    gridState = gridState,
                                    haptic = haptic,
                                    onItemHit = { index ->
                                        if (index in 0 until lazyPagingItems.itemCount) {
                                            val item = lazyPagingItems[index]
                                            if (item is GalleryItemModel.PhotoModel) {
                                                if (!multiSelectState.isSelectionMode) {
                                                    viewModel.enterSelectionMode(item.photo)
                                                } else {
                                                    viewModel.addPhotosToSelection(listOf(item.photo))
                                                }
                                            }
                                        }
                                    }
                                )
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { item ->
                                    when (item) {
                                        is GalleryItemModel.HeaderModel -> item.id
                                        is GalleryItemModel.PhotoModel -> item.photo.id
                                    }
                                },
                                span = { index ->
                                    val item = lazyPagingItems[index]
                                    if (item is GalleryItemModel.HeaderModel) {
                                        GridItemSpan(maxLineSpan)
                                    } else {
                                        GridItemSpan(1)
                                    }
                                }
                            ) { index ->
                                when (val item = lazyPagingItems[index]) {
                                    is GalleryItemModel.HeaderModel -> {
                                        val sectionPhotos = remember(lazyPagingItems.itemSnapshotList.items, item.dateGroupKey, sortOption) {
                                            lazyPagingItems.itemSnapshotList.items
                                                .filterIsInstance<GalleryItemModel.PhotoModel>()
                                                .map { it.photo }
                                                .filter { photo -> getSectionTitle(photo, sortOption) == item.dateGroupKey }
                                        }
                                        val isAllSelected = remember(multiSelectState.selectedPhotos, sectionPhotos) {
                                            derivedStateOf {
                                                sectionPhotos.isNotEmpty() && multiSelectState.selectedPhotos.containsAll(sectionPhotos)
                                            }
                                        }
                                        GridSectionHeader(
                                            title = item.title,
                                            count = sectionPhotos.size,
                                            isAllSelected = isAllSelected.value,
                                            onSectionSelectToggle = {
                                                viewModel.toggleSectionSelection(sectionPhotos)
                                            }
                                        )
                                    }
                                    is GalleryItemModel.PhotoModel -> {
                                        val photo = item.photo
                                        val isSelected by remember(multiSelectState.selectedPhotos, photo.id) {
                                            derivedStateOf { multiSelectState.selectedPhotos.contains(photo) }
                                        }
                                        PhotoThumbnail(
                                            photo = photo,
                                            isSelectionMode = multiSelectState.isSelectionMode,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (multiSelectState.isSelectionMode) {
                                                    viewModel.togglePhotoSelection(photo)
                                                } else {
                                                    onPhotoClick(photo)
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
                                    null -> {}
                                }
                            }

                            if (lazyPagingItems.loadState.append is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
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

                        if (pullToRefreshState.isRefreshing || pullToRefreshState.verticalOffset > 0f) {
                            PullToRefreshContainer(
                                state = pullToRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }

            FloatingDockContainer(

                currentDestination = DockDestination.ALBUMS,
                isVisible = isDockVisible || multiSelectState.isSelectionMode,
                isSelectionMode = multiSelectState.isSelectionMode,
                selectedCount = multiSelectState.selectedCount,
                mediaTypeFilter = mediaTypeFilter,
                onFilterChange = { viewModel.setMediaTypeFilter(it) },
                onNavigate = { destination ->
                    when (destination) {
                        DockDestination.GALLERY -> onGalleryClick()
                        DockDestination.ALBUMS -> onAlbumClick()
                        DockDestination.TRASH -> onTrashClick()
                        DockDestination.VAULT -> onVaultClick()
                        DockDestination.SETTINGS -> onSettingsClick()
                    }
                },
                onShare = {
                    val shareIntent = viewModel.getShareIntent()
                    if (shareIntent != null) {
                        context.startActivity(shareIntent)
                    }
                },
                onHideToVault = {
                    viewModel.hideSelectedPhotos()
                },
                onDelete = {
                    viewModel.deleteSelectedPhotos()
                },
                onCopyTo = {
                    viewModel.loadFolderList()
                    showCopyFolderPicker = true
                },
                onMoveTo = {
                    viewModel.loadFolderList()
                    showMoveFolderPicker = true
                },
                onCancelSelection = {
                    viewModel.exitSelectionMode()
                },
                onToggleSelectAll = {
                    val loadedPhotos = lazyPagingItems.itemSnapshotList.items
                        .filterIsInstance<GalleryItemModel.PhotoModel>()
                        .map { it.photo }
                    viewModel.toggleSelectAll(loadedPhotos)
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
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSortOption = sortOption,
            onSortOptionSelected = { viewModel.setSortOption(it) },
            onDismissRequest = { showSortSheet = false }
        )
    }

    if (showCopyFolderPicker) {
        val folders by viewModel.folderList.collectAsStateWithLifecycle()
        com.gallery.app.ui.components.FolderPickerBottomSheet(
            title = "Salin Foto ke Folder",
            folders = folders,
            onFolderSelected = { targetFolder ->
                viewModel.copySelectedPhotos(targetFolder)
            },
            onCreateNewFolder = { newFolder ->
                viewModel.createNewFolder(newFolder)
            },
            onDismissRequest = { showCopyFolderPicker = false }
        )
    }

    if (showMoveFolderPicker) {
        val folders by viewModel.folderList.collectAsStateWithLifecycle()
        com.gallery.app.ui.components.FolderPickerBottomSheet(
            title = "Pindahkan Foto ke Folder",
            folders = folders,
            onFolderSelected = { targetFolder ->
                viewModel.moveSelectedPhotos(targetFolder)
            },
            onCreateNewFolder = { newFolder ->
                viewModel.createNewFolder(newFolder)
            },
            onDismissRequest = { showMoveFolderPicker = false }
        )
    }
}
