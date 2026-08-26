package com.deepkush.reprange.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.utils.rememberPreference
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.data.db.LoggedSetType
import com.deepkush.reprange.data.db.SessionExerciseWithSets
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.Formatters
import com.deepkush.reprange.utils.listItemShape
import com.deepkush.reprange.workout.ActiveSessionManager
import com.deepkush.reprange.workout.ActiveSessionViewModel

@Composable
fun ActiveWorkoutScreen(
    onFinish: () -> Unit,
    onPickExercise: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val restRemaining by viewModel.restRemainingSeconds.collectAsStateWithLifecycle()
    val prFlash by viewModel.prFlash.collectAsStateWithLifecycle()

    val (unitPref, _) = com.deepkush.reprange.utils.rememberEnumPreference(PreferenceKeys.WEIGHT_UNIT, WeightUnit.KG)
    val (autoRest, _) = rememberPreference(PreferenceKeys.AUTO_START_REST, true)

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session?.session?.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(prFlash) {
        if (prFlash != null) {
            AppHaptics.confirm(view)
            snackbarHostState.showSnackbar("New personal record!")
            viewModel.clearPrFlash()
        }
    }

    val active = session
    if (active == null) {
        // Session finished/discarded elsewhere.
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    var exerciseNames by remember(active.session.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(active.exercises.map { it.entry.exerciseId }.distinct()) {
        exerciseNames = viewModel.namesFor(active.exercises.map { it.entry.exerciseId })
    }

    val elapsedSeconds = ((nowMillis - active.session.startedAt) / 1000).coerceAtLeast(0)

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(44.dp))

            // Top bar: exit, elapsed, rest timer chip.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                IconButton(onClick = { AppHaptics.tap(view); showDiscardDialog = true }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    Formatters.durationSeconds(elapsedSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))

                AnimatedVisibility(visible = restRemaining != null, enter = fadeIn(), exit = fadeOut()) {
                    RestTimerChip(
                        seconds = restRemaining ?: 0,
                        onAddTime = { AppHaptics.tap(view); viewModel.addRestTime(15) },
                        onSkip = { AppHaptics.gestureEnd(view); viewModel.skipRest() },
                    )
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                item {
                    Text(
                        "Workout",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }

                itemsIndexed(active.exercises.sortedBy { it.entry.orderIndex }, key = { _, e -> e.entry.id }) { index, item ->
                    ExerciseEntryCard(
                        item = item,
                        index = index,
                        count = active.exercises.size,
                        unit = unitPref,
                        autoStartRest = autoRest,
                        viewModel = viewModel,
                        displayName = exerciseNames[item.entry.exerciseId] ?: item.entry.exerciseId,
                        onRemoved = { viewModel.removeEntry(item.entry) },
                    )
                }

                item {
                    TextButton(
                        onClick = { AppHaptics.tap(view); onPickExercise() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add exercise")
                    }
                }
                item { Spacer(Modifier.height(110.dp)) }
            }
        }

        // Bottom action bar: totals + finish.
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = listItemShape(0, 1, radius = 24.dp),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Formatters.volume(active.session.totalVolumeKg, unitPref),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${active.exercises.sumOf { it.sets.size }} sets · ${active.exercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { AppHaptics.primaryTap(view); showFinishDialog = true },
                    shape = listItemShape(0, 1, radius = 16.dp),
                ) {
                    Icon(Icons.Filled.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Finish")
                }
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (showDiscardDialog) {
        com.deepkush.reprange.ui.component.ActionPromptDialog(
            title = "End workout?",
            message = "Your progress will be discarded.",
            confirmLabel = "Discard",
            dismissLabel = "Keep going",
            onConfirm = {
                AppHaptics.thud(view)
                viewModel.discard { onFinish() }
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
    if (showFinishDialog) {
        com.deepkush.reprange.ui.component.ActionPromptDialog(
            title = "Finish workout?",
            message = "${Formatters.durationSeconds(elapsedSeconds)} · ${Formatters.volume(active.session.totalVolumeKg, unitPref)} · ${active.exercises.sumOf { it.sets.size }} sets",
            confirmLabel = "Finish",
            onConfirm = {
                AppHaptics.confirm(view)
                viewModel.finish { onFinish() }
            },
            onDismiss = { showFinishDialog = false },
        )
    }
}

@Composable
private fun RestTimerChip(seconds: Int, onAddTime: () -> Unit, onSkip: () -> Unit) {
    Surface(shape = listItemShape(0, 1, radius = 14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                Formatters.restCountdown(seconds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = onAddTime),
            )
            LinearProgressIndicator(
                progress = { (seconds % 60f) / 60f },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            IconButton(onClick = onSkip, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Skip rest", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ExerciseEntryCard(
    item: SessionExerciseWithSets,
    index: Int,
    count: Int,
    unit: WeightUnit,
    autoStartRest: Boolean,
    viewModel: ActiveSessionViewModel,
    displayName: String,
    onRemoved: () -> Unit,
) {
    val view = LocalView.current
    val entry = item.entry
    val sortedSets = item.sets.sortedBy { it.completedAt }

    var suggestion by remember(entry.id) { mutableStateOf<ActiveSessionManager.Suggestion?>(null) }
    LaunchedEffect(entry.id) {
        suggestion = viewModel.suggestionFor(entry.exerciseId)
    }

    var weightText by remember(entry.id) {
        mutableStateOf(suggestion?.let { Formatters.weightShort(it.weightKg, unit) } ?: "")
    }
    var repsText by remember(entry.id) { mutableStateOf(suggestion?.reps?.toString() ?: "") }
    var selectedType by remember(entry.id) { mutableStateOf(LoggedSetType.NORMAL) }
    var showMenu by remember { mutableStateOf(false) }

    val strategy = runCatching { SetStrategy.valueOf(entry.strategy) }.getOrDefault(SetStrategy.STANDARD)

    // Prefill from previous performance once loaded; step-up escalates per completed set.
    LaunchedEffect(sortedSets.size, strategy, suggestion) {
        val base = suggestion ?: return@LaunchedEffect
        val targetWeight = when {
            strategy == SetStrategy.STEP_UP && sortedSets.isNotEmpty() ->
                base.weightKg + 2.5 * sortedSets.size

            else -> base.weightKg
        }
        if (weightText.isBlank()) {
            weightText = Formatters.weightShort(targetWeight, unit)
        }
        if (repsText.isBlank() && sortedSets.isEmpty()) {
            repsText = base.reps.toString()
        }
    }

    Card(
        shape = listItemShape(index, count),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    displayName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (strategy != SetStrategy.STANDARD) {
                    Text(
                        strategy.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            suggestion?.let {
                Text(
                    "Last: ${Formatters.weight(it.weightKg, unit)} × ${it.reps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 36.dp, top = 2.dp),
                )
            }

            sortedSets.forEach { set ->
                CompletedSetRow(set = set, unit = unit, onDelete = {
                    AppHaptics.swipeCommit(view)
                    viewModel.removeSet(set)
                })
            }

            // Logging row: weight / reps / type chips / commit.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(unit.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.width(92.dp),
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                    label = { Text("reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.width(78.dp),
                )

                LoggedSetType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            AppHaptics.segmentTick(view)
                            selectedType = type
                        },
                        label = { Text(type.shortLabel) },
                        modifier = Modifier.width(38.dp),
                    )
                }

                val commit = {
                    val w = unit.toKg(weightText.toDoubleOrNull() ?: 0.0)
                    val r = repsText.toIntOrNull() ?: 0
                    if (w > 0 && r > 0) {
                        AppHaptics.primaryTap(view)
                        viewModel.logSet(entry, selectedType, w, r, entry.restSeconds, autoStartRest)
                        repsText = ""
                        if (selectedType != LoggedSetType.NORMAL) selectedType = LoggedSetType.NORMAL
                    } else {
                        AppHaptics.reject(view)
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = commit),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = "Log set", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            if (showMenu) {
                TextButton(onClick = { AppHaptics.thud(view); onRemoved() }) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Remove exercise", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** One-swipe left deletes a logged set (§ brief frictionless logging). */
@Composable
private fun CompletedSetRow(set: com.deepkush.reprange.data.db.CompletedSetEntity, unit: WeightUnit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 20.dp)
                    .background(Color.Transparent),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.error)
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                val type = runCatching { LoggedSetType.valueOf(set.setType) }.getOrDefault(LoggedSetType.NORMAL)
                Text(
                    type.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (type == LoggedSetType.NORMAL) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(22.dp),
                )
                Text("#${set.setNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp))
                Text(
                    "${Formatters.weightShort(set.weightKg, unit)} ${unit.symbol} × ${set.reps}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("✓", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
