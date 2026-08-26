package com.deepkush.reprange.navigation

import kotlinx.serialization.Serializable

/** Tab destinations - order drives directional transitions (§8.1). */
val TabOrder = listOf(
    HomeRoute::class.simpleName ?: "HomeRoute",
    LibraryRoute::class.simpleName ?: "LibraryRoute",
    ProgressRoute::class.simpleName ?: "ProgressRoute",
    SettingsRoute::class.simpleName ?: "SettingsRoute",
)

fun routeIndex(routeName: String?): Int =
    TabOrder.indexOfFirst { routeName?.contains(it ?: "") == true }.takeIf { it >= 0 } ?: -1

@Serializable
data object HomeRoute

@Serializable
data object LibraryRoute

@Serializable
data object ProgressRoute

@Serializable
data object SettingsRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

@Serializable
data class ActiveWorkoutRoute(val sessionId: Long)

@Serializable
data class TemplateBuilderRoute(val templateId: Long? = null)

/** Selection modes: "template" adds to builder draft, "session" adds to live session. */
@Serializable
data class ExercisePickerRoute(val mode: String, val contextId: Long = 0L)
