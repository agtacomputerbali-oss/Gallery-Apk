package com.gallery.app.ui.viewer

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.gallery.app.domain.model.PhotoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    initialIndex: Int,
    onBackClick: () -> Unit,
    onEditClick: (Uri) -> Unit = {},
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lazyPagingItems: LazyPagingItems<PhotoItem> = viewModel.photosState.collectAsLazyPagingItems()
    var isOverlayVisible by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lazyPagingItems.refresh()
        }
    }

    val totalCount = lazyPagingItems.itemCount
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (totalCount - 1).coerceAtLeast(0)),
        pageCount = { totalCount }
    )

    val currentPhoto = if (totalCount > 0 && pagerState.currentPage < totalCount) {
        lazyPagingItems[pagerState.currentPage]
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (totalCount > 0) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = lazyPagingItems[page]
                if (photo != null) {
                    ZoomableImage(
                        photo = photo,
                        onTap = { isOverlayVisible = !isOverlayVisible },
                        onZoomStateChanged = { zoomed -> isZoomed = zoomed }
                    )
                }
            }
        }

        // Top Bar Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentPhoto?.displayName ?: "Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentPhoto != null && currentPhoto.dateTaken > 0) {
                            val formattedDate = SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                Locale.getDefault()
                            ).format(Date(currentPhoto.dateTaken))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }

        // Bottom Bar Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentPhoto?.let { photo ->
                                val shareIntent = viewModel.sharePhoto(photo)
                                if (shareIntent != null) {
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        enabled = currentPhoto != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            currentPhoto?.let { photo ->
                                onEditClick(photo.uri)
                            }
                        },
                        enabled = currentPhoto != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            currentPhoto?.let { photo ->
                                viewModel.deletePhoto(
                                    photo = photo,
                                    onIntentSenderReady = { intentSender ->
                                        deleteLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    },
                                    onDeleteSuccess = {
                                        lazyPagingItems.refresh()
                                    }
                                )
                            }
                        },
                        enabled = currentPhoto != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* Detail Info */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}


