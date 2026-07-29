package com.gallery.app.ui.editor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset

data class AdjustmentState(
    val brightness: Float = 0f,    // -100f s/d +100f
    val contrast: Float = 0f,      // -100f s/d +100f
    val saturation: Float = 0f,    // -100f s/d +100f
    val warmth: Float = 0f,        // -100f s/d +100f
    val vignette: Float = 0f       // 0f s/d 100f
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f && vignette == 0f
}

data class TransformState(
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
) {
    val isDefault: Boolean
        get() = rotationDegrees == 0f && !flipHorizontal && !flipVertical
}

data class DoodlePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class TextOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val color: Color,
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.5f,
    val textSizeSp: Float = 24f
)

data class DoodleState(
    val paths: List<DoodlePath> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList()
) {
    val isEmpty: Boolean
        get() = paths.isEmpty() && textOverlays.isEmpty()
}

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    JPEG("jpg", "image/jpeg", "JPEG (.jpg)"),
    PNG("png", "image/png", "PNG (.png)"),
    WEBP("webp", "image/webp", "WEBP (.webp)")
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 90 // 50 s/d 100
)

enum class ExtendedFilterType(val displayName: String) {
    ORIGINAL("Asli"),
    VIVID("Zest / Vivid"),
    WARM_VINTAGE("Warm Vintage"),
    COOL_BREEZE("Cool Breeze"),
    DRAMATIC_BW("Dramatic B&W"),
    NOIR("Noir"),
    SEPIA("Sepia"),
    CINEMA("Cinema"),
    PASTEL("Pastel"),
    CYBERPUNK("Cyberpunk");

    fun getColorArray(): FloatArray {
        return when (this) {
            ORIGINAL -> floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            VIVID -> floatArrayOf(
                1.3f, 0f, 0f, 0f, 10f,
                0f, 1.3f, 0f, 0f, 10f,
                0f, 0f, 1.3f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
            WARM_VINTAGE -> floatArrayOf(
                1.1f, 0.1f, 0.1f, 0f, 15f,
                0.1f, 0.95f, 0.1f, 0f, 10f,
                0.05f, 0.1f, 0.75f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            )
            COOL_BREEZE -> floatArrayOf(
                0.85f, 0f, 0.1f, 0f, 0f,
                0f, 1.05f, 0.1f, 0f, 5f,
                0.1f, 0.1f, 1.25f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )
            DRAMATIC_BW -> floatArrayOf(
                0.4f, 0.6f, 0.1f, 0f, -20f,
                0.4f, 0.6f, 0.1f, 0f, -20f,
                0.4f, 0.6f, 0.1f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
            NOIR -> floatArrayOf(
                0.25f, 0.65f, 0.1f, 0f, -40f,
                0.25f, 0.65f, 0.1f, 0f, -40f,
                0.25f, 0.65f, 0.1f, 0f, -40f,
                0f, 0f, 0f, 1f, 0f
            )
            SEPIA -> floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            CINEMA -> floatArrayOf(
                0.9f, 0.1f, 0f, 0f, 5f,
                0f, 1.0f, 0.1f, 0f, 0f,
                0.1f, 0.1f, 1.2f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
            PASTEL -> floatArrayOf(
                0.9f, 0.1f, 0.1f, 0f, 30f,
                0.1f, 0.9f, 0.1f, 0f, 30f,
                0.1f, 0.1f, 0.9f, 0f, 35f,
                0f, 0f, 0f, 1f, 0f
            )
            CYBERPUNK -> floatArrayOf(
                1.2f, 0f, 0.2f, 0f, 20f,
                0f, 0.8f, 0.2f, 0f, -10f,
                0.3f, 0f, 1.4f, 0f, 30f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    }
}
