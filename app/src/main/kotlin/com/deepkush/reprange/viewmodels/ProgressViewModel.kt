package com.deepkush.reprange.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.domain.usecase.CalculateProgressUseCase
import com.deepkush.reprange.domain.usecase.CreateWorkoutPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrWithExercise(
    val exerciseId: String,
    val name: String,
    val bestWeightKg: Double,
    val bestReps: Int,
    val bestEst1Rm: Double,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val calculateProgressUseCase: CalculateProgressUseCase,
) : ViewModel() {

    val totals: StateFlow<CalculateProgressUseCase.ProgressTotals> =
        calculateProgressUseCase.observeTotals()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                CalculateProgressUseCase.ProgressTotals(0.0, 0, 0),
            )

    private val _weeklyVolume = MutableStateFlow<List<CalculateProgressUseCase.WeeklyVolume>>(emptyList())
    val weeklyVolume: StateFlow<List<CalculateProgressUseCase.WeeklyVolume>> = _weeklyVolume

    private val _personalRecords = MutableStateFlow<List<PrWithExercise>>(emptyList())
    val personalRecords: StateFlow<List<PrWithExercise>> = _personalRecords

    /** Exercise selected for the 1RM-over-time chart; defaults to the top PR. */
    private val _selectedExerciseId = MutableStateFlow<String?>(null)
    val selectedExerciseId: StateFlow<String?> = _selectedExerciseId

    /** Estimated 1RM series for the selected exercise (day buckets, best Epley per day). */
    private val _oneRmSeries = MutableStateFlow<List<Pair<Long, Double>>>(emptyList())
    val oneRmSeries: StateFlow<List<Pair<Long, Double>>> = _oneRmSeries

    fun selectExercise(exerciseId: String) {
        _selectedExerciseId.value = exerciseId
        viewModelScope.launch {
            _oneRmSeries.value = calculateProgressUseCase.est1RmSeries(
                exerciseId,
                System.currentTimeMillis() - ONE_RM_HISTORY_MILLIS,
            )
        }
    }

    init {
        viewModelScope.launch {
            _weeklyVolume.value = calculateProgressUseCase.weeklyVolume(weeks = 10)
        }
        viewModelScope.launch {
            calculateProgressUseCase.observePersonalRecords().collect { rows ->
                if (rows.isEmpty()) {
                    _personalRecords.value = emptyList()
                    return@collect
                }
                val exercises = exerciseRepository.getExercises(rows.map { it.exerciseId })
                val names = exercises.associate { it.id to it.name }
                _personalRecords.value = rows
                    .sortedByDescending { it.bestEst1Rm }
                    .mapNotNull { row ->
                        names[row.exerciseId]?.let { name ->
                            PrWithExercise(
                                exerciseId = row.exerciseId,
                                name = name.replaceFirstChar { c -> c.uppercase() },
                                bestWeightKg = row.bestWeightKg,
                                bestReps = row.bestReps,
                                bestEst1Rm = row.bestEst1Rm,
                            )
                        }
                    }
                if (_selectedExerciseId.value == null) {
                    _personalRecords.value.firstOrNull()?.exerciseId?.let { selectExercise(it) }
                }
            }
        }
    }

    companion object {
        /** Lookback window for the 1RM-over-time chart (90 days). */
        const val ONE_RM_HISTORY_MILLIS = 90L * 24L * 3600_000L
    }
}
