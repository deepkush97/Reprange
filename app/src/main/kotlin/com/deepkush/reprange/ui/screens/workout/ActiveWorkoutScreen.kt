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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.utils.rememberPreference
import com.deepkush.reprange.constants.WeightUnit
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import com.deepkush.reprange.data.db.LoggedSetType
import com.deepkush.reprange.data.db.SessionExerciseWithSets
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.Formatters
import com.deepkush.reprange.utils.listItemShape
import com.deepkush.reprange.workout.ActiveSessionManager
import com.deepkush.reprange.workout.ActiveSessionViewModel

private fun LoggedSetType.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    LoggedSetType.NORMAL -> Icons.Filled.FitnessCenter
    LoggedSetType.WARMUP -> Icons.Filled.LocalFireDepartment
    LoggedSetType.DROP_SET -> Icons.Filled.KeyboardDoubleArrowDown
    LoggedSetType.FAILURE -> Icons.Filled.Bolt
}

@Composable
fun ActiveWorkoutScreen(
    onFinish: () -> Unit,
    onPickExercise: () -> Unit,
    onExerciseClick: (String) -> Unit = {},
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
            Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
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

                val ordered = active.exercises.sortedBy { it.entry.orderIndex }
                val orderedEntries = ordered.map { it.entry }
                itemsIndexed(ordered, key = { _, e -> e.entry.id }) { index, item ->
                    val nextUpEntry = SupersetGrouping.nextUp(orderedEntries, item.entry.id)
                    val nextUpName = nextUpEntry?.let {
                        exerciseNames[it.exerciseId] ?: it.exerciseId
                    }
                    ExerciseEntryCard(
                        item = item,
                        index = index,
                        count = active.exercises.size,
                        unit = unitPref,
                        autoStartRest = autoRest,
                        viewModel = viewModel,
                        displayName = exerciseNames[item.entry.exerciseId] ?: item.entry.exerciseId,
                        onRemoved = { viewModel.removeEntry(item.entry) },
                        onExerciseClick = onExerciseClick,
                        supersetLabel = SupersetGrouping.supersetLabel(item.entry),
                        nextUpName = nextUpName,
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
    onExerciseClick: (String) -> Unit = {},
    supersetLabel: String? = null,
    nextUpName: String? = null,
) {
    val view = LocalView.current
    val entry = item.entry
    val sortedSets = item.sets.sortedBy { it.completedAt }
    val strategy = runCatching { SetStrategy.valueOf(entry.strategy) }.getOrDefault(SetStrategy.STANDARD)

    var suggestion by remember(entry.id) { mutableStateOf<ActiveSessionManager.Suggestion?>(null) }
    LaunchedEffect(entry.id) {
        suggestion = viewModel.suggestionFor(entry.exerciseId)
    }

    var drafts by remember(entry.id) { mutableStateOf<List<DraftRow>>(emptyList()) }
    var editTarget by remember(entry.id) { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(entry.id, suggestion) {
        val planned = entry.plannedSets ?: 0
        if (drafts.isEmpty() && planned == 0 && sortedSets.isEmpty()) {
            val base = suggestion
            drafts = listOf(
                DraftRow(
                    weightKg = base?.weightKg ?: 20.0,
                    reps = base?.reps ?: 10,
                    setType = LoggedSetType.NORMAL,
                ),
            )
        }
        if (drafts.isEmpty() && planned > 0) {
            val base = entry.targetWeightKg?.let { ActiveSessionManager.Suggestion(it, entry.targetReps ?: 10) }
                ?: suggestion
            drafts = (0 until planned).map { i ->
                val w = when {
                    base == null -> 20.0
                    strategy == SetStrategy.STEP_UP -> base.weightKg + 2.5 * i
                    else -> base.weightKg
                }
                DraftRow(
                    weightKg = w,
                    reps = base?.reps ?: 10,
                    setType = LoggedSetType.NORMAL,
                )
            }
        }
    }

    fun addSet() {
        val template = drafts.lastOrNull()
            ?: sortedSets.lastOrNull()?.let {
                DraftRow(it.weightKg, it.reps, runCatching { LoggedSetType.valueOf(it.setType) }.getOrDefault(LoggedSetType.NORMAL))
            }
            ?: suggestion?.let { DraftRow(it.weightKg, it.reps, LoggedSetType.NORMAL) }
            ?: DraftRow(20.0, 10, LoggedSetType.NORMAL)
        AppHaptics.tap(view)
        drafts = drafts + template.copy(setType = LoggedSetType.NORMAL)
    }

    fun logRow(rowIdx: Int) {
        val row = drafts.getOrNull(rowIdx) ?: return
        if (row.weightKg <= 0.0 || row.reps <= 0) {
            AppHaptics.reject(view)
            return
        }
        AppHaptics.primaryTap(view)
        viewModel.logSet(entry, row.setType, row.weightKg, row.reps, entry.restSeconds, autoStartRest)
        drafts = drafts.filterIndexed { i, _ -> i != rowIdx }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            AppHaptics.tap(view)
                            onExerciseClick(entry.exerciseId)
                        },
                ) {
                    Text(
                        displayName.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                }
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
                if (supersetLabel != null) {
                    if (strategy != SetStrategy.STANDARD) Spacer(Modifier.width(6.dp))
                    Text(
                        supersetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("superset_badge_${entry.id}"),
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
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
                )
            }

            if (nextUpName != null) {
                Text(
                    "Next Up: $nextUpName",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 4.dp)
                        .testTag("next_up_${entry.id}"),
                )
            }

            sortedSets.forEach { set ->
                val type = runCatching { LoggedSetType.valueOf(set.setType) }.getOrDefault(LoggedSetType.NORMAL)
                SetRow(
                    setNumber = set.setNumber,
                    weightKg = set.weightKg,
                    reps = set.reps,
                    setType = type,
                    logged = true,
                    unit = unit,
                    onToggle = { },
                    onCycleType = { },
                    onEditWeight = { },
                    onEditReps = { },
                    onStepWeight = { },
                    onStepReps = { },
                    onDelete = {
                        AppHaptics.swipeCommit(view)
                        viewModel.removeSet(set)
                    },
                )
            }

            drafts.forEachIndexed { rowIdx, row ->
                SetRow(
                    setNumber = sortedSets.size + rowIdx + 1,
                    weightKg = row.weightKg,
                    reps = row.reps,
                    setType = row.setType,
                    logged = false,
                    unit = unit,
                    onToggle = { logRow(rowIdx) },
                    onCycleType = {
                        AppHaptics.segmentTick(view)
                        val next = LoggedSetType.entries[(row.setType.ordinal + 1) % LoggedSetType.entries.size]
                        drafts = drafts.update(rowIdx) { it.copy(setType = next) }
                    },
                    onEditWeight = { editTarget = rowIdx to true },
                    onEditReps = { editTarget = rowIdx to false },
                    onStepWeight = { delta ->
                        val display = unit.fromKg(row.weightKg) + delta
                        drafts = drafts.update(rowIdx) { it.copy(weightKg = unit.toKg(display).coerceAtLeast(0.0)) }
                    },
                    onStepReps = { delta ->
                        drafts = drafts.update(rowIdx) { it.copy(reps = (it.reps + delta).coerceAtLeast(1)) }
                    },
                    onDelete = { },
                )
            }

            TextButton(onClick = ::addSet, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add set")
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

    editTarget?.let { (rowIdx, isWeight) ->
        val row = drafts.getOrNull(rowIdx) ?: run { editTarget = null; null }
        if (row != null) {
            com.deepkush.reprange.ui.component.TextFieldDialog(
                title = if (isWeight) "Weight (${unit.symbol})" else "Reps",
                fields = listOf(
                    com.deepkush.reprange.ui.component.DialogFieldSpec(
                        initial = if (isWeight) Formatters.weightShort(row.weightKg, unit)
                        else row.reps.toString(),
                        label = if (isWeight) unit.symbol else "reps",
                        isValid = { v -> (v.toDoubleOrNull() ?: 0.0) > 0.0 },
                    ),
                ),
                onConfirm = { vals ->
                    val v = vals.first().toDoubleOrNull() ?: 0.0
                    drafts = drafts.update(rowIdx) {
                        if (isWeight) it.copy(weightKg = unit.toKg(v)) else it.copy(reps = v.toInt())
                    }
                    editTarget = null
                },
                onDismiss = { editTarget = null },
            )
        }
    }
}

private fun List<DraftRow>.update(index: Int, transform: (DraftRow) -> DraftRow): List<DraftRow> =
    mapIndexed { i, row -> if (i == index) transform(row) else row }

private data class DraftRow(
    val weightKg: Double,
    val reps: Int,
    val setType: LoggedSetType,
)

@Composable
private fun SetRow(
    setNumber: Int,
    weightKg: Double,
    reps: Int,
    setType: LoggedSetType,
    logged: Boolean,
    unit: WeightUnit,
    onToggle: () -> Unit,
    onCycleType: () -> Unit,
    onEditWeight: () -> Unit,
    onEditReps: () -> Unit,
    onStepWeight: (Float) -> Unit,
    onStepReps: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val checkCircle: @Composable () -> Unit = {
        Surface(
            shape = CircleShape,
            color = if (logged) MaterialTheme.colorScheme.primary else Color.Transparent,
            border = if (logged) null else androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onToggle, enabled = !logged),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (logged) "Logged" else "Complete set",
                    tint = if (logged) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }

    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        ) {
            checkCircle()
            Spacer(Modifier.width(8.dp))
            Text(
                "#$setNumber",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(26.dp),
            )
            StepperValue(
                value = Formatters.weightShort(weightKg, unit),
                label = unit.symbol,
                onDecrement = { onStepWeight(-2.5f) },
                onIncrement = { onStepWeight(2.5f) },
                onTap = onEditWeight,
                modifier = Modifier.weight(1f),
                readOnly = logged,
            )
            Spacer(Modifier.width(6.dp))
            StepperValue(
                value = reps.toString(),
                label = "reps",
                onDecrement = { onStepReps(-1) },
                onIncrement = { onStepReps(1) },
                onTap = onEditReps,
                modifier = Modifier.width(104.dp),
                readOnly = logged,
            )
            Spacer(Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onCycleType, enabled = !logged),
            ) {
                Icon(
                    setType.icon(),
                    contentDescription = if (logged) setType.label else "${setType.label} - tap to change",
                    tint = if (setType == LoggedSetType.NORMAL) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (logged) {
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
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
                        .padding(end = 20.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            },
            enableDismissFromStartToEnd = false,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                content()
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun StepperValue(
    value: String,
    label: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(enabled = !readOnly, onClick = onDecrement),
            ) {
                if (!readOnly) {
                    Text("−", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !readOnly, onClick = onTap)
                    .padding(horizontal = 2.dp, vertical = 3.dp),
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(enabled = !readOnly, onClick = onIncrement),
            ) {
                if (!readOnly) {
                    Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
