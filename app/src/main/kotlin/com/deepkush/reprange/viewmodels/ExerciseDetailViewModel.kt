package com.deepkush.reprange.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.domain.usecase.StartWorkoutSessionUseCase
import com.deepkush.reprange.workout.ActiveSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val activeSessionManager: ActiveSessionManager,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
) : ViewModel() {

    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""

    val exercise: StateFlow<ExerciseEntity?> = exerciseRepository.observeExercise(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun hasActiveSession(): Boolean = activeSessionManager.hasActive()

    /** "Start now" - creates a fresh session containing only this exercise. */
    fun startNow(onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = startWorkoutSessionUseCase.blank()
            activeSessionManager.addExercise(exerciseId)
            onStarted(sessionId)
        }
    }

    /** Adds this exercise to the currently running workout. */
    fun addToLiveSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(activeSessionManager.addExercise(exerciseId))
        }
    }
}
