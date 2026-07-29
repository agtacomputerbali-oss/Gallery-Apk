package com.gallery.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.app.domain.model.MediaTypeFilter
import com.gallery.app.domain.model.VaultLockDelay
import com.gallery.app.ui.settings.components.AppThemeSegmentedRow
import com.gallery.app.ui.settings.components.ColorPickerPreferenceRow
import com.gallery.app.ui.settings.components.GridColumnPickerDialog
import com.gallery.app.ui.settings.components.PreferenceCategoryHeader
import com.gallery.app.ui.settings.components.PreferenceGroupCard
import com.gallery.app.ui.settings.components.PreferenceSwitchTile
import com.gallery.app.ui.settings.components.PreferenceTile
import com.gallery.app.ui.settings.components.SingleChoiceDialog
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onDuplicateClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme by viewModel.themeModeState.collectAsStateWithLifecycle()
    val indexedCount by viewModel.indexedCountState.collectAsStateWithLifecycle()
    val galleryCols by viewModel.galleryGridColumns.collectAsStateWithLifecycle()
    val albumCols by viewModel.albumGridColumns.collectAsStateWithLifecycle()
    val vaultCols by viewModel.vaultGridColumns.collectAsStateWithLifecycle()

    val defaultFilter by viewModel.defaultFilterState.collectAsStateWithLifecycle()
    val autoPlayVideo by viewModel.autoPlayVideoState.collectAsStateWithLifecycle()
    val muteVideo by viewModel.muteVideoState.collectAsStateWithLifecycle()
    val vaultLockDelay by viewModel.vaultLockDelayState.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColorState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showVaultDelayDialog by remember { mutableStateOf(false) }
    var showDefaultFilterDialog by remember { mutableStateOf(false) }
    var showGridPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messageEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category 1: Tampilan & Tema
            PreferenceCategoryHeader(title = "TAMPILAN & TEMA")
            PreferenceGroupCard {
                PreferenceTile(
                    title = "Tema Aplikasi",
                    icon = Icons.Default.Palette,
                    summary = "Pilih mode tampilan antarmuka"
                )
                AppThemeSegmentedRow(
                    selectedTheme = currentTheme,
                    onThemeSelected = { viewModel.setThemeMode(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceTile(
                    title = "Warna Aksen Material 3",
                    summary = accentColor.displayName
                )
                ColorPickerPreferenceRow(
                    selectedColor = accentColor,
                    onColorSelected = { viewModel.setAccentColor(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceTile(
                    title = "Tata Letak Grid & Kolom",
                    icon = Icons.Default.GridView,
                    summary = "Galeri: $galleryCols | Album: $albumCols | Vault: $vaultCols",
                    onClick = { showGridPickerDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Category 2: Filter & Pemutaran Media
            PreferenceCategoryHeader(title = "FILTER & PEMUTARAN MEDIA")
            PreferenceGroupCard {
                PreferenceTile(
                    title = "Filter Media Default Saat Dibuka",
                    icon = Icons.Default.FilterList,
                    summary = defaultFilter.label,
                    onClick = { showDefaultFilterDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceSwitchTile(
                    title = "Auto-Play Video di Peninjau",
                    icon = Icons.Default.PlayCircle,
                    summary = "Putar otomatis video saat dibuka di preview",
                    checked = autoPlayVideo,
                    onCheckedChange = { viewModel.setAutoPlayVideo(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceSwitchTile(
                    title = "Bungkam Suara Video Default",
                    summary = "Putar video dalam keadaan hening secara default",
                    checked = muteVideo,
                    onCheckedChange = { viewModel.setMuteVideoByDefault(it) }
                )
            }

            // Category 3: Keamanan & Hidden Vault
            PreferenceCategoryHeader(title = "KEAMANAN & HIDDEN VAULT")
            PreferenceGroupCard {
                PreferenceTile(
                    title = "Tenggang Waktu Kunci Vault",
                    icon = Icons.Default.Lock,
                    summary = vaultLockDelay.displayName,
                    onClick = { showVaultDelayDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Category 4: Organisasi & Penyimpanan
            PreferenceCategoryHeader(title = "ORGANISASI & PENYIMPANAN")
            PreferenceGroupCard {
                PreferenceTile(
                    title = "Metadata Cache (Room DB)",
                    icon = Icons.Default.Storage,
                    summary = "Foto Ter-index: $indexedCount foto"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceTile(
                    title = "Sinkronkan Database & Metadata",
                    icon = Icons.Default.Refresh,
                    summary = "Paksa pembaruan indeks MediaStore",
                    onClick = { viewModel.triggerManualReindex() }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceTile(
                    title = "Bersihkan Cache Thumbnail",
                    icon = Icons.Default.CleaningServices,
                    summary = "Kosongkan memori & disk cache Coil",
                    onClick = { viewModel.clearCoilThumbnailCache() }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                PreferenceTile(
                    title = "✨ Cari & Bersihkan Foto Duplikat",
                    icon = Icons.Default.CleaningServices,
                    summary = "Pindai perceptual hash (pHash) foto serupa",
                    onClick = onDuplicateClick,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Category 5: Tentang Aplikasi
            PreferenceCategoryHeader(title = "TENTANG APLIKASI")
            PreferenceGroupCard(modifier = Modifier.padding(bottom = 24.dp)) {
                PreferenceTile(
                    title = "OP Gallery",
                    icon = Icons.Default.Info,
                    summary = "Versi 1.11.0 — Filter Media, M3 Settings Hub & Professional Photo Editor"
                )
            }
        }
    }

    // Dialogs
    if (showVaultDelayDialog) {
        SingleChoiceDialog(
            title = "Tenggang Waktu Kunci Vault",
            options = VaultLockDelay.entries,
            selectedOption = vaultLockDelay,
            onOptionSelected = { viewModel.setVaultLockDelay(it) },
            onDismissRequest = { showVaultDelayDialog = false },
            getItemLabel = { it.displayName }
        )
    }

    if (showDefaultFilterDialog) {
        SingleChoiceDialog(
            title = "Filter Media Default Saat Dibuka",
            options = MediaTypeFilter.entries,
            selectedOption = defaultFilter,
            onOptionSelected = { viewModel.setDefaultMediaFilter(it) },
            onDismissRequest = { showDefaultFilterDialog = false },
            getItemLabel = { it.label }
        )
    }

    if (showGridPickerDialog) {
        GridColumnPickerDialog(
            galleryCols = galleryCols,
            albumCols = albumCols,
            vaultCols = vaultCols,
            onGalleryColsChange = { viewModel.setGalleryGridColumns(it) },
            onAlbumColsChange = { viewModel.setAlbumGridColumns(it) },
            onVaultColsChange = { viewModel.setVaultGridColumns(it) },
            onDismissRequest = { showGridPickerDialog = false }
        )
    }
}
