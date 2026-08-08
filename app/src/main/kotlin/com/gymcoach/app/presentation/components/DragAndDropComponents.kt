package com.gymcoach.app.presentation.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

data class DragAndDropState(
    val draggedItemIndex: Int,
    val dragOffsetY: Float,
    val isDragging: Boolean,
    val onDragStart: (Int) -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit
)

@Composable
fun rememberDragAndDropState(lazyListState: LazyListState): DragAndDropState {
    var draggedItemIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val isDragging = draggedItemIndex >= 0

    return remember {
        DragAndDropState(
            draggedItemIndex = draggedItemIndex,
            dragOffsetY = dragOffsetY,
            isDragging = isDragging,
            onDragStart = { index -> draggedItemIndex = index },
            onDrag = { delta -> dragOffsetY += delta },
            onDragEnd = { draggedItemIndex = -1; dragOffsetY = 0f }
        )
    }
}

@Composable
fun DraggableItem(
    state: DragAndDropState,
    index: Int,
    content: @Composable () -> Unit
) {
    val isDragged = state.isDragging && state.draggedItemIndex == index
    val modifier = if (isDragged) {
        Modifier
            .offset { IntOffset(0, state.dragOffsetY.toInt()) }
            .shadow(8.dp)
            .alpha(0.9f)
    } else {
        Modifier
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { state.onDragStart(index) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount.y)
                },
                onDragEnd = { state.onDragEnd() },
                onDragCancel = { state.onDragEnd() }
            )
        }
    ) {
        content()
    }
}
