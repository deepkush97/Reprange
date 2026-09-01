package com.deepkush.reprange.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.domain.onboarding.EquipmentProfile
import com.deepkush.reprange.domain.onboarding.Experience
import com.deepkush.reprange.domain.onboarding.Goal
import com.deepkush.reprange.domain.onboarding.Split
import com.deepkush.reprange.domain.onboarding.validForDays
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.dataStore
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var previewError by remember { mutableStateOf<String?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }

    fun persistAndFinish() {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.ONBOARDING_COMPLETED] = true
                prefs[PreferenceKeys.ONBOARDING_GOAL] = profile.goal.name
                prefs[PreferenceKeys.ONBOARDING_EXPERIENCE] = profile.experience.name
                prefs[PreferenceKeys.ONBOARDING_EQUIPMENT] = profile.equipment.name
                prefs[PreferenceKeys.ONBOARDING_DAYS] = profile.days
                prefs[PreferenceKeys.ONBOARDING_SPLIT] = profile.split.name
            }
            onFinish()
        }
    }

    fun onSkip() {
        AppHaptics.tap(view)
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.ONBOARDING_COMPLETED] = true
            }
            onFinish()
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                TextButton(onClick = ::onSkip) {
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
                                previewError = null
                                viewModel.nextStep()
                            } else {
                                scope.launch {
                                    isGeneratingPreview = true
                                    previewError = null
                                    try {
                                        viewModel.generatePreview()
                                        val result = viewModel.preview.value
                                        if (result == null) {
                                            val msg = "Failed to generate preview. Please try again."
                                            previewError = msg
                                            snackbarHostState.showSnackbar(msg)
                                        } else {
                                            persistAndFinish()
                                        }
                                    } catch (e: Exception) {
                                        val msg = e.message ?: "Failed to generate preview. Please try again."
                                        previewError = msg
                                        snackbarHostState.showSnackbar(msg)
                                    } finally {
                                        isGeneratingPreview = false
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingPreview,
                    ) {
                        Text(if (currentStep < 4) "Continue" else "Finish")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
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
private fun GoalStep(selected: Goal, onSelect: (Goal) -> Unit) {
    StepHeader(title = "What's your goal?", subtitle = "This sets sets, reps and rest for your routines.")
    Spacer(Modifier.height(16.dp))
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
