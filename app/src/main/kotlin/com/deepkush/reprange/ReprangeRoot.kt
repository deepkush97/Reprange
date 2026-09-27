package com.deepkush.reprange

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import com.deepkush.reprange.constants.DarkMode
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.navigation.ActiveWorkoutRoute
import com.deepkush.reprange.navigation.ExerciseDetailRoute
import com.deepkush.reprange.navigation.ExercisePickerRoute
import com.deepkush.reprange.navigation.HomeRoute
import com.deepkush.reprange.navigation.LibraryRoute
import com.deepkush.reprange.navigation.OnboardingRoute
import com.deepkush.reprange.navigation.ProgressRoute
import com.deepkush.reprange.navigation.SettingsRoute
import com.deepkush.reprange.navigation.TemplateBuilderRoute
import com.deepkush.reprange.navigation.routeIndex
import com.deepkush.reprange.ui.component.AppBottomBar
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.ui.component.SheetHost
import com.deepkush.reprange.ui.component.SheetState
import com.deepkush.reprange.ui.component.SessionDock
import com.deepkush.reprange.ui.screens.exercise.ExerciseDetailScreen
import com.deepkush.reprange.ui.screens.home.HomeScreen
import com.deepkush.reprange.ui.screens.library.LibraryScreen
import com.deepkush.reprange.ui.screens.onboarding.OnboardingScreen
import com.deepkush.reprange.ui.screens.library.ExercisePickerScreen
import com.deepkush.reprange.ui.screens.progress.ProgressScreen
import com.deepkush.reprange.ui.screens.settings.SettingsScreen
import com.deepkush.reprange.ui.screens.template.TemplateBuilderScreen
import com.deepkush.reprange.ui.screens.workout.ActiveWorkoutScreen
import com.deepkush.reprange.ui.theme.AppTheme
import com.deepkush.reprange.ui.theme.ColorSaver
import com.deepkush.reprange.ui.theme.DefaultThemeColor
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.rememberEnumPreference
import com.deepkush.reprange.utils.rememberPreference

@Composable
fun ReprangeRoot() {
    val (darkModePref, _) = rememberEnumPreference(PreferenceKeys.DARK_MODE, DarkMode.AUTO)
    val (pureBlack, _) = rememberPreference(PreferenceKeys.PURE_BLACK, false)
    val (selectedColorInt, _) = rememberPreference(PreferenceKeys.SELECTED_COLOR, DefaultThemeColor.toArgb())
    val hapticsEnabled by rememberPreference(PreferenceKeys.HAPTICS_ENABLED, true)

    val darkTheme = when (darkModePref) {
        DarkMode.AUTO -> isSystemInDarkTheme()
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    val themeColor = rememberSaveable(stateSaver = ColorSaver) {
        mutableStateOf(Color(selectedColorInt))
    }

    LaunchedEffect(hapticsEnabled) {
        AppHaptics.setEnabled(hapticsEnabled)
    }

    // Status-bar icon appearance follows the APP theme, not the system setting (§9.1).
    val rootView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(darkTheme) {
        var ctx: android.content.Context? = rootView.context
        while (ctx != null && ctx !is android.app.Activity) {
            ctx = (ctx as? android.content.ContextWrapper)?.baseContext
        }
        val activity = ctx as? android.app.Activity
        val window = activity?.window
        val controller = window?.decorView?.let { v ->
            androidx.core.view.WindowInsetsControllerCompat(window, v)
        }
        controller?.isAppearanceLightStatusBars = !darkTheme
        onDispose { }
    }

    AppTheme(
        darkTheme = darkTheme,
        pureBlack = pureBlack,
        themeColor = themeColor.value,
    ) {
        RootComposition(themeColor = themeColor)
    }
}

@Composable
private fun RootComposition(themeColor: androidx.compose.runtime.MutableState<Color>) {
    val navController = rememberNavController()
    val menuState = remember { SheetState() }
    val density = LocalDensity.current

    // Paint the generated scheme behind EVERYTHING and keep the Activity window
    // background in sync, so in-app Dark ON/OFF prefs render correctly even when
    // they diverge from the system setting (the XML theme only covers system-follow).
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val rootView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(scheme.background, scheme.surface) {
        var ctx: android.content.Context? = rootView.context
        while (ctx != null && ctx !is Activity) {
            ctx = (ctx as? ContextWrapper)?.baseContext
        }
        (ctx as? Activity)?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(scheme.background.toArgb()),
        )
        onDispose { }
    }

    var bottomBarVisible by remember { mutableStateOf(true) }
    var dockHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = backStackEntry?.destination?.route
    val onTabRoute = routeIndex(currentRouteName) >= 0

    LaunchedEffect(currentRouteName) {
        bottomBarVisible = onTabRoute
    }

    // §9.1 - ONE insets source of truth: system safe drawing merged with app chrome
    // (bottom navigation bar + active-session dock) so every screen pads correctly.
    val safeDrawing = WindowInsets.safeDrawing
    val chromeBottomPx = dockHeightPx + if (bottomBarVisible) bottomBarHeightPx else 0
    val chromeBottomDp = with(density) { chromeBottomPx.toDp() }
    val merged = remember(safeDrawing, chromeBottomDp) {
        safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            .add(
                safeDrawing.only(WindowInsetsSides.Bottom)
                    .union(WindowInsets(bottom = chromeBottomDp)),
            )
    }

    CompositionLocalProvider(
        LocalAppWindowInsets provides merged,
        com.deepkush.reprange.ui.theme.LocalThemeSeedController provides themeColor,
    ) {
        // Surface (not bare Box): paints scheme.background AND provides
        // LocalContentColor = onBackground, so default-colored Text is readable
        // in both modes (bare Box leaves LocalContentColor = Black).
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = scheme.background,
        ) {
            Box(Modifier.fillMaxSize()) {
                AppNavHost(navController = navController)

            if (bottomBarVisible) {
                AppBottomBar(
                    currentRouteName = currentRouteName,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                    onTabSelected = { tab ->
                        navController.navigate(tab) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },                    onHeightMeasured = { bottomBarHeightPx = it },
                )
            }

            val hideChrome = currentRouteName?.contains("OnboardingRoute") == true || currentRouteName?.contains("ActiveWorkout") == true
            val hideDockOnWorkout = hideChrome
            if (!hideDockOnWorkout) {
                SessionDock(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(
                            bottom = with(density) {
                                (if (bottomBarVisible) bottomBarHeightPx else 0).toDp()
                            },
                        ),
                    onOpenWorkout = { navController.navigate(ActiveWorkoutRoute(it)) { launchSingleTop = true } },
                    onHeightMeasured = { dockHeightPx = it },
                )
            } else {
                // Zero dock height when hidden so insets don't reserve space.
                LaunchedEffect(Unit) { dockHeightPx = 0 }
            }

            SheetHost(state = menuState)
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    val tabNames = listOf("HomeRoute", "LibraryRoute", "ProgressRoute", "SettingsRoute")

    fun tabIndex(routeName: String?): Int =
        tabNames.indexOfFirst { routeName?.contains(it) == true }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = {
            val cur = tabIndex(targetState.destination.route)
            val prev = tabIndex(initialState.destination.route)
            when {
                cur == -1 || prev == -1 || cur > prev ->
                    slideInHorizontally { it / 8 } + fadeIn(tween(200))

                else -> slideInHorizontally { -it / 8 } + fadeIn(tween(200))
            }
        },
        exitTransition = {
            val cur = tabIndex(targetState.destination.route)
            val prev = tabIndex(initialState.destination.route)
            when {
                cur == -1 || prev == -1 || cur > prev ->
                    slideOutHorizontally { it / 8 } + fadeOut(tween(200))

                else -> slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
            }
        },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
        },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onStartTemplate = { id -> navController.navigate(ActiveWorkoutRoute(id)) },
                onStartBlank = { id -> navController.navigate(ActiveWorkoutRoute(id)) },
                onEditTemplate = { id -> navController.navigate(TemplateBuilderRoute(id)) },
                onCreateTemplate = { navController.navigate(TemplateBuilderRoute(null)) },
                onOpenExercise = { id -> navController.navigate(ExerciseDetailRoute(id)) },
                onNavigateToOnboarding = { navController.navigate(OnboardingRoute) },
            )
        }
        composable<LibraryRoute> {
            LibraryScreen(onOpenExercise = { id -> navController.navigate(ExerciseDetailRoute(id)) })
        }
        composable<ProgressRoute> {
            ProgressScreen(onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToOnboarding = { navController.navigate(OnboardingRoute) },
            )
        }
        composable<OnboardingRoute> { entry ->
            val onboardingVm: com.deepkush.reprange.ui.screens.onboarding.OnboardingViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(entry)
            val picked by entry.savedStateHandle
                .getStateFlow("picked_exercise", "")
                .collectAsStateWithLifecycle()
            LaunchedEffect(picked) {
                if (picked.isNotBlank()) {
                    val tIdx = entry.savedStateHandle.get<Int>("swap_template_idx") ?: -1
                    val iIdx = entry.savedStateHandle.get<Int>("swap_item_idx") ?: -1
                    if (tIdx >= 0 && iIdx >= 0) {
                        onboardingVm.swapExercise(tIdx, iIdx, picked)
                    }
                    entry.savedStateHandle["picked_exercise"] = ""
                    entry.savedStateHandle["swap_template_idx"] = -1
                    entry.savedStateHandle["swap_item_idx"] = -1
                }
            }
            OnboardingScreen(
                onFinish = { navController.popBackStack() },
                onPickExercise = { tIdx, iIdx, bucket ->
                    entry.savedStateHandle["swap_template_idx"] = tIdx
                    entry.savedStateHandle["swap_item_idx"] = iIdx
                    navController.navigate(ExercisePickerRoute("template", 0L, bucket))
                },
                onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) },
                viewModel = onboardingVm,
            )
        }
        composable<ExerciseDetailRoute> { entry ->
            val route = entry.toRoute<ExerciseDetailRoute>()
            ExerciseDetailScreen(
                exerciseId = route.exerciseId,
                onBack = { navController.popBackStack() },
                onStartWorkout = { id ->
                    navController.navigate(ActiveWorkoutRoute(id)) { launchSingleTop = true }
                },
            )
        }
        composable<ActiveWorkoutRoute> { entry ->
            val sessionVm: com.deepkush.reprange.workout.ActiveSessionViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(entry)
            val picked by entry.savedStateHandle
                .getStateFlow("picked_exercise", "")
                .collectAsStateWithLifecycle()
            LaunchedEffect(picked) {
                if (picked.isNotBlank()) {
                    sessionVm.addToActiveSession(picked)
                    entry.savedStateHandle["picked_exercise"] = ""
                }
            }
            ActiveWorkoutScreen(
                onFinish = { navController.popBackStack() },
                onPickExercise = { navController.navigate(ExercisePickerRoute("session")) },
                onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) },
            )
        }
        composable<TemplateBuilderRoute> { entry ->
            val route = entry.toRoute<TemplateBuilderRoute>()
            val builderVm: com.deepkush.reprange.viewmodels.TemplateBuilderViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(entry)
            val picked by entry.savedStateHandle
                .getStateFlow("picked_exercise", "")
                .collectAsStateWithLifecycle()
            LaunchedEffect(picked) {
                if (picked.isNotBlank()) {
                    builderVm.addExercise(picked)
                    entry.savedStateHandle["picked_exercise"] = ""
                }
            }
            TemplateBuilderScreen(
                templateId = route.templateId,
                onDone = { navController.popBackStack() },
                onPickExercise = { navController.navigate(ExercisePickerRoute("template")) },
                onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) },
                viewModel = builderVm,
            )
        }
        composable<ExercisePickerRoute> { entry ->
            val route = entry.toRoute<ExercisePickerRoute>()
            ExercisePickerScreen(
                mode = route.mode,
                onSelect = { exerciseId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("picked_exercise", exerciseId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                initialCategory = route.initialCategory,
            )
        }
    }
}
