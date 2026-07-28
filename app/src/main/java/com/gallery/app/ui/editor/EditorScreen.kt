package com.gallery.app.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.ui.editor.components.CropOverlay
import com.gallery.app.ui.editor.components.FilterStrip
import com.gallery.app.ui.editor.model.CropRatio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    photoUri: Uri,
    onBackClick: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(photoUri) {
        viewModel.loadPhoto(photoUri)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Foto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal"
                        )
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            color = Color.White
                        )
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.saveChanges {
                                    Toast.makeText(context, "Foto tersimpan!", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Simpan"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            // Workspace Editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else if (uiState.bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .onGloballyPositioned { coordinates ->
                                viewModel.updateCropRect(
                                    rect = androidx.compose.ui.geometry.Rect(
                                        0f,
                                        0f,
                                        coordinates.size.width.toFloat(),
                                        coordinates.size.height.toFloat()
                                    ),
                                    displayWidth = coordinates.size.width.toFloat(),
                                    displayHeight = coordinates.size.height.toFloat()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uiState.bitmap)
                                .build(),
                            contentDescription = "Edit Preview",
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(uiState.selectedFilter.getColorMatrix()),
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(uiState.rotationDegrees)
                        )

                        CropOverlay(
                            cropRatio = uiState.cropRatio,
                            onCropRectChanged = { rect ->
                                // Crop rect updated
                            }
                        )
                    }
                }
            }

            // Bottom Controls
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // Filter Bar
                    FilterStrip(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Crop Ratio & Rotate Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CropRatio.values().forEach { ratio ->
                                FilterChip(
                                    selected = uiState.cropRatio == ratio,
                                    onClick = { viewModel.setCropRatio(ratio) },
                                    label = { Text(text = ratio.displayName) }
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.rotate90() }) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotasi 90°",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
