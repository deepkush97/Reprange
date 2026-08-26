package com.deepkush.reprange.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.WindowInsets

/**
 * The ONE insets source of truth per §9.1: system bars merged with app-owned chrome
 * (bottom nav bar, active-session dock). Provided once at the Activity root;
 * screens consume `.only(WindowInsetsSides.Horizontal + Bottom)` plus manual top spacers.
 */
val LocalAppWindowInsets = staticCompositionLocalOf<WindowInsets> { WindowInsets(0, 0, 0, 0) }
