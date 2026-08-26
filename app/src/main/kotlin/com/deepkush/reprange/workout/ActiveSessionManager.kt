package com.deepkush.reprange.workout

import com.deepkush.reprange.data.db.CompletedSetEntity
import com.deepkush.reprange.data.db.LoggedSetType
import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.data.db.SessionWithExercises
import com.deepkush.reprange.data.db.WorkoutSessionEntity
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.data.repo.SessionRepository
import com.deepkush.reprange.domain.usecase.LogCompletedSetUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide live workout state: survives navigation (and process death via Room,
 * since an unfinished session is persisted with ended_at = NULL). Owns the rest timer.
 */
@Singleton
class ActiveSessionManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logCompletedSetUseCase: LogCompletedSetUseCase,
) {

    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    private val _session = MutableStateFlow<SessionWithExercises?>(null)
    val session: StateFlow<SessionWithExercises?> = _session

    private val _restRemainingSeconds = MutableStateFlow<Int?>(null)
    val restRemainingSeconds: StateFlow<Int?> = _restRemainingSeconds

    /** Transient "New PR!" flash keyed by exercise name; UI consumes and clears. */
    private val _prFlash = MutableStateFlow<String?>(null)
    val prFlash: StateFlow<String?> = _prFlash

    private var restJob: Job? = null

    init {
        scope.launch {
            sessionRepository.observeActiveSession().collect { active ->
                _session.value = active
            }
        }
    }

    fun hasActive(): Boolean = _session.value != null

    suspend fun addExercise(exerciseId: String, restSeconds: Int = 90): Boolean {
        val current = _session.value ?: return false
        if (current.exercises.any { it.entry.exerciseId == exerciseId }) return false
        sessionRepository.insertEntries(
            listOf(
                SessionExerciseEntity(
                    sessionId = current.session.id,
                    exerciseId = exerciseId,
                    orderIndex = current.exercises.size,
                    restSeconds = restSeconds,
                ),
            ),
        )
        return true
    }

    data class Suggestion(val weightKg: Double, val reps: Int)

    /** Auto-populate from previous performance: last normal set of this exercise. */
    suspend fun suggestionFor(exerciseId: String): Suggestion? =
        sessionRepository.previousSets(exerciseId, System.currentTimeMillis())
            .firstOrNull()
            ?.let { Suggestion(it.weightKg, it.reps) }

    /**
     * Step-up/pyramid auto-fill: escalate weight per completed set index while trimming reps.
     */
    fun stepUpSuggestion(base: Suggestion?, setIndex: Int, incrementKg: Double = 2.5): Suggestion? =
        base?.let { s ->
            if (setIndex <= 0) s else Suggestion(s.weightKg + incrementKg * setIndex, s.reps)
        }

    suspend fun logSet(
        entry: SessionExerciseEntity,
        setType: LoggedSetType,
        weightKg: Double,
        reps: Int,
        restSeconds: Int,
        autoStartRest: Boolean,
    ): Boolean {
        val current = _session.value ?: return false
        val isPr = logCompletedSetUseCase(current.session.id, entry, setType, weightKg, reps)
        if (isPr) _prFlash.value = entry.exerciseId
        if (autoStartRest && setType == LoggedSetType.NORMAL && restSeconds > 0) {
            startRest(restSeconds)
        }
        return true
    }

    suspend fun removeSet(set: CompletedSetEntity) {
        val current = _session.value ?: return
        sessionRepository.deleteSet(set)
        sessionRepository.updateSession(
            current.session.copy(
                totalVolumeKg = (current.session.totalVolumeKg - set.weightKg * set.reps).coerceAtLeast(0.0),
            ),
        )
    }

    suspend fun removeEntry(entry: SessionExerciseEntity) {
        sessionRepository.deleteEntry(entry)
    }

    fun startRest(seconds: Int) {
        restJob?.cancel()
        _restRemainingSeconds.value = seconds.coerceAtLeast(1)
        restJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _restRemainingSeconds.value = remaining
            }
            _restRemainingSeconds.value = null
        }
    }

    fun addRestTime(seconds: Int) {
        val current = _restRemainingSeconds.value ?: return
        startRest(current + seconds)
    }

    fun skipRest() {
        restJob?.cancel()
        _restRemainingSeconds.value = null
    }

    fun clearPrFlash() {
        _prFlash.value = null
    }

    suspend fun finish(): WorkoutSessionEntity? {
        val current = _session.value ?: return null
        skipRest()
        val finished = current.session.copy(endedAt = System.currentTimeMillis())
        sessionRepository.updateSession(finished)
        return finished
    }

    suspend fun discard() {
        val current = _session.value ?: return
        skipRest()
        // Deleting the session cascades to entries and completed sets.
        sessionRepository.deleteSession(current.session.id)
    }
}
