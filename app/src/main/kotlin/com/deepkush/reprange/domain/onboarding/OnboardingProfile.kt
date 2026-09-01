package com.deepkush.reprange.domain.onboarding

import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.data.repo.Difficulty

enum class Goal { STRENGTH, HYPERTROPHY, FAT_LOSS, GENERAL_FITNESS }

enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }

enum class EquipmentProfile {
    BODYWEIGHT,
    DUMBBELL_ONLY,
    BARBELL_DUMBBELL,
    FULL_GYM;

    fun allowedEquipment(): Set<String> = when (this) {
        BODYWEIGHT -> setOf("body weight")
        DUMBBELL_ONLY -> setOf("body weight", "dumbbell")
        BARBELL_DUMBBELL -> setOf("body weight", "dumbbell", "barbell", "ez barbell")
        FULL_GYM -> emptySet()
    }
}

enum class Split { FULL_BODY, UPPER_LOWER, PPL }

fun Split.validForDays(days: Int): Boolean = when (this) {
    Split.FULL_BODY -> days in 2..6
    Split.UPPER_LOWER -> days >= 4
    Split.PPL -> days >= 3
}

data class GoalConfig(val sets: Int, val reps: Int, val restSeconds: Int, val strategy: SetStrategy)

fun Goal.config(): GoalConfig = when (this) {
    Goal.STRENGTH -> GoalConfig(4, 5, 90, SetStrategy.STEP_UP)
    Goal.HYPERTROPHY -> GoalConfig(3, 10, 90, SetStrategy.STANDARD)
    Goal.FAT_LOSS -> GoalConfig(3, 13, 60, SetStrategy.STANDARD)
    Goal.GENERAL_FITNESS -> GoalConfig(3, 10, 90, SetStrategy.STANDARD)
}

data class OnboardingProfile(
    val goal: Goal,
    val experience: Experience,
    val equipment: EquipmentProfile,
    val days: Int,
    val split: Split,
) {
    fun allowedEquipment(): Set<String> = equipment.allowedEquipment()

    fun maxDifficulty(): Difficulty = when (experience) {
        Experience.BEGINNER -> Difficulty.BEGINNER
        Experience.INTERMEDIATE -> Difficulty.INTERMEDIATE
        Experience.ADVANCED -> Difficulty.ADVANCED
    }
}
