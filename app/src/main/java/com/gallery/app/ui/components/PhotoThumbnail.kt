package com.gallery.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.domain.model.PhotoItem

import coil.size.Precision

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoThumbnail(
    photo: PhotoItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
    } else Modifier

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(borderModifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .memoryCacheKey("${photo.uri}_${photo.dateTaken}")
                .crossfade(false)
                .size(256)
                .precision(Precision.INEXACT)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (photo.mimeType.startsWith("video/")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
            )

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isSelected) "Selected" else "Unselected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
            )
        }
    }
}

