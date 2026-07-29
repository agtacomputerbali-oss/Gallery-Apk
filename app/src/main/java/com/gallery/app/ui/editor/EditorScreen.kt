package com.gallery.app.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.ui.text.input.KeyboardType
import com.gallery.app.data.worker.MediaStoreObserver
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.app.ui.editor.components.CropOverlay
import com.gallery.app.ui.editor.model.CropRatio
import com.gallery.app.ui.editor.model.DoodlePath
import com.gallery.app.ui.editor.model.ExportFormat
import com.gallery.app.ui.editor.model.ExtendedFilterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    photoUri: Uri,
    onBackClick: () -> Unit,
    onSaveSuccess: (Uri) -> Unit = { onBackClick() },
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExportSheet by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // Display container bounds
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // Brush state
    var selectedBrushColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }
    val currentPathPoints = remember { mutableStateListOf<Offset>() }

    LaunchedEffect(photoUri) {
        viewModel.loadPhoto(photoUri)
    }

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collectLatest { event ->
            when (event) {
                is EditorSaveEvent.Success -> {
                    Toast.makeText(context, "Foto berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    try {
                        coil.Coil.imageLoader(context).memoryCache?.clear()
                    } catch (e: Exception) {
                        // ignore cache clear error if any
                    }
                    MediaStoreObserver.triggerImmediateRefresh()
                    onSaveSuccess(event.uri)
                }
                is EditorSaveEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Dynamic Live Preview ColorFilter calculation (Adjustments + Filter Presets)
    val composeColorFilter = remember(uiState.adjustments, uiState.selectedFilter) {
        val cm = android.graphics.ColorMatrix()
        if (!uiState.adjustments.isDefault) {
            val bOffset = uiState.adjustments.brightness
            cm.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, bOffset,
                        0f, 1f, 0f, 0f, bOffset,
                        0f, 0f, 1f, 0f, bOffset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            val cScale = (uiState.adjustments.contrast + 100f) / 100f
            val cTranslate = (1f - cScale) * 128f
            cm.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        cScale, 0f, 0f, 0f, cTranslate,
                        0f, cScale, 0f, 0f, cTranslate,
                        0f, 0f, cScale, 0f, cTranslate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            val satVal = (uiState.adjustments.saturation + 100f) / 100f
            val satMatrix = android.graphics.ColorMatrix().apply { setSaturation(satVal) }
            cm.postConcat(satMatrix)

            val warmthVal = uiState.adjustments.warmth
            cm.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1f + (warmthVal / 500f), 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f - (warmthVal / 500f), 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        if (uiState.selectedFilter != ExtendedFilterType.ORIGINAL) {
            cm.postConcat(android.graphics.ColorMatrix(uiState.selectedFilter.getColorArray()))
        }
        ColorFilter.colorMatrix(ColorMatrix(cm.array))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Foto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Batal")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportSheet = true }, enabled = !uiState.isSaving) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.8f))
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.bitmap != null -> {
                    val bitmap = uiState.bitmap!!

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Canvas Display Area with Live Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black)
                                .onGloballyPositioned { coords ->
                                    containerWidth = coords.size.width.toFloat()
                                    containerHeight = coords.size.height.toFloat()
                                }
                                .pointerInput(uiState.activeTab) {
                                    if (uiState.activeTab == EditorTab.DOODLE) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPathPoints.clear()
                                                currentPathPoints.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentPathPoints.add(change.position)
                                            },
                                            onDragEnd = {
                                                if (currentPathPoints.size >= 2) {
                                                    viewModel.addDoodlePath(
                                                        DoodlePath(
                                                            points = currentPathPoints.toList(),
                                                            color = selectedBrushColor,
                                                            strokeWidth = strokeWidth
                                                        )
                                                    )
                                                }
                                                currentPathPoints.clear()
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Live Interactive Bitmap Display
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Photo Preview",
                                contentScale = ContentScale.Fit,
                                colorFilter = composeColorFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationZ = uiState.transform.rotationDegrees
                                        scaleX = if (uiState.transform.flipHorizontal) -1f else 1f
                                        scaleY = if (uiState.transform.flipVertical) -1f else 1f
                                    }
                            )

                            // Interactive Crop Overlay Grid
                            if (uiState.activeTab == EditorTab.CROP) {
                                val isRotated = (uiState.transform.rotationDegrees % 180f) != 0f
                                val imgW = if (isRotated) bitmap.height.toFloat() else bitmap.width.toFloat()
                                val imgH = if (isRotated) bitmap.width.toFloat() else bitmap.height.toFloat()
                                CropOverlay(
                                    cropRatio = uiState.cropRatio,
                                    onCropRectChanged = { rect ->
                                        viewModel.updateCropRect(rect, containerWidth, containerHeight)
                                    },
                                    imageWidth = imgW,
                                    imageHeight = imgH,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Live Doodle & Text Overlay Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw completed doodle paths
                                uiState.doodleState.paths.forEach { path ->
                                    if (path.points.size >= 2) {
                                        for (i in 0 until path.points.size - 1) {
                                            drawLine(
                                                color = path.color,
                                                start = path.points[i],
                                                end = path.points[i + 1],
                                                strokeWidth = path.strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }

                                // Draw current active doodle path
                                if (currentPathPoints.size >= 2) {
                                    for (i in 0 until currentPathPoints.size - 1) {
                                        drawLine(
                                            color = selectedBrushColor,
                                            start = currentPathPoints[i],
                                            end = currentPathPoints[i + 1],
                                            strokeWidth = strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Control Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            // Tab Specific Tool Controls
                            when (uiState.activeTab) {
                                EditorTab.CROP -> {
                                    Text("Rasio Pemotongan (Crop)", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CropRatio.entries.forEach { ratio ->
                                            FilterChip(
                                                selected = uiState.cropRatio == ratio,
                                                onClick = { viewModel.setCropRatio(ratio) },
                                                label = { Text(ratio.displayName) }
                                            )
                                        }
                                    }
                                }

                                EditorTab.ADJUSTMENTS -> {
                                    Text("Penyetelan Gambar", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Kecerahan (Brightness): ${uiState.adjustments.brightness.toInt()}", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = uiState.adjustments.brightness,
                                            onValueChange = { val b = it; viewModel.updateAdjustments { a -> a.copy(brightness = b) } },
                                            valueRange = -100f..100f
                                        )

                                        Text("Kontras (Contrast): ${uiState.adjustments.contrast.toInt()}", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = uiState.adjustments.contrast,
                                            onValueChange = { val c = it; viewModel.updateAdjustments { a -> a.copy(contrast = c) } },
                                            valueRange = -100f..100f
                                        )

                                        Text("Saturasi (Saturation): ${uiState.adjustments.saturation.toInt()}", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = uiState.adjustments.saturation,
                                            onValueChange = { val s = it; viewModel.updateAdjustments { a -> a.copy(saturation = s) } },
                                            valueRange = -100f..100f
                                        )

                                        Text("Kehangatan Warna (Warmth): ${uiState.adjustments.warmth.toInt()}", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = uiState.adjustments.warmth,
                                            onValueChange = { val w = it; viewModel.updateAdjustments { a -> a.copy(warmth = w) } },
                                            valueRange = -100f..100f
                                        )
                                    }
                                }

                                EditorTab.ROTATE_FLIP -> {
                                    Text("Transformasi Geometri", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        IconButton(onClick = { viewModel.rotate90CounterClockwise() }) {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotasi Kiri 90°")
                                        }
                                        IconButton(onClick = { viewModel.rotate90Clockwise() }) {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotasi Kanan 90°")
                                        }
                                        IconButton(onClick = { viewModel.toggleFlipHorizontal() }) {
                                            Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip Horizontal")
                                        }
                                    }
                                }

                                EditorTab.FILTERS -> {
                                    Text("Preset Filter Visual", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ExtendedFilterType.entries.forEach { filter ->
                                            FilterChip(
                                                selected = uiState.selectedFilter == filter,
                                                onClick = { viewModel.setFilter(filter) },
                                                label = { Text(filter.displayName, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                EditorTab.DOODLE -> {
                                    Text("Coretan Lukis & Teks", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White, Color.Black).forEach { col ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(col)
                                                        .clickable { selectedBrushColor = col }
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = { showAddTextDialog = true }) {
                                                Icon(imageVector = Icons.Default.TextFields, contentDescription = "Tambah Teks")
                                            }
                                            IconButton(onClick = { viewModel.clearDoodle() }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus Coretan", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Editor Category Navigation Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                EditorTabButton(icon = Icons.Default.Crop, label = "Crop", isSelected = uiState.activeTab == EditorTab.CROP) { viewModel.setActiveTab(EditorTab.CROP) }
                                EditorTabButton(icon = Icons.Default.Tune, label = "Penyetelan", isSelected = uiState.activeTab == EditorTab.ADJUSTMENTS) { viewModel.setActiveTab(EditorTab.ADJUSTMENTS) }
                                EditorTabButton(icon = Icons.AutoMirrored.Filled.RotateRight, label = "Rotasi", isSelected = uiState.activeTab == EditorTab.ROTATE_FLIP) { viewModel.setActiveTab(EditorTab.ROTATE_FLIP) }
                                EditorTabButton(icon = Icons.Default.Palette, label = "Filter", isSelected = uiState.activeTab == EditorTab.FILTERS) { viewModel.setActiveTab(EditorTab.FILTERS) }
                                EditorTabButton(icon = Icons.Default.Brush, label = "Doodle", isSelected = uiState.activeTab == EditorTab.DOODLE) { viewModel.setActiveTab(EditorTab.DOODLE) }
                            }
                        }
                    }
                }
            }

            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Menyimpan foto...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // Modal Add Text Overlay Dialog
    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text(text = "Tambah Teks Overlay") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Teks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.addTextOverlay(inputText, selectedBrushColor)
                            inputText = ""
                        }
                        showAddTextDialog = false
                    }
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Export Options Sheet
    if (showExportSheet) {
        var expandedFormat by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            windowInsets = WindowInsets.ime
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Opsi Simpan & Ekspor Foto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Format Berkas", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedFormat,
                        onExpandedChange = { expandedFormat = !expandedFormat },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.exportOptions.format.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFormat) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFormat,
                            onDismissRequest = { expandedFormat = false }
                        ) {
                            ExportFormat.entries.forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.displayName) },
                                    onClick = {
                                        viewModel.setExportOptions(uiState.exportOptions.copy(format = fmt))
                                        expandedFormat = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Kualitas Kompresi (%)", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = uiState.exportOptions.quality.toFloat(),
                            onValueChange = { q -> viewModel.setExportOptions(uiState.exportOptions.copy(quality = q.toInt())) },
                            valueRange = 50f..100f,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.exportOptions.quality.toString(),
                            onValueChange = { input ->
                                val number = input.filter { it.isDigit() }.toIntOrNull()
                                if (number != null) {
                                    viewModel.setExportOptions(uiState.exportOptions.copy(quality = number.coerceIn(50, 100)))
                                } else if (input.isEmpty()) {
                                    viewModel.setExportOptions(uiState.exportOptions.copy(quality = 50))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showExportSheet = false
                        viewModel.saveChanges()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Simpan Foto Baru")
                }
            }
        }
    }
}


@Composable
private fun EditorTabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
