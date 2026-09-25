package com.deepkush.reprange.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.domain.onboarding.EquipmentProfile
import com.deepkush.reprange.domain.onboarding.Experience
import com.deepkush.reprange.domain.onboarding.Goal
import com.deepkush.reprange.domain.onboarding.Split
import com.deepkush.reprange.domain.onboarding.validForDays
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onPickExercise: (templateIndex: Int, itemIndex: Int, bucket: String?) -> Unit = { _, _, _ -> },
    onExerciseClick: (String) -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val seedState by viewModel.seedState.collectAsStateWithLifecycle()
    val exerciseMap by viewModel.exerciseMap.collectAsStateWithLifecycle()
    val previewError by viewModel.previewError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isGeneratingPreview by viewModel.isGeneratingPreview.collectAsStateWithLifecycle()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    // Auto-show preview when it becomes non-null (after generation)
    LaunchedEffect(preview) {
        if (preview != null) showPreview = true
    }

    LaunchedEffect(previewError) {
        previewError?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        if (showPreview && preview != null) {
            // Preview pager mode
            val templates = preview!!
            val pagerState = rememberPagerState(pageCount = { templates.size })
            var selectedTab by remember { mutableStateOf(0) }
            LaunchedEffect(selectedTab) {
                if (selectedTab != pagerState.currentPage) {
                    pagerState.animateScrollToPage(selectedTab)
                }
            }
            LaunchedEffect(pagerState.currentPage) {
                selectedTab = pagerState.currentPage
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
                Spacer(Modifier.height(16.dp))
                Text("Your recommended routines", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text("Preview and swap any exercise before saving.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                if (templates.size > 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        templates.forEachIndexed { idx, tmpl ->
                            SegmentedButton(
                                selected = idx == selectedTab,
                                onClick = {
                                    AppHaptics.segmentTick(view)
                                    selectedTab = idx
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = templates.size),
                            ) {
                                Text(
                                    text = tmpl.title.substringBefore(" —").take(10),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(templates.first().title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }

                if (previewError != null) {
                    Text(
                        text = previewError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }

                when (seedState) {
                    is DatasetSeeder.SeedState.Loading -> {
                        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Fetching the exercise dataset…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    is DatasetSeeder.SeedState.Error -> {
                        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Text("Failed to load dataset", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    (seedState as DatasetSeeder.SeedState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(16.dp))
                                TextButton(onClick = { viewModel.retrySeed() }) { Text("Retry") }
                            }
                        }
                    }
                    else -> {
                        Box(Modifier.weight(1f)) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val template = templates[page]
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            item {
                                Text(
                                    text = "${template.items.size} exercises · ${template.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                )
                            }
                            itemsIndexed(template.items, key = { idx, item -> "$page-$idx-${item.exerciseId}" }) { itemIndex, item ->
                                val exercise = remember(item.exerciseId, exerciseMap) { exerciseMap[item.exerciseId] }
                                PreviewExerciseRow(
                                    exerciseId = item.exerciseId,
                                    exercise = exercise,
                                    setCount = item.setCount,
                                    reps = item.targetReps,
                                    restSeconds = item.restSeconds,
                                    strategy = item.strategy.name,
                                    onSwap = {
                                        AppHaptics.tap(view)
                                        onPickExercise(page, itemIndex, exercise?.category)
                                    },
                                    onExerciseClick = onExerciseClick,
                                )
                            }
                            // Short-list placeholder: if bucket was empty (even after fallback), show "+ Add exercise"
                            item {
                                val expected = expectedCountForTemplate(profile.split, template.title)
                                if (template.items.size < expected) {
                                    val missing = expected - template.items.size
                                    AddExercisePlaceholder(
                                        missing = missing,
                                        onAdd = {
                                            AppHaptics.tap(view)
                                            onPickExercise(page, template.items.size, null)
                                        },
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(120.dp)) }
                        }
                    }
                        }
                    }
                }

                // Footer for preview: Back to edit + Create routines
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        AppHaptics.tap(view)
                        showPreview = false
                    }) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            AppHaptics.primaryTap(view)
                            scope.launch {
                                val result = viewModel.saveAll()
                                if (result.isSuccess) {
                                    onFinish()
                                } else {
                                    val msg = viewModel.previewError.value ?: "Failed to save routines."
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        enabled = !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (templates.size == 1) "Create routine" else "Create ${templates.size} routines")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            // Wizard mode (5 steps)
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
                Spacer(Modifier.height(16.dp))
                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(5) { index ->
                        val isActive = index == currentStep
                        val isCompleted = index < currentStep
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isActive) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isActive -> MaterialTheme.colorScheme.primary
                                        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${currentStep + 1} / 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(20.dp))

                when (seedState) {
                    is DatasetSeeder.SeedState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Loading exercise database…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is DatasetSeeder.SeedState.Error -> {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            onClick = { viewModel.retrySeed() },
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Couldn't load exercises",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        (seedState as DatasetSeeder.SeedState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                        maxLines = 2,
                                    )
                                }
                                TextButton(onClick = { viewModel.retrySeed() }) { Text("Retry") }
                            }
                        }
                    }
                    else -> {}
                }

                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it / 3 } + fadeIn(tween(220)) togetherWith
                                    slideOutHorizontally { -it / 3 } + fadeOut(tween(220))
                            } else {
                                slideInHorizontally { -it / 3 } + fadeIn(tween(220)) togetherWith
                                    slideOutHorizontally { it / 3 } + fadeOut(tween(220))
                            }
                        },
                        label = "onboarding_step",
                    ) { step ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            when (step) {
                                0 -> GoalStep(selected = profile.goal, onSelect = { AppHaptics.segmentTick(view); viewModel.setGoal(it) })
                                1 -> ExperienceStep(selected = profile.experience, onSelect = { AppHaptics.segmentTick(view); viewModel.setExperience(it) })
                                2 -> EquipmentStep(selected = profile.equipment, onSelect = { AppHaptics.segmentTick(view); viewModel.setEquipment(it) })
                                3 -> DaysStep(days = profile.days, onDaysChange = { newDays -> AppHaptics.scrubTick(view); viewModel.setDays(newDays) })
                                4 -> SplitStep(selected = profile.split, days = profile.days, onSelect = { AppHaptics.segmentTick(view); viewModel.setSplit(it) })
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }

                if (previewError != null) {
                    Text(
                        text = previewError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        AppHaptics.tap(view)
                        scope.launch {
                            viewModel.skipOnboarding()
                            onFinish()
                        }
                    }) {
                        Text("Skip")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentStep > 0) {
                            TextButton(onClick = { AppHaptics.tap(view); viewModel.prevStep() }) {
                                Text("Back")
                            }
                        }
                        Button(
                            onClick = {
                                AppHaptics.primaryTap(view)
                                if (currentStep < 4) {
                                    viewModel.clearPreviewError()
                                    viewModel.nextStep()
                                } else {
                                    scope.launch {
                                        try {
                                            viewModel.generatePreview()
                                            val result = viewModel.preview.value
                                            if (result == null || result.isEmpty()) {
                                                val msg = "Failed to generate preview. Please try again."
                                                snackbarHostState.showSnackbar(msg)
                                            } else {
                                                showPreview = true
                                            }
                                        } catch (e: Exception) {
                                            val msg = e.message ?: "Failed to generate preview. Please try again."
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                }
                            },
                            enabled = !isGeneratingPreview && !isSaving && seedState !is DatasetSeeder.SeedState.Loading,
                        ) {
                            if (isGeneratingPreview) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (currentStep < 4) "Continue" else "Preview")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun PreviewExerciseRow(
    exerciseId: String,
    exercise: ExerciseEntity?,
    setCount: Int,
    reps: Int?,
    restSeconds: Int?,
    strategy: String,
    onSwap: () -> Unit,
    onExerciseClick: (String) -> Unit = {},
) {
    val view = LocalView.current
    // Reuse ExerciseRow visual pattern: Card with surfaceVariant, rounded corners, swap IconButton.
    // Lookup ExerciseEntity for display; fallback to raw id.
    val title = remember(exercise, exerciseId) { exercise?.name ?: exerciseId }
    val subtitle = remember(exercise) {
        if (exercise != null) {
            "${exercise.muscleGroup.replaceFirstChar { it.uppercase() }} · ${exercise.equipment.replaceFirstChar { it.uppercase() }}"
        } else {
            "Unknown exercise"
        }
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        AppHaptics.tap(view)
                        onExerciseClick(exerciseId)
                    },
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp),
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (exercise != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(exercise.equipment, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            border = null,
                            modifier = Modifier.height(22.dp),
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(exercise.muscleGroup, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                            border = null,
                            modifier = Modifier.height(22.dp),
                        )
                    }
                    Text(
                        buildString {
                            append("$setCount sets")
                            if (reps != null) append(" · $reps reps")
                            if (restSeconds != null) append(" · ${restSeconds}s rest")
                            append(" · $strategy")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onSwap) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap exercise")
            }
        }
    }
}

@Composable
private fun GoalStep(selected: Goal, onSelect: (Goal) -> Unit) {
    StepHeader(title = "What's your goal?", subtitle = "This sets sets, reps and rest for your routines.")
    Spacer(Modifier.height(16.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Goal.entries.forEach { goal ->
                FilterChip(
                    selected = goal == selected,
                    onClick = { onSelect(goal) },
                    label = { Text(goal.label()) },
                )
            }
        }
    }
}

@Composable
private fun ExperienceStep(selected: Experience, onSelect: (Experience) -> Unit) {
    StepHeader(title = "Experience level", subtitle = "We use this to filter exercise difficulty.")
    Spacer(Modifier.height(16.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Experience.entries.forEach { exp ->
                FilterChip(
                    selected = exp == selected,
                    onClick = { onSelect(exp) },
                    label = { Text(exp.label()) },
                )
            }
        }
    }
}

@Composable
private fun EquipmentStep(selected: EquipmentProfile, onSelect: (EquipmentProfile) -> Unit) {
    StepHeader(title = "Available equipment", subtitle = "Only exercises matching your equipment will be suggested.")
    Spacer(Modifier.height(16.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EquipmentProfile.entries.forEach { eq ->
                FilterChip(
                    selected = eq == selected,
                    onClick = { onSelect(eq) },
                    label = { Text(eq.label()) },
                )
            }
        }
    }
}

@Composable
private fun DaysStep(days: Int, onDaysChange: (Int) -> Unit) {
    StepHeader(title = "Days per week", subtitle = "How many days can you train? 2–6.")
    Spacer(Modifier.height(16.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onDaysChange(days - 1) },
                enabled = days > 2,
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$days",
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = "days / week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { onDaysChange(days + 1) },
                enabled = days < 6,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "You can change this later in settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun SplitStep(selected: Split, days: Int, onSelect: (Split) -> Unit) {
    StepHeader(title = "Preferred split", subtitle = "Options filtered by your $days days/week.")
    Spacer(Modifier.height(16.dp))
    val available = Split.entries.filter { it.validForDays(days) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            available.forEach { split ->
                FilterChip(
                    selected = split == selected,
                    onClick = { onSelect(split) },
                    label = { Text(split.label()) },
                )
            }
            if (available.isEmpty()) {
                Text(
                    "No splits available for $days days. Try adjusting days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun expectedCountForTemplate(split: Split, title: String): Int = when (split) {
    Split.FULL_BODY -> 6
    Split.UPPER_LOWER -> 5
    Split.PPL -> 5
}

@Composable
private fun AddExercisePlaceholder(missing: Int, onAdd: () -> Unit) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onAdd),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "+ Add exercise",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (missing > 1) {
                    Text(
                        "$missing exercises missing — tap to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "No exercise found for this bucket — tap to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Goal.label(): String = when (this) {
    Goal.STRENGTH -> "Strength"
    Goal.HYPERTROPHY -> "Hypertrophy"
    Goal.FAT_LOSS -> "Fat loss"
    Goal.GENERAL_FITNESS -> "General fitness"
}

private fun Experience.label(): String = when (this) {
    Experience.BEGINNER -> "Beginner"
    Experience.INTERMEDIATE -> "Intermediate"
    Experience.ADVANCED -> "Advanced"
}

private fun EquipmentProfile.label(): String = when (this) {
    EquipmentProfile.BODYWEIGHT -> "Bodyweight"
    EquipmentProfile.DUMBBELL_ONLY -> "Dumbbells"
    EquipmentProfile.BARBELL_DUMBBELL -> "Barbell + Dumbbells"
    EquipmentProfile.FULL_GYM -> "Full gym"
}

private fun Split.label(): String = when (this) {
    Split.FULL_BODY -> "Full body"
    Split.UPPER_LOWER -> "Upper / Lower"
    Split.PPL -> "Push / Pull / Legs"
}
