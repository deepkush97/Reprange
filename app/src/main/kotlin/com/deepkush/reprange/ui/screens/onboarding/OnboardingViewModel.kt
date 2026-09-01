package com.deepkush.reprange.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.domain.onboarding.EquipmentProfile
import com.deepkush.reprange.domain.onboarding.Experience
import com.deepkush.reprange.domain.onboarding.Goal
import com.deepkush.reprange.domain.onboarding.OnboardingProfile
import com.deepkush.reprange.domain.onboarding.OnboardingRecommender
import com.deepkush.reprange.domain.onboarding.PreviewTemplate
import com.deepkush.reprange.domain.onboarding.Split
import com.deepkush.reprange.domain.onboarding.validForDays
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val recommender: OnboardingRecommender,
) : ViewModel() {

    // Secondary constructor for unit tests that supply a fake ExerciseDao directly.
    constructor(dao: ExerciseDao) : this(OnboardingRecommender(dao))

    private val _profile = MutableStateFlow(
        OnboardingProfile(
            goal = Goal.GENERAL_FITNESS,
            experience = Experience.BEGINNER,
            equipment = EquipmentProfile.FULL_GYM,
            days = 3,
            split = Split.FULL_BODY,
        ),
    )
    val profile: StateFlow<OnboardingProfile> = _profile.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _preview = MutableStateFlow<List<PreviewTemplate>?>(null)
    val preview: StateFlow<List<PreviewTemplate>?> = _preview.asStateFlow()

    fun setGoal(goal: Goal) {
        _profile.value = _profile.value.copy(goal = goal)
    }

    fun setExperience(experience: Experience) {
        _profile.value = _profile.value.copy(experience = experience)
    }

    fun setEquipment(equipment: EquipmentProfile) {
        _profile.value = _profile.value.copy(equipment = equipment)
    }

    fun setDays(days: Int) {
        val clamped = days.coerceIn(2, 6)
        var updated = _profile.value.copy(days = clamped)
        // Auto-correct split if current split becomes invalid for new days
        if (!updated.split.validForDays(clamped)) {
            val firstValid = Split.entries.firstOrNull { it.validForDays(clamped) } ?: Split.FULL_BODY
            updated = updated.copy(split = firstValid)
        }
        _profile.value = updated
    }

    fun setSplit(split: Split) {
        _profile.value = _profile.value.copy(split = split)
    }

    fun nextStep() {
        if (_currentStep.value < 4) _currentStep.value += 1
    }

    fun prevStep() {
        if (_currentStep.value > 0) _currentStep.value -= 1
    }

    fun setStep(step: Int) {
        _currentStep.value = step.coerceIn(0, 4)
    }

    suspend fun generatePreview() {
        _preview.value = recommender.recommend(_profile.value)
    }

    fun generatePreviewAsync() {
        viewModelScope.launch {
            _preview.value = recommender.recommend(_profile.value)
        }
    }

    fun swapExercise(templateIndex: Int, itemIndex: Int, newExerciseId: String) {
        val current = _preview.value ?: return
        if (templateIndex !in current.indices) return
        val template = current[templateIndex]
        if (itemIndex !in template.items.indices) return
        val updatedItems = template.items.toMutableList()
        updatedItems[itemIndex] = updatedItems[itemIndex].copy(exerciseId = newExerciseId)
        val updatedTemplate = template.copy(items = updatedItems)
        val updated = current.toMutableList()
        updated[templateIndex] = updatedTemplate
        _preview.value = updated
    }
}
