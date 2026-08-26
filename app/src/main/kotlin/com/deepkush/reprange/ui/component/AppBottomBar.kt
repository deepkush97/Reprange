package com.deepkush.reprange.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.listItemShape

enum class Tab(
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    LIBRARY("Library", Icons.Filled.TravelExplore, Icons.Outlined.TravelExplore),
    PROGRESS("Progress", Icons.Filled.Insights, Icons.Outlined.Insights),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

/**
 * Expressive bottom navigation per §8.1: pill background tween(200), selection scale
 * spring(MediumBouncy) -> 1.05f, label reveal expandHorizontally spring(NoBouncy/MediumLow).
 */
@Composable
fun AppBottomBar(
    currentRouteName: String?,
    onTabSelected: (Any) -> Unit,
    modifier: Modifier = Modifier,
    onHeightMeasured: (Int) -> Unit = {},
) {
    val selectedIndex = tabIndexOf(currentRouteName)
    val view = LocalView.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .onSizeChanged { onHeightMeasured(it.height) },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = listItemShape(0, 1, radius = 28.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Tab.entries.forEachIndexed { index, tab ->
                BottomTabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (index != selectedIndex) {
                            AppHaptics.segmentTick(view)
                            onTabSelected(tabDestination(tab))
                        }
                    },
                )
            }
        }
    }
}

private fun tabIndexOf(routeName: String?): Int =
    when {
        routeName == null -> -1
        routeName.contains("HomeRoute") -> 0
        routeName.contains("LibraryRoute") -> 1
        routeName.contains("ProgressRoute") -> 2
        routeName.contains("SettingsRoute") -> 3
        else -> -1
    }

private fun tabDestination(tab: Tab): Any = when (tab) {
    Tab.HOME -> com.deepkush.reprange.navigation.HomeRoute
    Tab.LIBRARY -> com.deepkush.reprange.navigation.LibraryRoute
    Tab.PROGRESS -> com.deepkush.reprange.navigation.ProgressRoute
    Tab.SETTINGS -> com.deepkush.reprange.navigation.SettingsRoute
}

@Composable
private fun BottomTabItem(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else Color.Transparent,
        animationSpec = tween(200),
        label = "navPill",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "navScale",
    )
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentWidth(align = Alignment.CenterHorizontally, unbounded = true)
                .background(pillColor, CircleShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale),
            )
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                ),
                exit = shrinkHorizontally(),
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
