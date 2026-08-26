package com.deepkush.reprange.domain.usecase

import com.deepkush.reprange.data.db.CompletedSetEntity
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.db.LoggedSetType
import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.data.db.TemplateExerciseEntity
import com.deepkush.reprange.data.db.WorkoutSessionEntity
import com.deepkush.reprange.data.db.WorkoutTemplateEntity
import com.deepkush.reprange.data.repo.SessionRepository
import com.deepkush.reprange.data.repo.TemplateRepository
import com.deepkush.reprange.utils.OneRmEstimator
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class PlanItem(
    val exerciseId: String,
    val setCount: Int = 3,
    val strategy: SetStrategy = SetStrategy.STANDARD,
    val targetWeightKg: Double? = null,
    val targetReps: Int? = null,
    val restSeconds: Int? = null,
    val supersetGroup: String? = null,
)

/** Creates/updates a saved workout template (Plan Ahead). */
class CreateWorkoutPlanUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(templateId: Long?, title: String, notes: String, items: List<PlanItem>): Long {
        val now = System.currentTimeMillis()
        val id = templateRepository.upsertTemplate(
            WorkoutTemplateEntity(id = templateId ?: 0, title = title.trim(), notes = notes.trim(), createdAt = now),
        )
        templateRepository.replaceExercises(
            id,
            items.mapIndexed { index, item ->
                TemplateExerciseEntity(
                    templateId = id,
                    exerciseId = item.exerciseId,
                    orderIndex = index,
                    setCount = item.setCount.coerceIn(1, 12),
                    strategy = item.strategy.name,
                    targetWeightKg = item.targetWeightKg,
                    targetReps = item.targetReps,
                    restSeconds = item.restSeconds,
                    supersetGroup = item.supersetGroup,
                )
            },
        )
        return id
    }
}

/** Starts a live session from a saved template or blank. */
class StartWorkoutSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val templateRepository: TemplateRepository,
    private val exerciseRepository: com.deepkush.reprange.data.repo.ExerciseRepository,
) {
    suspend fun fromTemplate(templateId: Long): Long? {
        val templateWithEntries =
            templateRepository.observeTemplate(templateId).firstOrNull() ?: return null
        val exercises = exerciseRepository.getExercises(templateWithEntries.entries.map { it.exerciseId })
        return create(
            templateId = templateId,
            exercises = exercises,
            entries = templateWithEntries.entries
                .sortedBy { it.orderIndex }
                .map { e ->
                    StartEntry(
                        exerciseId = e.exerciseId,
                        restSeconds = e.restSeconds ?: 90,
                        supersetGroup = e.supersetGroup,
                    )
                },
        )
    }

    suspend fun blank(): Long = create(templateId = null, exercises = emptyList(), entries = emptyList())

    data class StartEntry(val exerciseId: String, val restSeconds: Int, val supersetGroup: String?)

    private suspend fun create(
        templateId: Long?,
        exercises: List<ExerciseEntity>,
        entries: List<StartEntry>,
    ): Long {
        val validIds = exercises.map { it.id }.toSet()
        val filtered = entries.filter { it.exerciseId in validIds }
        val sessionId = sessionRepository.insertSession(
            WorkoutSessionEntity(templateId = templateId, startedAt = System.currentTimeMillis()),
        )
        if (filtered.isNotEmpty()) {
            sessionRepository.insertEntries(
                filtered.mapIndexed { i, e ->
                    SessionExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = e.exerciseId,
                        orderIndex = i,
                        restSeconds = e.restSeconds,
                        supersetGroup = e.supersetGroup,
                    )
                },
            )
        }
        return sessionId
    }
}

/**
 * Logs a completed set, updates session volume, and detects personal records using the
 * estimated-1RM comparison (Epley). Returns true when this set is a new PR.
 */
class LogCompletedSetUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        entry: SessionExerciseEntity,
        setType: LoggedSetType,
        weightKg: Double,
        reps: Int,
    ): Boolean {
        if (weightKg <= 0.0 || reps <= 0) return false
        val existing = sessionRepository.getActiveSession()
            ?.exercises
            ?.firstOrNull { it.entry.id == entry.id }
            ?.sets
            .orEmpty()
            .filter { it.setType == LoggedSetType.NORMAL.name }

        val priorBest = existing.maxOfOrNull { OneRmEstimator.prScore(it.weightKg, it.reps) } ?: 0.0
        val isNewPr = setType == LoggedSetType.NORMAL &&
            existing.isNotEmpty() &&
            OneRmEstimator.prScore(weightKg, reps) > priorBest

        val nextNumber = (existing.maxOfOrNull { it.setNumber } ?: 0) + 1
        sessionRepository.insertSet(
            CompletedSetEntity(
                sessionId = sessionId,
                sessionExerciseId = entry.id,
                exerciseId = entry.exerciseId,
                setNumber = nextNumber,
                setType = setType.name,
                weightKg = weightKg,
                reps = reps,
                completedAt = System.currentTimeMillis(),
            ),
        )
        sessionRepository.getActiveSession()?.let { current ->
            sessionRepository.updateSession(
                current.session.copy(totalVolumeKg = current.session.totalVolumeKg + weightKg * reps),
            )
        }
        return isNewPr
    }
}

/** Analytics: volume buckets, estimated 1RM series, personal records, completion counts. */
class CalculateProgressUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    data class WeeklyVolume(val weekStartMillis: Long, val volumeKg: Double)

    data class ProgressTotals(val weekVolumeKg: Double, val weekSets: Int, val prCount: Int)

    suspend fun weeklyVolume(weeks: Int = 10, now: Long = System.currentTimeMillis()): List<WeeklyVolume> {
        val since = now - weeks.toLong() * 7L * 24L * 3600_000L
        val points = sessionRepository.allNormalSetsSince(since)
        if (points.isEmpty()) return List(weeks) { WeeklyVolume(now - ((weeks - 1 - it) * 7L * 24L * 3600_000L), 0.0) }
        val weekMillis = 7L * 24L * 3600_000L
        val weekStarts = (0 until weeks).map { i -> now - (weeks - 1 - i) * weekMillis }
        val bucketed: Map<Long, Double> = points.groupBy { p ->
            val offsetDays = ((now - p.ts) / (24L * 3600_000L)).toInt()
            val bucketIndex = (offsetDays / 7).coerceAtMost(weeks - 1)
            now - bucketIndex * weekMillis
        }.mapValues { (_, ps) -> ps.sumOf { it.weightKg * it.reps } }
        return weekStarts.map { start -> WeeklyVolume(start, bucketed[start] ?: 0.0) }
    }

    fun observeTotals(now: Long = System.currentTimeMillis()): kotlinx.coroutines.flow.Flow<ProgressTotals> {
        val weekAgo = now - 7L * 24L * 3600_000L
        val volume = sessionRepository.observeVolumeBetween(weekAgo, now)
        val sets = sessionRepository.observeSetCountBetween(weekAgo, now)
        val prs = sessionRepository.observePrTable()
        return kotlinx.coroutines.flow.combine(volume, sets, prs) { v, s, p ->
            ProgressTotals(v, s, p.size)
        }
    }

    fun observePersonalRecords() = sessionRepository.observePrTable()

    /** Estimated 1RM series for one exercise over time (for the progress chart). */
    suspend fun est1RmSeries(exerciseId: String, since: Long): List<Pair<Long, Double>> =
        sessionRepository.normalSetsSince(exerciseId, since)
            .groupBy { dayBucket(it.ts) }
            .map { (day, pts) -> day to pts.maxOf { OneRmEstimator.epley(it.weightKg, it.reps) } }
            .sortedBy { it.first }

    private fun dayBucket(ts: Long): Long = ts / (24L * 3600_000L) * (24L * 3600_000L)
}
