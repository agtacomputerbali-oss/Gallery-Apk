package com.gallery.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.gridPinchToZoomGesture(
    currentColumns: Int,
    maxColumns: Int,
    minColumns: Int = 3,
    haptic: HapticFeedback? = null,
    onColumnChange: (Int) -> Unit
): Modifier = this.pointerInput(currentColumns, maxColumns) {
    var accumulatedScale = 1.0f
    detectTransformGestures(panZoomLock = true) { _, _, zoom, _ ->
        accumulatedScale *= zoom
        if (accumulatedScale > 1.25f && currentColumns > minColumns) {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            onColumnChange(currentColumns - 1)
            accumulatedScale = 1.0f
        } else if (accumulatedScale < 0.8f && currentColumns < maxColumns) {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            onColumnChange(currentColumns + 1)
            accumulatedScale = 1.0f
        }
    }
}

fun Modifier.gridDragToSelectGesture(
    gridState: LazyGridState,
    haptic: HapticFeedback? = null,
    onItemHit: (Int) -> Unit
): Modifier = this.pointerInput(gridState) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            findHitItemIndex(gridState, offset)?.let { index ->
                haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                onItemHit(index)
            }
        },
        onDrag = { change, _ ->
            change.consume()
            findHitItemIndex(gridState, change.position)?.let { index ->
                onItemHit(index)
            }
        }
    )
}

private fun findHitItemIndex(gridState: LazyGridState, touchOffset: Offset): Int? {
    return gridState.layoutInfo.visibleItemsInfo
        .firstOrNull { visibleItem ->
            touchOffset.x >= visibleItem.offset.x &&
            touchOffset.x <= visibleItem.offset.x + visibleItem.size.width &&
            touchOffset.y >= visibleItem.offset.y &&
            touchOffset.y <= visibleItem.offset.y + visibleItem.size.height
        }?.index
}
