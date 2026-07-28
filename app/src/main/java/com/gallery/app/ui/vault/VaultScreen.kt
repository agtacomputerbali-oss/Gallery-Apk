package com.gallery.app.ui.vault

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.domain.model.VaultItem
import com.gallery.app.ui.vault.components.VaultLockScreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(
    onBackClick: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

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
        Scaffold(
            topBar = {
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

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.vaultItems,
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
                                            .size(256)
                                            .crossfade(true)
                                            .build(),
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

