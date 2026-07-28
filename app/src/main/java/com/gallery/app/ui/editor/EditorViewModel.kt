package com.gallery.app.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.domain.usecase.SaveEditedPhotoUseCase
import com.gallery.app.ui.editor.model.CropRatio
import com.gallery.app.ui.editor.model.FilterType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EditorUiState(
    val bitmap: Bitmap? = null,
    val cropRatio: CropRatio = CropRatio.FREE,
    val selectedFilter: FilterType = FilterType.ORIGINAL,
    val rotationDegrees: Float = 0f,
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

    private var rawBitmap: Bitmap? = null
    private var currentCropRect: Rect? = null
    private var containerWidth: Float = 0f
    private var containerHeight: Float = 0f

    fun loadPhoto(uri: Uri) {
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
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
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

    fun setCropRatio(ratio: CropRatio) {
        _uiState.update { it.copy(cropRatio = ratio) }
    }

    fun setFilter(filter: FilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun rotate90() {
        _uiState.update { it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f) }
    }

    fun updateCropRect(rect: Rect, displayWidth: Float, displayHeight: Float) {
        currentCropRect = rect
        containerWidth = displayWidth
        containerHeight = displayHeight
    }

    fun saveChanges(onSuccess: (Uri) -> Unit) {
        val sourceBitmap = rawBitmap ?: return
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val processedBitmap = processBitmap(
                    source = sourceBitmap,
                    rotation = _uiState.value.rotationDegrees,
                    filter = _uiState.value.selectedFilter,
                    cropRect = currentCropRect,
                    displayWidth = containerWidth,
                    displayHeight = containerHeight
                )

                val savedUri = saveEditedPhotoUseCase(processedBitmap)
                if (savedUri != null) {
                    _uiState.update { it.copy(isSaving = false, saveSuccessUri = savedUri) }
                    onSuccess(savedUri)
                } else {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Gagal menyimpan gambar.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    private suspend fun processBitmap(
        source: Bitmap,
        rotation: Float,
        filter: FilterType,
        cropRect: Rect?,
        displayWidth: Float,
        displayHeight: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        var result = source

        // 1. Rotasi
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
        }

        // 2. Crop
        if (cropRect != null && displayWidth > 0 && displayHeight > 0) {
            val scaleX = result.width.toFloat() / displayWidth
            val scaleY = result.height.toFloat() / displayHeight

            val cropLeft = (cropRect.left * scaleX).toInt().coerceIn(0, result.width - 1)
            val cropTop = (cropRect.top * scaleY).toInt().coerceIn(0, result.height - 1)
            val cropWidth = (cropRect.width * scaleX).toInt().coerceIn(1, result.width - cropLeft)
            val cropHeight = (cropRect.height * scaleY).toInt().coerceIn(1, result.height - cropTop)

            result = Bitmap.createBitmap(result, cropLeft, cropTop, cropWidth, cropHeight)
        }

        // 3. Filter Warna
        if (filter != FilterType.ORIGINAL) {
            val filteredBitmap = Bitmap.createBitmap(result.width, result.height, result.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(filteredBitmap)
            val paint = Paint().apply {
                val matrix = ColorMatrix(filter.getColorMatrix().values)
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(result, 0f, 0f, paint)
            result = filteredBitmap
        }

        result
    }

    override fun onCleared() {
        super.onCleared()
        rawBitmap?.recycle()
        rawBitmap = null
    }
}
