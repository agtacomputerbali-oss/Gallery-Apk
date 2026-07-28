package com.gallery.app.ui.editor.model

import androidx.compose.ui.graphics.ColorMatrix

enum class FilterType(val displayName: String) {
    ORIGINAL("Asli"),
    GRAYSCALE("Hitam Putih"),
    SEPIA("Sepia"),
    VINTAGE("Vintage"),
    INVERT("Invert"),
    WARM("Warm"),
    COOL("Cool");

    fun getColorMatrix(): ColorMatrix {
        return when (this) {
            ORIGINAL -> ColorMatrix()
            GRAYSCALE -> ColorMatrix(
                floatArrayOf(
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            VINTAGE -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0.1f, 0.1f, 0f, 10f,
                    0.1f, 0.8f, 0.1f, 0f, 10f,
                    0.1f, 0.1f, 0.5f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            INVERT -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            WARM -> ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 10f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            COOL -> ColorMatrix(
                floatArrayOf(
                    0.8f, 0f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }
}
