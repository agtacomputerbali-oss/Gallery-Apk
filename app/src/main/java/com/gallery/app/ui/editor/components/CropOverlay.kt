package com.gallery.app.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gallery.app.ui.editor.model.CropRatio

@Composable
fun CropOverlay(
    cropRatio: CropRatio,
    onCropRectChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var cropRect by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(canvasSize, cropRatio) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val targetRatio = cropRatio.ratio
            val width: Float
            val height: Float
            if (targetRatio != null) {
                if (canvasSize.width / canvasSize.height > targetRatio) {
                    height = canvasSize.height * 0.8f
                    width = height * targetRatio
                } else {
                    width = canvasSize.width * 0.8f
                    height = width / targetRatio
                }
            } else {
                width = canvasSize.width * 0.8f
                height = canvasSize.height * 0.8f
            }
            val left = (canvasSize.width - width) / 2f
            val top = (canvasSize.height - height) / 2f
            cropRect = Rect(left, top, left + width, top + height)
            onCropRectChanged(cropRect)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(canvasSize, cropRatio) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (cropRect != Rect.Zero) {
                        var newWidth = cropRect.width * zoom
                        var newHeight = cropRect.height * zoom
                        val targetRatio = cropRatio.ratio

                        if (targetRatio != null) {
                            newHeight = newWidth / targetRatio
                        }

                        newWidth = newWidth.coerceIn(100f, canvasSize.width)
                        newHeight = newHeight.coerceIn(100f, canvasSize.height)

                        var newLeft = cropRect.left + pan.x - (newWidth - cropRect.width) / 2f
                        var newTop = cropRect.top + pan.y - (newHeight - cropRect.height) / 2f

                        newLeft = newLeft.coerceIn(0f, canvasSize.width - newWidth)
                        newTop = newTop.coerceIn(0f, canvasSize.height - newHeight)

                        cropRect = Rect(newLeft, newTop, newLeft + newWidth, newTop + newHeight)
                        onCropRectChanged(cropRect)
                    }
                }
            }
    ) {
        canvasSize = size

        if (cropRect != Rect.Zero) {
            // Background Dimming (4 outer rectangles)
            val dimColor = Color.Black.copy(alpha = 0.5f)
            drawRect(
                color = dimColor,
                topLeft = Offset.Zero,
                size = Size(size.width, cropRect.top)
            )
            drawRect(
                color = dimColor,
                topLeft = Offset(0f, cropRect.bottom),
                size = Size(size.width, size.height - cropRect.bottom)
            )
            drawRect(
                color = dimColor,
                topLeft = Offset(0f, cropRect.top),
                size = Size(cropRect.left, cropRect.height)
            )
            drawRect(
                color = dimColor,
                topLeft = Offset(cropRect.right, cropRect.top),
                size = Size(size.width - cropRect.right, cropRect.height)
            )

            // White Crop Border
            drawRect(
                color = Color.White,
                topLeft = cropRect.topLeft,
                size = cropRect.size,
                style = Stroke(width = 2.dp.toPx())
            )

            // Grid Lines (Rule of Thirds)
            val thirdWidth = cropRect.width / 3f
            val thirdHeight = cropRect.height / 3f

            for (i in 1..2) {
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cropRect.left + thirdWidth * i, cropRect.top),
                    end = Offset(cropRect.left + thirdWidth * i, cropRect.bottom),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cropRect.left, cropRect.top + thirdHeight * i),
                    end = Offset(cropRect.right, cropRect.top + thirdHeight * i),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}
