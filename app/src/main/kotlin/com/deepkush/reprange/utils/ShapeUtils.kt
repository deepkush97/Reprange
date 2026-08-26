package com.deepkush.reprange.utils

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Fused group shape utility per ANDROID_M3_EXPRESSIVE_STANDARDS §4.1:
 * first item rounds top corners, last rounds bottom, middle items square.
 */
fun listItemShape(index: Int, count: Int, radius: Dp = Dp(24f)): Shape {
    val s = 60 // superellipse smoothness %
    return when {
        count == 1 -> AbsoluteSmoothCornerShape(radius, s, radius, s, radius, s, radius, s)
        index == 0 -> AbsoluteSmoothCornerShape(radius, s, radius, s, Dp(0f), 0, Dp(0f), 0)
        index == count - 1 -> AbsoluteSmoothCornerShape(Dp(0f), 0, Dp(0f), 0, radius, s, radius, s)
        else -> RectangleShape
    }
}
