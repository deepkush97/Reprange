package com.deepkush.reprange.ui.screens.onboarding

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.domain.onboarding.EquipmentProfile
import com.deepkush.reprange.domain.onboarding.Experience
import com.deepkush.reprange.domain.onboarding.Goal
import com.deepkush.reprange.domain.onboarding.OnboardingProfile
import com.deepkush.reprange.domain.onboarding.OnboardingRecommender
import com.deepkush.reprange.domain.onboarding.PreviewTemplate
import com.deepkush.reprange.domain.onboarding.Split
import com.deepkush.reprange.domain.onboarding.config
import com.deepkush.reprange.domain.onboarding.validForDays
import com.deepkush.reprange.domain.usecase.CreateWorkoutPlanUseCase
import com.deepkush.reprange.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val recommender: OnboardingRecommender,
    private val exerciseRepository: ExerciseRepository,
    private val createWorkoutPlanUseCase: CreateWorkoutPlanUseCase? = null,
    @ApplicationContext private val appContext: Context? = null,
) : ViewModel() {

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

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isGeneratingPreview = MutableStateFlow(false)
    val isGeneratingPreview: StateFlow<Boolean> = _isGeneratingPreview.asStateFlow()

    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    private val _exerciseMap = MutableStateFlow<Map<String, ExerciseEntity>>(emptyMap())
    val exerciseMap: StateFlow<Map<String, ExerciseEntity>> = _exerciseMap.asStateFlow()

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
        _isGeneratingPreview.value = true
        _previewError.value = null
        try {
            val result = recommender.recommend(_profile.value)
            _preview.value = result
            refreshExerciseMap()
        } catch (e: Exception) {
            _previewError.value = e.message ?: "Failed to generate preview."
            throw e
        } finally {
            _isGeneratingPreview.value = false
        }
    }

    fun generatePreviewAsync() {
        viewModelScope.launch {
            _isGeneratingPreview.value = true
            _previewError.value = null
            try {
                val result = recommender.recommend(_profile.value)
                _preview.value = result
                refreshExerciseMap()
            } catch (e: Exception) {
                _previewError.value = e.message ?: "Failed to generate preview."
            } finally {
                _isGeneratingPreview.value = false
            }
        }
    }

    val seedState: StateFlow<DatasetSeeder.SeedState>
        get() = exerciseRepository.seedState

    fun retrySeed() {
        viewModelScope.launch { exerciseRepository.seedIfNeeded() }
    }

    fun swapExercise(templateIndex: Int, itemIndex: Int, newExerciseId: String) {
        val current = _preview.value ?: return
        if (templateIndex !in current.indices) return
        val template = current[templateIndex]
        if (itemIndex == template.items.size) {
            // Placeholder add case: append new exercise
            addExercise(templateIndex, newExerciseId)
            return
        }
        if (itemIndex !in template.items.indices) return
        val updatedItems = template.items.toMutableList()
        updatedItems[itemIndex] = updatedItems[itemIndex].copy(exerciseId = newExerciseId)
        val updatedTemplate = template.copy(items = updatedItems)
        val updated = current.toMutableList()
        updated[templateIndex] = updatedTemplate
        _preview.value = updated
        viewModelScope.launch { fetchAndCacheExercise(newExerciseId) }
    }

    fun addExercise(templateIndex: Int, newExerciseId: String) {
        val current = _preview.value ?: return
        if (templateIndex !in current.indices) return
        val template = current[templateIndex]
        val config = _profile.value.goal.config()
        // Use profile's goal config for added item; strategy similar to recommender
        val strategy = com.deepkush.reprange.data.db.SetStrategy.STANDARD
        val newItem = com.deepkush.reprange.domain.usecase.PlanItem(
            exerciseId = newExerciseId,
            setCount = config.sets,
            strategy = strategy,
            targetReps = config.repLow,
            restSeconds = config.restSeconds,
        )
        val updatedItems = template.items + newItem
        val updatedTemplate = template.copy(items = updatedItems)
        val updated = current.toMutableList()
        updated[templateIndex] = updatedTemplate
        _preview.value = updated
        viewModelScope.launch { fetchAndCacheExercise(newExerciseId) }
    }

    suspend fun saveAll(): Result<Unit> {
        _isSaving.value = true
        _previewError.value = null
        return try {
            val current = _preview.value
            if (current.isNullOrEmpty()) {
                val msg = "Nothing to save."
                _previewError.value = msg
                Result.failure(IllegalStateException(msg))
            } else {
                val useCase = createWorkoutPlanUseCase
                    ?: return Result.failure(IllegalStateException("CreateWorkoutPlanUseCase not available"))
                val ctx = appContext
                    ?: return Result.failure(IllegalStateException("Context not available"))
                current.forEach { tmpl ->
                    useCase(null, tmpl.title, "", tmpl.items)
                }
                ctx.dataStore.edit { prefs ->
                    prefs[PreferenceKeys.ONBOARDING_COMPLETED] = true
                    prefs[PreferenceKeys.ONBOARDING_GOAL] = _profile.value.goal.name
                    prefs[PreferenceKeys.ONBOARDING_EXPERIENCE] = _profile.value.experience.name
                    prefs[PreferenceKeys.ONBOARDING_EQUIPMENT] = _profile.value.equipment.name
                    prefs[PreferenceKeys.ONBOARDING_DAYS] = _profile.value.days
                    prefs[PreferenceKeys.ONBOARDING_SPLIT] = _profile.value.split.name
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to save routines."
            _previewError.value = msg
            Result.failure(e)
        } finally {
            _isSaving.value = false
        }
    }

    suspend fun skipOnboarding() {
        _previewError.value = null
        val ctx = appContext ?: return
        ctx.dataStore.edit { prefs ->
            prefs[PreferenceKeys.ONBOARDING_COMPLETED] = true
        }
    }

    fun clearPreviewError() {
        _previewError.value = null
    }

    private suspend fun refreshExerciseMap() {
        val ids = _preview.value?.flatMap { it.items.map { item -> item.exerciseId } }?.distinct() ?: emptyList()
        if (ids.isEmpty()) {
            _exerciseMap.value = emptyMap()
            return
        }
        val fetched = exerciseRepository.getExercises(ids)
        _exerciseMap.value = fetched.associateBy { it.id }
    }

    private suspend fun fetchAndCacheExercise(id: String) {
        if (id in _exerciseMap.value) return
        val entity = exerciseRepository.getExercise(id) ?: return
        _exerciseMap.value = _exerciseMap.value + (entity.id to entity)
    }
}
