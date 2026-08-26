package com.deepkush.reprange.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.db.CompletedSetEntity
import com.deepkush.reprange.data.db.LoggedSetType
import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.domain.usecase.StartWorkoutSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    val manager: ActiveSessionManager,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val session get() = manager.session
    val restRemainingSeconds get() = manager.restRemainingSeconds
    val prFlash get() = manager.prFlash

    fun startBlank(onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            onStarted(startWorkoutSessionUseCase.blank())
        }
    }

    fun startTemplate(templateId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val id = startWorkoutSessionUseCase.fromTemplate(templateId)
            if (id != null) onStarted(id) else onStarted(-1L)
        }
    }

    /** Adds an exercise picked from the library into the live session. */
    fun addToActiveSession(exerciseId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(manager.addExercise(exerciseId)) }
    }

    suspend fun suggestionFor(exerciseId: String): ActiveSessionManager.Suggestion? =
        manager.suggestionFor(exerciseId)

    fun logSet(
        entry: SessionExerciseEntity,
        setType: LoggedSetType,
        weightKg: Double,
        reps: Int,
        restSeconds: Int,
        autoStartRest: Boolean,
    ) {
        viewModelScope.launch {
            manager.logSet(entry, setType, weightKg, reps, restSeconds, autoStartRest)
        }
    }

    fun removeSet(set: CompletedSetEntity) {
        viewModelScope.launch { manager.removeSet(set) }
    }

    fun removeEntry(entry: SessionExerciseEntity) {
        viewModelScope.launch { manager.removeEntry(entry) }
    }

    fun skipRest() = manager.skipRest()

    fun addRestTime(seconds: Int) = manager.addRestTime(seconds)

    fun clearPrFlash() = manager.clearPrFlash()

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            manager.discard()
            onDone()
        }
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            manager.finish()
            onDone()
        }
    }

    /** Resolves display names for the given exercise ids. */
    suspend fun namesFor(exerciseIds: List<String>): Map<String, String> =
        exerciseRepository.getExercises(exerciseIds.distinct())
            .associate { it.id to it.name }
}
