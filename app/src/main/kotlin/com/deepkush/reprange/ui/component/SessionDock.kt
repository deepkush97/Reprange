package com.deepkush.reprange.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.Formatters
import com.deepkush.reprange.utils.listItemShape
import com.deepkush.reprange.utils.rememberPreference
import com.deepkush.reprange.workout.ActiveSessionViewModel
import kotlinx.coroutines.delay

/**
 * Persistent mini-bar for a live workout - sits above the bottom bar and is merged into
 * LocalAppWindowInsets so screens pad above it (the vivi "player-aware insets" pattern).
 */
@Composable
fun SessionDock(
    modifier: Modifier = Modifier,
    onOpenWorkout: (Long) -> Unit,
    onHeightMeasured: (Int) -> Unit = {},
) {
    val viewModel: ActiveSessionViewModel = hiltViewModel()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val view = LocalView.current

    val visible = session != null
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session?.session?.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    AnimatedVisibility(
        visible = visible && session != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        val active = session ?: return@AnimatedVisibility
        val elapsedSeconds = ((nowMillis - active.session.startedAt) / 1000).coerceAtLeast(0)

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .onSizeChanged { onHeightMeasured(it.height) },
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = listItemShape(0, 1, radius = 20.dp),
            shadowElevation = 6.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        AppHaptics.tap(view)
                        onOpenWorkout(active.session.id)
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Workout in progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "${Formatters.durationSeconds(elapsedSeconds)} · ${active.exercises.sumOf { it.sets.size }} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
