package com.gallery.app.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.domain.usecase.SaveEditedPhotoUseCase
import com.gallery.app.ui.editor.model.AdjustmentState
import com.gallery.app.ui.editor.model.CropRatio
import com.gallery.app.ui.editor.model.DoodlePath
import com.gallery.app.ui.editor.model.DoodleState
import com.gallery.app.ui.editor.model.ExportOptions
import com.gallery.app.ui.editor.model.ExtendedFilterType
import com.gallery.app.ui.editor.model.TextOverlay
import com.gallery.app.ui.editor.model.TransformState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EditorTab {
    CROP, ADJUSTMENTS, ROTATE_FLIP, FILTERS, DOODLE
}

sealed class EditorSaveEvent {
    data class Success(val uri: Uri) : EditorSaveEvent()
    data class Error(val message: String) : EditorSaveEvent()
}

data class EditorUiState(
    val bitmap: Bitmap? = null,
    val cropPreviewBitmap: Bitmap? = null,
    val activeTab: EditorTab = EditorTab.CROP,
    val cropRatio: CropRatio = CropRatio.FREE,
    val selectedFilter: ExtendedFilterType = ExtendedFilterType.ORIGINAL,
    val adjustments: AdjustmentState = AdjustmentState(),
    val transform: TransformState = TransformState(),
    val doodleState: DoodleState = DoodleState(),
    val exportOptions: ExportOptions = ExportOptions(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccessUri: Uri? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val saveEditedPhotoUseCase: SaveEditedPhotoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _saveEvent = MutableSharedFlow<EditorSaveEvent>()
    val saveEvent: SharedFlow<EditorSaveEvent> = _saveEvent.asSharedFlow()

    private var currentPhotoUri: Uri? = null
    private var rawBitmap: Bitmap? = null
    private var currentCropRect: Rect? = null
    private var containerWidth: Float = 0f
    private var containerHeight: Float = 0f
    private var cropPreviewJob: Job? = null

    fun loadPhoto(uri: Uri) {
        currentPhotoUri = uri
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val loadedBitmap = loadScaledBitmap(uri)
                if (loadedBitmap != null) {
                    rawBitmap = loadedBitmap
                    _uiState.update {
                        it.copy(
                            bitmap = loadedBitmap,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal memuat gambar."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Terjadi kesalahan saat memuat gambar."
                    )
                }
            }
        }
    }

    private suspend fun loadScaledBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val maxDimension = 2048
        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false

        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun setActiveTab(tab: EditorTab) {
        if (tab != EditorTab.CROP) {
            cropPreviewJob?.cancel()
            _uiState.update { it.copy(activeTab = tab, cropPreviewBitmap = null) }
        } else {
            _uiState.update { it.copy(activeTab = tab) }
        }
    }

    fun setCropRatio(ratio: CropRatio) {
        _uiState.update { it.copy(cropRatio = ratio) }
    }

    fun setFilter(filter: ExtendedFilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun updateAdjustments(update: (AdjustmentState) -> AdjustmentState) {
        _uiState.update { it.copy(adjustments = update(it.adjustments)) }
    }

    fun rotate90Clockwise() {
        currentCropRect = null
        _uiState.update {
            val currentRot = it.transform.rotationDegrees
            it.copy(
                transform = it.transform.copy(rotationDegrees = (currentRot + 90f) % 360f),
                cropPreviewBitmap = null
            )
        }
    }

    fun rotate90CounterClockwise() {
        currentCropRect = null
        _uiState.update {
            val currentRot = it.transform.rotationDegrees
            val nextRot = if (currentRot - 90f < 0) currentRot + 270f else currentRot - 90f
            it.copy(
                transform = it.transform.copy(rotationDegrees = nextRot),
                cropPreviewBitmap = null
            )
        }
    }

    fun toggleFlipHorizontal() {
        currentCropRect = null
        _uiState.update {
            it.copy(
                transform = it.transform.copy(flipHorizontal = !it.transform.flipHorizontal),
                cropPreviewBitmap = null
            )
        }
    }

    fun toggleFlipVertical() {
        currentCropRect = null
        _uiState.update {
            it.copy(
                transform = it.transform.copy(flipVertical = !it.transform.flipVertical),
                cropPreviewBitmap = null
            )
        }
    }

    fun addDoodlePath(path: DoodlePath) {
        _uiState.update {
            val updatedPaths = it.doodleState.paths + path
            it.copy(doodleState = it.doodleState.copy(paths = updatedPaths))
        }
    }

    fun addTextOverlay(text: String, color: Color) {
        if (text.isBlank()) return
        _uiState.update {
            val overlay = TextOverlay(text = text.trim(), color = color)
            val updatedOverlays = it.doodleState.textOverlays + overlay
            it.copy(doodleState = it.doodleState.copy(textOverlays = updatedOverlays))
        }
    }

    fun clearDoodle() {
        _uiState.update { it.copy(doodleState = DoodleState()) }
    }

    fun setExportOptions(options: ExportOptions) {
        _uiState.update { it.copy(exportOptions = options) }
    }

    fun updateCropRect(rect: Rect, displayWidth: Float, displayHeight: Float) {
        currentCropRect = rect
        containerWidth = displayWidth
        containerHeight = displayHeight

        cropPreviewJob?.cancel()
        val source = rawBitmap ?: return
        if (displayWidth <= 0f || displayHeight <= 0f) return

        cropPreviewJob = viewModelScope.launch(Dispatchers.Default) {
            delay(200L)
            val cropped = cropBitmapPreview(source, rect, displayWidth, displayHeight, _uiState.value.transform)
            _uiState.update { it.copy(cropPreviewBitmap = cropped) }
        }
    }

    private fun cropBitmapPreview(
        source: Bitmap,
        cropRect: Rect,
        displayWidth: Float,
        displayHeight: Float,
        transform: TransformState
    ): Bitmap? {
        return try {
            var result = source
            if (!transform.isDefault) {
                val matrix = Matrix().apply {
                    if (transform.rotationDegrees != 0f) postRotate(transform.rotationDegrees)
                    val sx = if (transform.flipHorizontal) -1f else 1f
                    val sy = if (transform.flipVertical) -1f else 1f
                    if (sx != 1f || sy != 1f) postScale(sx, sy)
                }
                result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
            }

            val isRotated90or270 = ((transform.rotationDegrees % 180f) != 0f)
            val effectiveDisplayWidth = if (isRotated90or270) displayHeight else displayWidth
            val effectiveDisplayHeight = if (isRotated90or270) displayWidth else displayHeight

            val scaleX = if (effectiveDisplayWidth > 0f) result.width.toFloat() / effectiveDisplayWidth else 1f
            val scaleY = if (effectiveDisplayHeight > 0f) result.height.toFloat() / effectiveDisplayHeight else 1f

            val cropLeft = (cropRect.left * scaleX).toInt().coerceIn(0, result.width - 1)
            val cropTop = (cropRect.top * scaleY).toInt().coerceIn(0, result.height - 1)
            val cropWidth = (cropRect.width * scaleX).toInt().coerceIn(1, result.width - cropLeft)
            val cropHeight = (cropRect.height * scaleY).toInt().coerceIn(1, result.height - cropTop)

            Bitmap.createBitmap(result, cropLeft, cropTop, cropWidth, cropHeight)
        } catch (e: Exception) {
            null
        }
    }

    fun saveChanges() {
        val sourceBitmap = rawBitmap ?: return
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                val processedBitmap = processBitmap(
                    source = sourceBitmap,
                    transform = state.transform,
                    filter = state.selectedFilter,
                    adjustments = state.adjustments,
                    doodleState = state.doodleState,
                    cropRect = currentCropRect,
                    displayWidth = containerWidth,
                    displayHeight = containerHeight
                )

                val savedUri = saveEditedPhotoUseCase(processedBitmap, state.exportOptions, currentPhotoUri)
                if (savedUri != null) {
                    _uiState.update { it.copy(isSaving = false, saveSuccessUri = savedUri) }
                    _saveEvent.emit(EditorSaveEvent.Success(savedUri))
                } else {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Gagal menyimpan gambar.") }
                    _saveEvent.emit(EditorSaveEvent.Error("Gagal menyimpan gambar."))
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Terjadi kesalahan saat menyimpan foto."
                _uiState.update { it.copy(isSaving = false, errorMessage = msg) }
                _saveEvent.emit(EditorSaveEvent.Error(msg))
            }
        }
    }

    private suspend fun processBitmap(
        source: Bitmap,
        transform: TransformState,
        filter: ExtendedFilterType,
        adjustments: AdjustmentState,
        doodleState: DoodleState,
        cropRect: Rect?,
        displayWidth: Float,
        displayHeight: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        var result = source

        // 1. Transformasi Matrix (Rotate & Flip H/V)
        if (!transform.isDefault) {
            val matrix = Matrix().apply {
                if (transform.rotationDegrees != 0f) {
                    postRotate(transform.rotationDegrees)
                }
                val sx = if (transform.flipHorizontal) -1f else 1f
                val sy = if (transform.flipVertical) -1f else 1f
                if (sx != 1f || sy != 1f) {
                    postScale(sx, sy)
                }
            }
            result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
        }

        // 2. Crop
        if (cropRect != null && displayWidth > 0f && displayHeight > 0f) {
            val bitmapW = result.width.toFloat()
            val bitmapH = result.height.toFloat()

            val scale = minOf(displayWidth / bitmapW, displayHeight / bitmapH)
            val drawnW = bitmapW * scale
            val drawnH = bitmapH * scale
            val offsetX = (displayWidth - drawnW) / 2f
            val offsetY = (displayHeight - drawnH) / 2f

            val cropLeftInImage = (cropRect.left - offsetX).coerceIn(0f, drawnW)
            val cropTopInImage = (cropRect.top - offsetY).coerceIn(0f, drawnH)
            val cropRightInImage = (cropRect.right - offsetX).coerceIn(0f, drawnW)
            val cropBottomInImage = (cropRect.bottom - offsetY).coerceIn(0f, drawnH)

            val cropLeft = (cropLeftInImage / drawnW * bitmapW).toInt().coerceIn(0, result.width - 1)
            val cropTop = (cropTopInImage / drawnH * bitmapH).toInt().coerceIn(0, result.height - 1)
            val cropWidth = ((cropRightInImage - cropLeftInImage) / drawnW * bitmapW).toInt().coerceIn(1, result.width - cropLeft)
            val cropHeight = ((cropBottomInImage - cropTopInImage) / drawnH * bitmapH).toInt().coerceIn(1, result.height - cropTop)

            result = Bitmap.createBitmap(result, cropLeft, cropTop, cropWidth, cropHeight)
        }

        // 3. Render Canvas (Adjustments + Filter + Vignette + Doodle + Text)
        val filteredBitmap = Bitmap.createBitmap(result.width, result.height, result.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filteredBitmap)

        // ColorMatrix Gabungan (Adjustments + Filter Preset)
        val finalColorMatrix = ColorMatrix()

        // Apply Adjustments (Brightness, Contrast, Saturation, Warmth)
        if (!adjustments.isDefault) {
            // Brightness (-100..+100) -> offset
            val bOffset = adjustments.brightness
            val brightnessMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, bOffset,
                    0f, 1f, 0f, 0f, bOffset,
                    0f, 0f, 1f, 0f, bOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(brightnessMatrix)

            // Contrast (-100..+100) -> scale
            val cScale = (adjustments.contrast + 100f) / 100f
            val cTranslate = (1f - cScale) * 128f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    cScale, 0f, 0f, 0f, cTranslate,
                    0f, cScale, 0f, 0f, cTranslate,
                    0f, 0f, cScale, 0f, cTranslate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(contrastMatrix)

            // Saturation (-100..+100)
            val satVal = (adjustments.saturation + 100f) / 100f
            val satMatrix = ColorMatrix().apply { setSaturation(satVal) }
            finalColorMatrix.postConcat(satMatrix)

            // Warmth / Temperature (-100..+100) -> adjust Red/Blue
            val warmthVal = adjustments.warmth
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f + (warmthVal / 500f), 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f - (warmthVal / 500f), 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(warmthMatrix)
        }

        // Apply Preset Filter
        if (filter != ExtendedFilterType.ORIGINAL) {
            finalColorMatrix.postConcat(ColorMatrix(filter.getColorArray()))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(finalColorMatrix)
        }
        canvas.drawBitmap(result, 0f, 0f, paint)

        // Render Vignette Gradient Overlay
        if (adjustments.vignette > 0f) {
            val cx = result.width / 2f
            val cy = result.height / 2f
            val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
            val vignetteAlpha = (adjustments.vignette / 100f * 0.75f).coerceIn(0f, 0.9f)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb((vignetteAlpha * 255).toInt(), 0, 0, 0)),
                    floatArrayOf(0.4f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), vignettePaint)
        }

        // Render Doodle Lines
        if (doodleState.paths.isNotEmpty()) {
            val doodleScaleX = result.width.toFloat() / (if (displayWidth > 0) displayWidth else result.width.toFloat())
            val doodleScaleY = result.height.toFloat() / (if (displayHeight > 0) displayHeight else result.height.toFloat())

            doodleState.paths.forEach { path ->
                if (path.points.size >= 2) {
                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = path.color.toArgb()
                        strokeWidth = path.strokeWidth * doodleScaleX
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    for (i in 0 until path.points.size - 1) {
                        val p1 = path.points[i]
                        val p2 = path.points[i + 1]
                        canvas.drawLine(
                            p1.x * doodleScaleX, p1.y * doodleScaleY,
                            p2.x * doodleScaleX, p2.y * doodleScaleY,
                            linePaint
                        )
                    }
                }
            }
        }

        // Render Text Overlays
        if (doodleState.textOverlays.isNotEmpty()) {
            doodleState.textOverlays.forEach { textOverlay ->
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textOverlay.color.toArgb()
                    textSize = textOverlay.textSizeSp * (result.height.toFloat() / 800f)
                    textAlign = Paint.Align.CENTER
                }
                val tx = result.width * textOverlay.xRatio
                val ty = result.height * textOverlay.yRatio
                canvas.drawText(textOverlay.text, tx, ty, textPaint)
            }
        }

        filteredBitmap
    }

    override fun onCleared() {
        super.onCleared()
        rawBitmap?.recycle()
        rawBitmap = null
    }
}
