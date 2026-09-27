package com.deepkush.reprange.domain.onboarding

import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.data.repo.Difficulty
import com.deepkush.reprange.domain.usecase.PlanItem
import javax.inject.Inject

data class PreviewTemplate(
    val title: String,
    val items: List<PlanItem>,
)

class OnboardingRecommender @Inject constructor(
    private val exerciseDao: ExerciseDao,
) {

    suspend fun recommend(profile: OnboardingProfile): List<PreviewTemplate> {
        val config = profile.goal.config()
        val allowedEquipment = profile.allowedEquipment()
        val isFullGym = allowedEquipment.isEmpty()
        val maxDifficulty = profile.maxDifficulty()
        val allowedDiffs = diffsFor(maxDifficulty)
        val fallbackDiffs = fallbackDiffsFor(maxDifficulty)
        val usedIds = mutableSetOf<String>()

        // Helper to pick n exercises for a set of target aliases
        suspend fun pick(targets: List<String>, count: Int): List<ExerciseEntity> {
            if (count <= 0) return emptyList()
            // First attempt with allowed diffs
            val first = queryTargets(targets, allowedDiffs, allowedEquipment, isFullGym, count + 5)
                .filter { it.id !in usedIds }
            if (first.size >= count) {
                return first.take(count)
            }
            // Fallback one tier if not enough
            if (fallbackDiffs != allowedDiffs) {
                val fallback = queryTargets(targets, fallbackDiffs, allowedEquipment, isFullGym, count + 10)
                    .filter { it.id !in usedIds }
                if (fallback.size >= count) {
                    return fallback.take(count)
                }
                // If still not enough, return whatever we have (fallback)
                if (fallback.isNotEmpty()) return fallback.take(count)
            }
            return first.take(count)
        }

        fun strategyFor(targets: List<String>): SetStrategy {
            val compoundTargets = setOf("chest", "pectorals", "back", "lats", "upper legs", "quads", "glutes", "hamstrings")
            val isCompound = targets.any { it.lowercase() in compoundTargets }
            return if (isCompound && profile.experience != Experience.BEGINNER) {
                SetStrategy.STEP_UP
            } else {
                config.strategy
            }
        }

        fun toPlanItems(exercises: List<ExerciseEntity>, targets: List<String>): List<PlanItem> {
            val strat = strategyFor(targets)
            return exercises.map { e ->
                PlanItem(
                    exerciseId = e.id,
                    setCount = config.sets,
                    strategy = strat,
                    targetReps = config.repLow,
                    restSeconds = config.restSeconds,
                )
            }
        }

        return when (profile.split) {
            Split.FULL_BODY -> {
                val buckets = listOf(
                    listOf("chest", "pectorals") to 1,
                    listOf("back", "lats") to 1,
                    listOf("shoulders", "delts") to 1,
                    listOf("upper legs", "quads", "glutes", "hamstrings") to 1,
                    listOf("waist", "abs") to 1,
                    listOf("upper arms", "lower arms", "biceps", "triceps", "forearms") to 1,
                )
                val items = mutableListOf<PlanItem>()
                for ((targets, count) in buckets) {
                    val picked = pick(targets, count)
                    picked.forEach { usedIds.add(it.id) }
                    items += toPlanItems(picked, targets)
                }
                listOf(PreviewTemplate(title = "Full Body — Recommended", items = items))
            }
            Split.UPPER_LOWER -> {
                // Upper: Chest 1, Back 1, Shoulders 1, Arms 2 (upper+lower arms)
                val upperBuckets = listOf(
                    listOf("chest", "pectorals") to 1,
                    listOf("back", "lats") to 1,
                    listOf("shoulders", "delts") to 1,
                    listOf("upper arms", "lower arms", "biceps", "triceps", "forearms") to 2,
                )
                // Lower: upper legs 2, lower legs 1, waist 2
                val lowerBuckets = listOf(
                    listOf("upper legs", "quads", "glutes", "hamstrings") to 2,
                    listOf("lower legs", "calves") to 1,
                    listOf("waist", "abs") to 2,
                )
                val upperItems = mutableListOf<PlanItem>()
                for ((targets, count) in upperBuckets) {
                    val picked = pick(targets, count)
                    picked.forEach { usedIds.add(it.id) }
                    upperItems += toPlanItems(picked, targets)
                }
                val lowerItems = mutableListOf<PlanItem>()
                for ((targets, count) in lowerBuckets) {
                    val picked = pick(targets, count)
                    picked.forEach { usedIds.add(it.id) }
                    lowerItems += toPlanItems(picked, targets)
                }
                listOf(
                    PreviewTemplate(title = "Upper — Recommended", items = upperItems),
                    PreviewTemplate(title = "Lower — Recommended", items = lowerItems),
                )
            }
            Split.PPL -> {
                // Push: Chest 2, Shoulders 2, Triceps 1
                val pushBuckets = listOf(
                    listOf("chest", "pectorals") to 2,
                    listOf("shoulders", "delts") to 2,
                    listOf("triceps") to 1,
                )
                // Pull: Back 2, Biceps 2, rear delts 1 (shoulders)
                val pullBuckets = listOf(
                    listOf("back", "lats") to 2,
                    listOf("biceps") to 2,
                    listOf("shoulders", "delts") to 1,
                )
                // Legs: upper legs 2, lower legs 1, waist 2
                val legsBuckets = listOf(
                    listOf("upper legs", "quads", "glutes", "hamstrings") to 2,
                    listOf("lower legs", "calves") to 1,
                    listOf("waist", "abs") to 2,
                )

                suspend fun buildBuckets(buckets: List<Pair<List<String>, Int>>): List<List<PlanItem>> {
                    return buckets.map { (targets, count) ->
                        val picked = pick(targets, count)
                        picked.forEach { usedIds.add(it.id) }
                        toPlanItems(picked, targets)
                    }
                }

                suspend fun build(title: String, buckets: List<Pair<List<String>, Int>>): PreviewTemplate {
                    return PreviewTemplate(title = title, items = buildBuckets(buckets).flatten())
                }

                suspend fun buildRepeat(
                    title: String,
                    buckets: List<Pair<List<String>, Int>>,
                    firstCycleBuckets: List<List<PlanItem>>,
                ): PreviewTemplate {
                    val items = mutableListOf<PlanItem>()
                    buckets.forEachIndexed { index, (targets, count) ->
                        val picked = pick(targets, count)
                        picked.forEach { usedIds.add(it.id) }
                        items += toPlanItems(picked, targets)
                        // Fall back to first-cycle IDs for the same bucket when unused exercises run out.
                        val shortfall = count - picked.size
                        if (shortfall > 0) {
                            items += firstCycleBuckets[index].take(shortfall)
                        }
                    }
                    return PreviewTemplate(title = title, items = items)
                }

                if (profile.days == 6) {
                    val pushABuckets = buildBuckets(pushBuckets)
                    val pullABuckets = buildBuckets(pullBuckets)
                    val legsABuckets = buildBuckets(legsBuckets)
                    listOf(
                        PreviewTemplate(title = "Push A", items = pushABuckets.flatten()),
                        PreviewTemplate(title = "Pull A", items = pullABuckets.flatten()),
                        PreviewTemplate(title = "Legs A", items = legsABuckets.flatten()),
                        buildRepeat("Push B", pushBuckets, pushABuckets),
                        buildRepeat("Pull B", pullBuckets, pullABuckets),
                        buildRepeat("Legs B", legsBuckets, legsABuckets),
                    )
                } else {
                    listOf(
                        build("Push — Recommended", pushBuckets),
                        build("Pull — Recommended", pullBuckets),
                        build("Legs — Recommended", legsBuckets),
                    )
                }
            }
        }
    }

    private suspend fun queryTargets(
        targets: List<String>,
        difficulties: List<String>,
        allowedEquipment: Set<String>,
        isFullGym: Boolean,
        limit: Int,
    ): List<ExerciseEntity> {
        return if (isFullGym) {
            exerciseDao.getByTargets(targets, difficulties, limit)
        } else {
            exerciseDao.getByTargetsFiltered(targets, allowedEquipment.toList(), difficulties, limit)
        }
    }

    private fun diffsFor(max: Difficulty): List<String> = when (max) {
        Difficulty.BEGINNER -> listOf(Difficulty.BEGINNER.name)
        Difficulty.INTERMEDIATE -> listOf(Difficulty.BEGINNER.name, Difficulty.INTERMEDIATE.name)
        Difficulty.ADVANCED -> listOf(Difficulty.BEGINNER.name, Difficulty.INTERMEDIATE.name, Difficulty.ADVANCED.name)
    }

    private fun fallbackDiffsFor(max: Difficulty): List<String> = when (max) {
        Difficulty.BEGINNER -> listOf(Difficulty.BEGINNER.name, Difficulty.INTERMEDIATE.name)
        Difficulty.INTERMEDIATE -> listOf(Difficulty.BEGINNER.name, Difficulty.INTERMEDIATE.name, Difficulty.ADVANCED.name)
        Difficulty.ADVANCED -> listOf(Difficulty.BEGINNER.name, Difficulty.INTERMEDIATE.name, Difficulty.ADVANCED.name)
    }
}
