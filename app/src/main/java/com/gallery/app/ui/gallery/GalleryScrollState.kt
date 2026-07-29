package com.gallery.app.ui.gallery

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

@Composable
fun rememberFloatingDockVisibility(lazyGridState: LazyGridState): State<Boolean> {
    val isVisible = remember(lazyGridState) { mutableStateOf(true) }

    LaunchedEffect(lazyGridState) {
        var previousIndex = lazyGridState.firstVisibleItemIndex
        var previousScrollOffset = lazyGridState.firstVisibleItemScrollOffset

        snapshotFlow {
            Pair(lazyGridState.firstVisibleItemIndex, lazyGridState.firstVisibleItemScrollOffset)
        }.collect { (currentIndex, currentScrollOffset) ->
            if (currentIndex > previousIndex) {
                isVisible.value = false
            } else if (currentIndex < previousIndex) {
                isVisible.value = true
            } else {
                if (currentScrollOffset > previousScrollOffset + 20) {
                    isVisible.value = false
                } else if (currentScrollOffset < previousScrollOffset - 20) {
                    isVisible.value = true
                }
            }
            previousIndex = currentIndex
            previousScrollOffset = currentScrollOffset
        }
    }

    return isVisible
}
