package com.deepkush.reprange.ui.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * Activity-scoped seed controller for content-derived live seeding (§2.2).
 * Hero surfaces write their extracted color here when "dynamic from content" is enabled.
 */
val LocalThemeSeedController = compositionLocalOf<MutableState<Color>> {
    mutableStateOf(DefaultThemeColor)
}
