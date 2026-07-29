package com.gallery.app.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gallery.app.ui.editor.model.CropRatio
import kotlin.math.hypot

private enum class CropHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, CENTER
}

@Composable
fun CropOverlay(
    cropRatio: CropRatio,
    onCropRectChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    imageWidth: Float = 0f,
    imageHeight: Float = 0f
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var cropRect by remember { mutableStateOf(Rect.Zero) }
    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }

    val density = LocalDensity.current
    val touchRadiusPx = remember(density) { with(density) { 36.dp.toPx() } }
    val minSizePx = remember(density) { with(density) { 60.dp.toPx() } }
    val handleThicknessPx = remember(density) { with(density) { 4.dp.toPx() } }
    val handleLengthPx = remember(density) { with(density) { 20.dp.toPx() } }

    val imageBounds = remember(canvasSize, imageWidth, imageHeight) {
        val imgAspectRatio = if (imageWidth > 0f && imageHeight > 0f) imageWidth / imageHeight else null
        if (imgAspectRatio != null && canvasSize.width > 0f && canvasSize.height > 0f) {
            val scale = minOf(canvasSize.width / imageWidth, canvasSize.height / imageHeight)
            val drawnW = imageWidth * scale
            val drawnH = imageHeight * scale
            val offX = (canvasSize.width - drawnW) / 2f
            val offY = (canvasSize.height - drawnH) / 2f
            Rect(offX, offY, offX + drawnW, offY + drawnH)
        } else if (canvasSize.width > 0f && canvasSize.height > 0f) {
            Rect(0f, 0f, canvasSize.width, canvasSize.height)
        } else {
            Rect.Zero
        }
    }

    LaunchedEffect(imageBounds, cropRatio) {
        if (imageBounds.width > 0f && imageBounds.height > 0f) {
            val targetRatio = cropRatio.ratio
            val width: Float
            val height: Float
            if (targetRatio != null) {
                if (imageBounds.width / imageBounds.height > targetRatio) {
                    height = imageBounds.height * 0.8f
                    width = height * targetRatio
                } else {
                    width = imageBounds.width * 0.8f
                    height = width / targetRatio
                }
            } else {
                width = imageBounds.width * 0.85f
                height = imageBounds.height * 0.85f
            }
            val left = imageBounds.left + (imageBounds.width - width) / 2f
            val top = imageBounds.top + (imageBounds.height - height) / 2f
            cropRect = Rect(left, top, left + width, top + height)
            onCropRectChanged(cropRect)
        }
    }

    fun getHitHandle(point: Offset, rect: Rect): CropHandle {
        if (rect == Rect.Zero) return CropHandle.NONE

        // Corners
        if (hypot(point.x - rect.left, point.y - rect.top) <= touchRadiusPx) return CropHandle.TOP_LEFT
        if (hypot(point.x - rect.right, point.y - rect.top) <= touchRadiusPx) return CropHandle.TOP_RIGHT
        if (hypot(point.x - rect.left, point.y - rect.bottom) <= touchRadiusPx) return CropHandle.BOTTOM_LEFT
        if (hypot(point.x - rect.right, point.y - rect.bottom) <= touchRadiusPx) return CropHandle.BOTTOM_RIGHT

        // Edges
        if (point.x >= rect.left && point.x <= rect.right) {
            if (kotlin.math.abs(point.y - rect.top) <= touchRadiusPx) return CropHandle.TOP
            if (kotlin.math.abs(point.y - rect.bottom) <= touchRadiusPx) return CropHandle.BOTTOM
        }
        if (point.y >= rect.top && point.y <= rect.bottom) {
            if (kotlin.math.abs(point.x - rect.left) <= touchRadiusPx) return CropHandle.LEFT
            if (kotlin.math.abs(point.x - rect.right) <= touchRadiusPx) return CropHandle.RIGHT
        }

        // Center / Inner area
        if (rect.contains(point)) return CropHandle.CENTER

        return CropHandle.NONE
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(imageBounds, cropRatio) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = getHitHandle(offset, cropRect)
                    },
                    onDragEnd = { activeHandle = CropHandle.NONE },
                    onDragCancel = { activeHandle = CropHandle.NONE },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (cropRect == Rect.Zero || imageBounds == Rect.Zero || activeHandle == CropHandle.NONE) return@detectDragGestures

                        var left = cropRect.left
                        var top = cropRect.top
                        var right = cropRect.right
                        var bottom = cropRect.bottom
                        val targetRatio = cropRatio.ratio

                        if (activeHandle == CropHandle.CENTER) {
                            val newLeft = (left + dragAmount.x).coerceIn(imageBounds.left, imageBounds.right - cropRect.width)
                            val newTop = (top + dragAmount.y).coerceIn(imageBounds.top, imageBounds.bottom - cropRect.height)
                            cropRect = Rect(newLeft, newTop, newLeft + cropRect.width, newTop + cropRect.height)
                            onCropRectChanged(cropRect)
                            return@detectDragGestures
                        }

                        if (targetRatio == null) {
                            // MODE BEBAS (Free Crop)
                            when (activeHandle) {
                                CropHandle.TOP_LEFT -> {
                                    left = (left + dragAmount.x).coerceIn(imageBounds.left, right - minSizePx)
                                    top = (top + dragAmount.y).coerceIn(imageBounds.top, bottom - minSizePx)
                                }
                                CropHandle.TOP_RIGHT -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, imageBounds.right)
                                    top = (top + dragAmount.y).coerceIn(imageBounds.top, bottom - minSizePx)
                                }
                                CropHandle.BOTTOM_LEFT -> {
                                    left = (left + dragAmount.x).coerceIn(imageBounds.left, right - minSizePx)
                                    bottom = (bottom + dragAmount.y).coerceIn(top + minSizePx, imageBounds.bottom)
                                }
                                CropHandle.BOTTOM_RIGHT -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, imageBounds.right)
                                    bottom = (bottom + dragAmount.y).coerceIn(top + minSizePx, imageBounds.bottom)
                                }
                                CropHandle.LEFT -> {
                                    left = (left + dragAmount.x).coerceIn(imageBounds.left, right - minSizePx)
                                }
                                CropHandle.RIGHT -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, imageBounds.right)
                                }
                                CropHandle.TOP -> {
                                    top = (top + dragAmount.y).coerceIn(imageBounds.top, bottom - minSizePx)
                                }
                                CropHandle.BOTTOM -> {
                                    bottom = (bottom + dragAmount.y).coerceIn(top + minSizePx, imageBounds.bottom)
                                }
                                else -> {}
                            }
                        } else {
                            // MODE FIXED ASPECT RATIO
                            when (activeHandle) {
                                CropHandle.TOP_LEFT, CropHandle.LEFT, CropHandle.TOP -> {
                                    left = (left + dragAmount.x).coerceIn(imageBounds.left, right - minSizePx)
                                    val newW = right - left
                                    val newH = newW / targetRatio
                                    top = (bottom - newH).coerceIn(imageBounds.top, bottom - minSizePx)
                                }
                                CropHandle.TOP_RIGHT, CropHandle.RIGHT -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, imageBounds.right)
                                    val newW = right - left
                                    val newH = newW / targetRatio
                                    top = (bottom - newH).coerceIn(imageBounds.top, bottom - minSizePx)
                                }
                                CropHandle.BOTTOM_LEFT -> {
                                    left = (left + dragAmount.x).coerceIn(imageBounds.left, right - minSizePx)
                                    val newW = right - left
                                    val newH = newW / targetRatio
                                    bottom = (top + newH).coerceIn(top + minSizePx, imageBounds.bottom)
                                }
                                CropHandle.BOTTOM_RIGHT, CropHandle.BOTTOM -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, imageBounds.right)
                                    val newW = right - left
                                    val newH = newW / targetRatio
                                    bottom = (top + newH).coerceIn(top + minSizePx, imageBounds.bottom)
                                }
                                else -> {}
                            }
                        }

                        cropRect = Rect(left, top, right, bottom)
                        onCropRectChanged(cropRect)
                    }
                )
            }
    ) {
        canvasSize = size

        if (cropRect != Rect.Zero) {
            // Background Dimming (4 outer rectangles)
            val dimColor = Color.Black.copy(alpha = 0.5f)
            drawRect(color = dimColor, topLeft = Offset.Zero, size = Size(size.width, cropRect.top))
            drawRect(color = dimColor, topLeft = Offset(0f, cropRect.bottom), size = Size(size.width, size.height - cropRect.bottom))
            drawRect(color = dimColor, topLeft = Offset(0f, cropRect.top), size = Size(cropRect.left, cropRect.height))
            drawRect(color = dimColor, topLeft = Offset(cropRect.right, cropRect.top), size = Size(size.width - cropRect.right, cropRect.height))

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
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(cropRect.left + thirdWidth * i, cropRect.top),
                    end = Offset(cropRect.left + thirdWidth * i, cropRect.bottom),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(cropRect.left, cropRect.top + thirdHeight * i),
                    end = Offset(cropRect.right, cropRect.top + thirdHeight * i),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // L-Shaped Thick Corner Handles
            val handleCap = StrokeCap.Square

            // Top-Left
            drawLine(Color.White, Offset(cropRect.left - 1, cropRect.top), Offset(cropRect.left + handleLengthPx, cropRect.top), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(cropRect.left, cropRect.top - 1), Offset(cropRect.left, cropRect.top + handleLengthPx), handleThicknessPx, handleCap)

            // Top-Right
            drawLine(Color.White, Offset(cropRect.right + 1, cropRect.top), Offset(cropRect.right - handleLengthPx, cropRect.top), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(cropRect.right, cropRect.top - 1), Offset(cropRect.right, cropRect.top + handleLengthPx), handleThicknessPx, handleCap)

            // Bottom-Left
            drawLine(Color.White, Offset(cropRect.left - 1, cropRect.bottom), Offset(cropRect.left + handleLengthPx, cropRect.bottom), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(cropRect.left, cropRect.bottom + 1), Offset(cropRect.left, cropRect.bottom - handleLengthPx), handleThicknessPx, handleCap)

            // Bottom-Right
            drawLine(Color.White, Offset(cropRect.right + 1, cropRect.bottom), Offset(cropRect.right - handleLengthPx, cropRect.bottom), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(cropRect.right, cropRect.bottom + 1), Offset(cropRect.right, cropRect.bottom - handleLengthPx), handleThicknessPx, handleCap)

            // Mid-Edge Handle Indicators
            val midX = cropRect.left + cropRect.width / 2f
            val midY = cropRect.top + cropRect.height / 2f

            drawLine(Color.White, Offset(midX - handleLengthPx / 2, cropRect.top), Offset(midX + handleLengthPx / 2, cropRect.top), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(midX - handleLengthPx / 2, cropRect.bottom), Offset(midX + handleLengthPx / 2, cropRect.bottom), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(cropRect.left, midY - handleLengthPx / 2), Offset(cropRect.left, midY + handleLengthPx / 2), handleThicknessPx, handleCap)
            drawLine(Color.White, Offset(cropRect.right, midY - handleLengthPx / 2), Offset(cropRect.right, midY + handleLengthPx / 2), handleThicknessPx, handleCap)
        }
    }
}
