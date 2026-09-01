package com.deepkush.reprange.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class DarkMode { AUTO, ON, OFF }

enum class WeightUnit(val label: String, val symbol: String) {
    KG("Kilograms", "kg"),
    LB("Pounds", "lb");

    fun fromKg(kg: Double): Double = if (this == KG) kg else kg * 2.2046226218
    fun toKg(value: Double): Double = if (this == KG) value else value / 2.2046226218
}

enum class BackgroundStyle {
    THEMED,
    GRADIENT,
    BLUR,
    GLOW_ANIMATED,
    HERO_LAYERED,
    LIVE_MESH;

    companion object {
        val supported: List<BackgroundStyle>
            get() = if (android.os.Build.VERSION.SDK_INT >= 31) entries else entries.filter { it != BLUR }
    }
}

object PreferenceKeys {
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val PURE_BLACK = booleanPreferencesKey("pure_black")
    val SELECTED_COLOR = intPreferencesKey("selected_color")
    val DYNAMIC_FROM_CONTENT = booleanPreferencesKey("dynamic_from_content")
    val BACKGROUND_STYLE = stringPreferencesKey("background_style")
    val FONT_FAMILY = stringPreferencesKey("font_family")

    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")

    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    val DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
    val AUTO_START_REST = booleanPreferencesKey("auto_start_rest")
    val SHOW_GIFS = booleanPreferencesKey("show_gifs")

    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val ONBOARDING_GOAL = stringPreferencesKey("onboarding_goal")
    val ONBOARDING_EXPERIENCE = stringPreferencesKey("onboarding_experience")
    val ONBOARDING_EQUIPMENT = stringPreferencesKey("onboarding_equipment")
    val ONBOARDING_DAYS = intPreferencesKey("onboarding_days")
    val ONBOARDING_SPLIT = stringPreferencesKey("onboarding_split")
}
