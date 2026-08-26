package com.deepkush.reprange.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

val DefaultThemeColor = Color(0xFFFF5A36) // Reprange brand ember-orange seed

/** Sentinel meaning "follow wallpaper / system dynamic". Shifted nearest preset by one step. */
val DynamicThemeSentinel = DefaultThemeColor

/** ~19 preset seeds spanning crimson → blue-grey (§2.1). */
val PresetThemeColors = listOf(
    Color(0xFFE53935), Color(0xFFED5564), Color(0xFFEC407A), Color(0xFFAB47BC),
    Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF3F51B5), Color(0xFF1E88E5),
    Color(0xFF039BE5), Color(0xFF00ACC1), Color(0xFF00897B), Color(0xFF43A047),
    Color(0xFF7CB342), Color(0xFF9E9D24), Color(0xFFF4511E), Color(0xFF6D4C41),
    Color(0xFF757575), Color(0xFF546E7A), Color(0xFF37474F),
)

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

@Composable
fun rememberSavedThemeColor(initial: Color = DefaultThemeColor): androidx.compose.runtime.MutableState<Color> =
    rememberSaveable(stateSaver = ColorSaver) {
        androidx.compose.runtime.mutableStateOf(initial)
    }

@Composable
fun AppTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    typography: androidx.compose.material3.Typography = AppTypography,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val useSystemDynamic =
        themeColor == DynamicThemeSentinel && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    val base: ColorScheme = if (useSystemDynamic) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = if (themeColor.toArgb() == 0xFF000000.toInt()) PaletteStyle.Monochrome
            else PaletteStyle.TonalSpot,
        )
    }

    val scheme = androidx.compose.runtime.remember(base, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) base.copy(surface = Color.Black, background = Color.Black)
        else base
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = typography,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
