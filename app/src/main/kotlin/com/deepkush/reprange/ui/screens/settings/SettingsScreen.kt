package com.deepkush.reprange.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepkush.reprange.constants.BackgroundStyle
import com.deepkush.reprange.constants.DarkMode
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.ui.component.ActionPromptDialog
import com.deepkush.reprange.ui.component.EnumDialog
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.ui.component.ModernSwitch
import com.deepkush.reprange.ui.component.SettingsGroup
import com.deepkush.reprange.ui.component.SettingsGroupSpec
import com.deepkush.reprange.ui.component.SettingsItemSpec
import com.deepkush.reprange.ui.theme.DefaultThemeColor
import com.deepkush.reprange.ui.theme.PresetThemeColors
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.rememberEnumPreference
import com.deepkush.reprange.utils.rememberPreference

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val view = LocalView.current
    val context = LocalContext.current

    // §5.2 - hoist ALL preferences at the top of the screen.
    val (darkMode, onDarkMode) = rememberEnumPreference(PreferenceKeys.DARK_MODE, DarkMode.AUTO)
    val (pureBlack, onPureBlack) = rememberPreference(PreferenceKeys.PURE_BLACK, false)
    val (dynamicFromContent, onDynamicFromContent) = rememberPreference(PreferenceKeys.DYNAMIC_FROM_CONTENT, true)
    val (backgroundStyle, onBackgroundStyle) = rememberEnumPreference(PreferenceKeys.BACKGROUND_STYLE, BackgroundStyle.GRADIENT)
    val (weightUnit, onWeightUnit) = rememberEnumPreference(PreferenceKeys.WEIGHT_UNIT, WeightUnit.KG)
    val (defaultRest, onDefaultRest) = rememberPreference(PreferenceKeys.DEFAULT_REST_SECONDS, 90)
    val (autoStartRest, onAutoStartRest) = rememberPreference(PreferenceKeys.AUTO_START_REST, true)
    val (showGifs, onShowGifs) = rememberPreference(PreferenceKeys.SHOW_GIFS, true)
    val (hapticsEnabled, onHapticsEnabled) = rememberPreference(PreferenceKeys.HAPTICS_ENABLED, true)

    var showDarkModeDialog by rememberSaveable { mutableStateOf(false) }
    var showBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showUnitDialog by rememberSaveable { mutableStateOf(false) }
    var showPaletteDialog by rememberSaveable { mutableStateOf(false) }
    var showRestDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
        Text(
            "Settings",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 16.dp),
        )

        val groups = listOf(
            SettingsGroupSpec(
                title = "Appearance",
                items = listOf(
                    SettingsItemSpec(
                        title = "Accent color",
                        description = "Seed for the generated palette",
                        icon = Icons.Filled.Palette,
                        onClick = {
                            AppHaptics.tap(view)
                            showPaletteDialog = true
                        },
                    ),
                    SettingsItemSpec(
                        title = "Dark mode",
                        description = darkMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Filled.Contrast,
                        onClick = {
                            AppHaptics.tap(view)
                            showDarkModeDialog = true
                        },
                    ),
                    SettingsItemSpec(
                        title = "Pure black",
                        description = "AMOLED-friendly surfaces in dark mode",
                        icon = null,
                        trailingContent = { ModernSwitch(pureBlack) { v -> if (v) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view); onPureBlack(v) } },
                        onClick = {
                            if (!pureBlack) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view)
                            onPureBlack(!pureBlack)
                        },
                    ),
                    SettingsItemSpec(
                        title = "Dynamic from exercise art",
                        description = "Re-seed palette from hero images",
                        icon = Icons.Filled.Bolt,
                        trailingContent = { ModernSwitch(dynamicFromContent) { v -> if (v) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view); onDynamicFromContent(v) } },
                        onClick = {
                            if (!dynamicFromContent) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view)
                            onDynamicFromContent(!dynamicFromContent)
                        },
                    ),
                    SettingsItemSpec(
                        title = "Hero background style",
                        description = backgroundStyle.label(),
                        icon = Icons.Filled.Contrast,
                        enabled = true,
                        onClick = {
                            AppHaptics.tap(view)
                            showBackgroundDialog = true
                        },
                    ),
                ),
            ),
            SettingsGroupSpec(
                title = "Workout",
                items = listOf(
                    SettingsItemSpec(
                        title = "Weight unit",
                        description = weightUnit.symbol,
                        icon = Icons.Filled.FitnessCenter,
                        onClick = {
                            AppHaptics.tap(view)
                            showUnitDialog = true
                        },
                    ),
                    SettingsItemSpec(
                        title = "Default rest time",
                        description = "${defaultRest}s after completing a set",
                        icon = Icons.Filled.Timer,
                        onClick = {
                            AppHaptics.tap(view)
                            showRestDialog = true
                        },
                    ),
                    SettingsItemSpec(
                        title = "Auto-start rest timer",
                        description = "Triggered when a set is logged",
                        trailingContent = { ModernSwitch(autoStartRest) { v -> if (v) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view); onAutoStartRest(v) } },
                        onClick = {
                            if (!autoStartRest) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view)
                            onAutoStartRest(!autoStartRest)
                        },
                    ),
                    SettingsItemSpec(
                        title = "Play animation GIFs",
                        description = "Exercise animations in detail view",
                        trailingContent = { ModernSwitch(showGifs) { v -> if (v) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view); onShowGifs(v) } },
                        onClick = {
                            if (!showGifs) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view)
                            onShowGifs(!showGifs)
                        },
                    ),
                ),
            ),
            SettingsGroupSpec(
                title = "Feedback",
                items = listOf(
                    SettingsItemSpec(
                        title = "Haptic feedback",
                        description = "Rich vibration patterns",
                        trailingContent = { ModernSwitch(hapticsEnabled) { v -> if (v) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view); onHapticsEnabled(v) } },
                        onClick = {
                            if (!hapticsEnabled) AppHaptics.toggleOn(view) else AppHaptics.toggleOff(view)
                            onHapticsEnabled(!hapticsEnabled)
                        },
                    ),
                ),
            ),
            SettingsGroupSpec(
                title = "About",
                items = listOf(
                    SettingsItemSpec(
                        title = "Exercise dataset",
                        description = "1,324 exercises · github.com/hasaneyldrm",
                        icon = Icons.Filled.Info,
                        isExternalLink = true,
                        onClick = {
                            AppHaptics.tap(view)
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hasaneyldrm/exercises-dataset")))
                            }
                        },
                    ),
                    SettingsItemSpec(
                        title = "Media attribution",
                        description = "Exercise media © Gym visual",
                        icon = Icons.Filled.Info,
                        onClick = {
                            AppHaptics.tap(view)
                            showAboutDialog = true
                        },
                    ),
                ),
            ),
        )

        groups.forEach { group ->
            SettingsGroup(group)
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(50.dp))
    }

    if (showDarkModeDialog) {
        EnumDialog(
            title = "Dark mode",
            selected = darkMode,
            options = DarkMode.entries.toList(),
            labelOf = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            onSelect = { onDarkMode(it) },
            onDismiss = { showDarkModeDialog = false },
        )
    }
    if (showBackgroundDialog) {
        EnumDialog(
            title = "Hero background style",
            selected = backgroundStyle,
            options = BackgroundStyle.supported,
            labelOf = { it.label() },
            onSelect = { onBackgroundStyle(it) },
            onDismiss = { showBackgroundDialog = false },
        )
    }
    if (showUnitDialog) {
        EnumDialog(
            title = "Weight unit",
            selected = weightUnit,
            options = WeightUnit.entries.toList(),
            labelOf = { "${it.label} (${it.symbol})" },
            onSelect = { onWeightUnit(it) },
            onDismiss = { showUnitDialog = false },
        )
    }
    if (showPaletteDialog) {
        PaletteDialog(onDismiss = { showPaletteDialog = false })
    }
    if (showRestDialog) {
        RestSliderDialog(initialSeconds = defaultRest, onConfirm = { onDefaultRest(it) }, onDismiss = { showRestDialog = false })
    }
    if (showAboutDialog) {
        ActionPromptDialog(
            title = "Exercise media",
            message = "Thumbnails and animation GIFs are © Gym visual (gymvisual.com), provided via the hasaneyldrm/exercises-dataset repository under its media terms.",
            confirmLabel = "Close",
            dismissLabel = "",
            onDismiss = { showAboutDialog = false },
        )
    }
}

private fun BackgroundStyle.label(): String = when (this) {
    BackgroundStyle.THEMED -> "Themed"
    BackgroundStyle.GRADIENT -> "Gradient"
    BackgroundStyle.BLUR -> "Blur"
    BackgroundStyle.GLOW_ANIMATED -> "Animated glow"
    BackgroundStyle.HERO_LAYERED -> "Layered hero"
    BackgroundStyle.LIVE_MESH -> "Live mesh"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteDialog(onDismiss: () -> Unit) {
    val themeController = com.deepkush.reprange.ui.theme.LocalThemeSeedController.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.AlertDialogDefaults.shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Accent color", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The entire palette is generated from this seed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Swatch(color = DefaultThemeColor, selected = themeController.value == DefaultThemeColor, label = "System", onSelect = { themeController.value = DefaultThemeColor })
                    PresetThemeColors.forEach { preset ->
                        Swatch(
                            color = preset,
                            selected = themeController.value == preset && preset != DefaultThemeColor,
                            label = null,
                            onSelect = { themeController.value = preset },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, label: String?, onSelect: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(if (label != null) 64.dp else 40.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onSelect),
    ) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

@Composable
private fun RestSliderDialog(initialSeconds: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(initialSeconds.toFloat()) }
    ActionPromptDialog(
        title = "Default rest time",
        confirmLabel = "Save",
        onConfirm = { onConfirm(value.toInt()) },
        onDismiss = onDismiss,
    ) {
        Text("${value.toInt()}s", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = value,
            onValueChange = { value = it },
            valueRange = 30f..300f,
            steps = ((300 - 30) / 15) - 1,
        )
    }
}
