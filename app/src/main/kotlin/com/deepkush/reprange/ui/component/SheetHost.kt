package com.deepkush.reprange.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Root-hosted sheet system per §6.1: sheets are mounted ONCE at the activity root,
 * screens only call `show { }`. Content is cached during dismissal so the sheet never
 * flashes empty while animating out.
 */
@Stable
class SheetState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(c: @Composable ColumnScope.() -> Unit) {
        content = c
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
    }
}

val LocalMenuState = compositionLocalOf { SheetState() }

/** Top-rounded squircle for sheets. */
fun sheetShape(radius: Dp = 28.dp): Shape =
    AbsoluteSmoothCornerShape(radius, 60, radius, 60, 0.dp, 0, 0.dp, 0)

/**
 * Host rendered once at the root. Uses M3's spring-driven ModalBottomSheet.
 * Programmatic dismissal runs the hide animation first, then unmounts;
 * gesture dismissal unmounts directly (M3 has already settled to Hidden).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(state: SheetState, modifier: Modifier = Modifier) {
    var lastContent by remember {
        mutableStateOf<(@Composable ColumnScope.() -> Unit)>(state.content)
    }
    var mounted by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(state.content) {
        if (state.isVisible) lastContent = state.content
    }

    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            mounted = true
        } else if (mounted) {
            runCatching { sheetState.hide() }
            mounted = false
        }
    }

    if (mounted) {
        ModalBottomSheet(
            onDismissRequest = { state.dismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = sheetShape(),
            dragHandle = null,
            modifier = modifier,
        ) {
            lastContent()
        }
    }
}
