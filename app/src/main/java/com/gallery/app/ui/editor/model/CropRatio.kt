package com.gallery.app.ui.editor.model

enum class CropRatio(val displayName: String, val ratio: Float?) {
    FREE("Bebas", null),
    SQUARE("1:1", 1.0f),
    RATIO_4_3("4:3", 4.0f / 3.0f),
    RATIO_16_9("16:9", 16.0f / 9.0f)
}
