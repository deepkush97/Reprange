package com.deepkush.reprange.domain.onboarding

import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.domain.usecase.PlanItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakeExerciseDao(
    private val exercises: List<ExerciseEntity>,
) : ExerciseDao {
    val ids: Set<String> get() = exercises.map { it.id }.toSet()

    private fun matchesTarget(e: ExerciseEntity, target: String): Boolean {
        val t = target.lowercase()
        // Mirrors Daos.kt: LOWER(target)=LOWER(:target) OR LOWER(category)=... OR LOWER(secondary_muscles) LIKE '%'||LOWER(:target)||'%'
        return e.target.lowercase() == t || e.category.lowercase() == t || e.muscleGroup.lowercase() == t ||
            e.secondaryMuscles.any { it.lowercase().contains(t) } ||
            // Also check JSON string LIKE for completeness (secondary_muscles stored as JSON array string)
            e.secondaryMuscles.joinToString(",").lowercase().contains(t)
    }

    private fun matchesTargets(e: ExerciseEntity, targets: List<String>): Boolean {
        return targets.any { t -> matchesTarget(e, t) }
    }

    override suspend fun count(): Int = exercises.size
    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(exercises.find { it.id == id })
    override suspend fun getById(id: String): ExerciseEntity? = exercises.find { it.id == id }
    override suspend fun getByIds(ids: List<String>): List<ExerciseEntity> = exercises.filter { it.id in ids }
    override fun search(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?, limit: Int, offset: Int): Flow<List<ExerciseEntity>> = flowOf(emptyList())
    override fun countSearch(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?): Flow<Int> = flowOf(0)
    override fun observeCategories(): Flow<List<String>> = flowOf(exercises.map { it.category }.distinct())
    override fun observeEquipment(): Flow<List<String>> = flowOf(exercises.map { it.equipment }.distinct())
    override suspend fun getByTarget(target: String, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        val diffs = difficulties.map { it.uppercase() }.toSet()
        return exercises.filter { it.difficulty.uppercase() in diffs && matchesTarget(it, target) }
            .sortedBy { it.name }
            .take(limit)
    }

    override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        if (targets.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        val out = mutableListOf<ExerciseEntity>()
        val diffs = difficulties.map { it.uppercase() }.toSet()
        for (t in targets) {
            val batch = exercises.filter { it.difficulty.uppercase() in diffs && matchesTarget(it, t) }
                .sortedBy { it.name }
                .take(limit)
            for (e in batch) if (seen.add(e.id)) out.add(e)
            if (out.size >= limit) break
        }
        return out.sortedBy { it.name }.take(limit)
    }

    override suspend fun getByTargetFiltered(target: String, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        val eq = equipment.map { it.lowercase() }.toSet()
        val diffs = difficulties.map { it.uppercase() }.toSet()
        return exercises.filter { it.equipment.lowercase() in eq && it.difficulty.uppercase() in diffs && matchesTarget(it, target) }
            .sortedBy { it.name }
            .take(limit)
    }

    override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        if (targets.isEmpty()) return emptyList()
        val eq = equipment.map { it.lowercase() }.toSet()
        val diffs = difficulties.map { it.uppercase() }.toSet()
        val seen = mutableSetOf<String>()
        val out = mutableListOf<ExerciseEntity>()
        for (t in targets) {
            val batch = exercises.filter { it.equipment.lowercase() in eq && it.difficulty.uppercase() in diffs && matchesTarget(it, t) }
                .sortedBy { it.name }
                .take(limit)
            for (e in batch) if (seen.add(e.id)) out.add(e)
            if (out.size >= limit) break
        }
        return out.sortedBy { it.name }.take(limit)
    }
}

private fun exercise(
    id: String,
    name: String = "ex $id",
    category: String,
    target: String = category,
    muscleGroup: String = category,
    equipment: String = "body weight",
    difficulty: String = "BEGINNER",
): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    category = category,
    target = target,
    muscleGroup = muscleGroup,
    secondaryMuscles = emptyList(),
    equipment = equipment,
    difficulty = difficulty,
    instructionsEn = emptyList(),
    imageUrl = "",
    gifUrl = "",
    mediaId = "",
    attribution = "",
)

class OnboardingRecommenderTest {

    @Test
    fun recommend_fullBody_bodyweight_returns6Items() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        // One per bucket category for FULL_BODY (6 buckets): chest, back, shoulders, upper legs, waist, arms
        ex += exercise("1", category = "chest", equipment = "body weight")
        ex += exercise("2", category = "back", equipment = "body weight")
        ex += exercise("3", category = "shoulders", equipment = "body weight")
        ex += exercise("4", category = "upper legs", equipment = "body weight")
        ex += exercise("5", category = "waist", equipment = "body weight")
        ex += exercise("6", category = "upper arms", equipment = "body weight")
        // Add extras to test deduplication limit handling
        ex += exercise("7", category = "chest", equipment = "body weight")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.BODYWEIGHT, 3, Split.FULL_BODY)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(1)
        assertThat(result[0].items).hasSize(6)
        assertThat(result[0].items.all { it.exerciseId in fakeDao.ids }).isTrue()
    }

    @Test
    fun recommend_filtersByEquipment() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        // Bodyweight chest exercises + barbell chest
        ex += exercise("bw1", category = "chest", equipment = "body weight")
        ex += exercise("bb1", category = "chest", equipment = "barbell")
        ex += exercise("bw2", category = "back", equipment = "body weight")
        ex += exercise("bb2", category = "back", equipment = "barbell")
        ex += exercise("bw3", category = "shoulders", equipment = "body weight")
        ex += exercise("bb3", category = "shoulders", equipment = "barbell")
        ex += exercise("bw4", category = "upper legs", equipment = "body weight")
        ex += exercise("bb4", category = "upper legs", equipment = "barbell")
        ex += exercise("bw5", category = "waist", equipment = "body weight")
        ex += exercise("bb5", category = "waist", equipment = "barbell")
        ex += exercise("bw6", category = "upper arms", equipment = "body weight")
        ex += exercise("bb6", category = "upper arms", equipment = "barbell")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)

        // BODYWEIGHT profile should never return barbell
        val bwProfile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.BODYWEIGHT, 3, Split.FULL_BODY)
        val bwResult = engine.recommend(bwProfile)
        val bwIds = bwResult.flatMap { it.items }.map { it.exerciseId }
        assertThat(bwIds.none { it.startsWith("bb") }).isTrue()

        // FULL_GYM should be able to return barbell items (not filtered); we verify at least one barbell could appear when only barbell exists for a bucket
        val onlyBarbell = listOf(
            exercise("only_bb_chest", category = "chest", equipment = "barbell"),
            exercise("only_bb_back", category = "back", equipment = "barbell"),
            exercise("only_bb_shoulders", category = "shoulders", equipment = "barbell"),
            exercise("only_bb_ulegs", category = "upper legs", equipment = "barbell"),
            exercise("only_bb_waist", category = "waist", equipment = "barbell"),
            exercise("only_bb_arms", category = "upper arms", equipment = "barbell"),
        )
        val barbellDao = FakeExerciseDao(onlyBarbell)
        val engine2 = OnboardingRecommender(barbellDao)
        val gymProfile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.FULL_GYM, 3, Split.FULL_BODY)
        val gymResult = engine2.recommend(gymProfile)
        assertThat(gymResult[0].items).hasSize(6)
        assertThat(gymResult[0].items.all { it.exerciseId.startsWith("only_bb") }).isTrue()

        // BODYWEIGHT with same only-barbell dataset should return 0 or <6 (filtered out)
        val bwEmptyResult = engine2.recommend(bwProfile)
        // Since no bodyweight items, fallback still respects equipment filter, so should be empty
        assertThat(bwEmptyResult[0].items.size).isLessThan(6)
    }

    @Test
    fun recommend_upperLower_returnsTwoTemplates() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        // Provide enough for U/L (Upper 5, Lower 5)
        repeat(3) { ex += exercise("c$it", category = "chest") }
        repeat(3) { ex += exercise("b$it", category = "back") }
        repeat(3) { ex += exercise("s$it", category = "shoulders") }
        repeat(4) { ex += exercise("ua$it", category = "upper arms") }
        repeat(4) { ex += exercise("la$it", category = "lower arms") }
        repeat(4) { ex += exercise("ul$it", category = "upper legs") }
        repeat(3) { ex += exercise("ll$it", category = "lower legs") }
        repeat(4) { ex += exercise("w$it", category = "waist") }
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.HYPERTROPHY, Experience.INTERMEDIATE, EquipmentProfile.FULL_GYM, 4, Split.UPPER_LOWER)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(2)
        // Upper 5, Lower 5 = 10 total, but each template's size per spec: 5 each
        assertThat(result[0].items).hasSize(5)
        assertThat(result[1].items).hasSize(5)
    }

    @Test
    fun recommend_ppl_returnsThreeTemplates() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        // Provide enough for PPL: Push 5 (chest2, shoulders2, triceps1), Pull 5 (back2, biceps2, rear delts 1 via shoulders), Legs 5 (upper legs2, lower legs1, waist2)
        repeat(4) { ex += exercise("ch$it", category = "chest") }
        repeat(4) { ex += exercise("sh$it", category = "shoulders") }
        repeat(3) { ex += exercise("tri$it", category = "upper arms", target = "triceps", muscleGroup = "triceps") }
        repeat(4) { ex += exercise("bk$it", category = "back") }
        repeat(3) { ex += exercise("bi$it", category = "upper arms", target = "biceps", muscleGroup = "biceps") }
        repeat(4) { ex += exercise("ul$it", category = "upper legs") }
        repeat(3) { ex += exercise("ll$it", category = "lower legs") }
        repeat(4) { ex += exercise("wa$it", category = "waist") }
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.STRENGTH, Experience.ADVANCED, EquipmentProfile.FULL_GYM, 3, Split.PPL)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(3)
        assertThat(result[0].items).hasSize(5)
        assertThat(result[1].items).hasSize(5)
        assertThat(result[2].items).hasSize(5)
    }

    @Test
    fun recommend_ppl_sixDays_returnsTwoCyclesWithABTitles() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        // Enough for two full PPL cycles (30 items): Push x2 (chest 4, shoulders 4, triceps 2),
        // Pull x2 (back 4, biceps 4, shoulders 2 more -> 6 shoulders total), Legs x2 (upper legs 4, lower legs 2, waist 4)
        repeat(4) { ex += exercise("ch$it", category = "chest") }
        repeat(6) { ex += exercise("sh$it", category = "shoulders") }
        repeat(2) { ex += exercise("tri$it", category = "upper arms", target = "triceps", muscleGroup = "triceps") }
        repeat(4) { ex += exercise("bk$it", category = "back") }
        repeat(4) { ex += exercise("bi$it", category = "upper arms", target = "biceps", muscleGroup = "biceps") }
        repeat(4) { ex += exercise("ul$it", category = "upper legs") }
        repeat(2) { ex += exercise("ll$it", category = "lower legs") }
        repeat(4) { ex += exercise("wa$it", category = "waist") }
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.STRENGTH, Experience.ADVANCED, EquipmentProfile.FULL_GYM, 6, Split.PPL)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(6)
        assertThat(result.map { it.title }).containsExactly(
            "Push A", "Pull A", "Legs A", "Push B", "Pull B", "Legs B",
        ).inOrder()
        result.forEach { assertThat(it.items).hasSize(5) }
        // First cycle uses unused exercises when available: no ID reused across the two cycles
        val allIds = result.flatMap { it.items }.map { it.exerciseId }
        assertThat(allIds.size).isEqualTo(allIds.toSet().size)
        // Same goal config everywhere
        val config = Goal.STRENGTH.config()
        assertThat(result.flatMap { it.items }.all { it.setCount == config.sets }).isTrue()
        assertThat(result.flatMap { it.items }.all { it.targetReps == config.repLow }).isTrue()
        assertThat(result.flatMap { it.items }.all { it.restSeconds == config.restSeconds }).isTrue()
    }

    @Test
    fun recommend_ppl_sixDays_fallsBackToFirstCycleIds_whenPoolExhausted() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        // Only enough for a single PPL cycle (15 items)
        repeat(2) { ex += exercise("ch$it", category = "chest") }
        repeat(3) { ex += exercise("sh$it", category = "shoulders") }
        ex += exercise("tri0", category = "upper arms", target = "triceps", muscleGroup = "triceps")
        repeat(2) { ex += exercise("bk$it", category = "back") }
        repeat(2) { ex += exercise("bi$it", category = "upper arms", target = "biceps", muscleGroup = "biceps") }
        repeat(2) { ex += exercise("ul$it", category = "upper legs") }
        ex += exercise("ll0", category = "lower legs")
        repeat(2) { ex += exercise("wa$it", category = "waist") }
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.STRENGTH, Experience.ADVANCED, EquipmentProfile.FULL_GYM, 6, Split.PPL)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(6)
        assertThat(result.map { it.title }).containsExactly(
            "Push A", "Pull A", "Legs A", "Push B", "Pull B", "Legs B",
        ).inOrder()
        result.forEach { assertThat(it.items).hasSize(5) }
        // Second cycle falls back to first-cycle IDs with the same goal config
        assertThat(result[3].items).isEqualTo(result[0].items)
        assertThat(result[4].items).isEqualTo(result[1].items)
        assertThat(result[5].items).isEqualTo(result[2].items)
    }

    @Test
    fun recommend_deduplicatesAcrossBuckets() = runTest {
        // Single exercise that could match multiple buckets (e.g., chest appears only once) should not be reused
        val singleChest = exercise("only1", category = "chest")
        val back = exercise("back1", category = "back")
        val shoulders = exercise("sh1", category = "shoulders")
        val ulegs = exercise("ul1", category = "upper legs")
        val waist = exercise("wa1", category = "waist")
        val arms = exercise("arm1", category = "upper arms")
        // For PPL push needs 2 chest but only 1 available -> second should be missing or fallback, not duplicate same id
        val fakeDao = FakeExerciseDao(listOf(singleChest, back, shoulders, ulegs, waist, arms))
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.BODYWEIGHT, 3, Split.PPL)
        val result = engine.recommend(profile)
        val allIds = result.flatMap { it.items }.map { it.exerciseId }
        assertThat(allIds.size).isEqualTo(allIds.toSet().size) // no duplicates
    }

    @Test
    fun recommend_respectsGoalConfig_setsAndReps() = runTest {
        val ex = mutableListOf<ExerciseEntity>()
        ex += exercise("1", category = "chest")
        ex += exercise("2", category = "back")
        ex += exercise("3", category = "shoulders")
        ex += exercise("4", category = "upper legs")
        ex += exercise("5", category = "waist")
        ex += exercise("6", category = "upper arms")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.STRENGTH, Experience.BEGINNER, EquipmentProfile.FULL_GYM, 3, Split.FULL_BODY)
        val result = engine.recommend(profile)
        val config = Goal.STRENGTH.config()
        assertThat(result[0].items.all { it.setCount == config.sets }).isTrue()
        assertThat(result[0].items.all { it.restSeconds == config.restSeconds }).isTrue()
        // targetReps should be repLow for strength
        assertThat(result[0].items.all { it.targetReps == config.repLow }).isTrue()
    }

    @Test
    fun recommend_fallsBackOneTier_whenEmpty() = runTest {
        // Only INTERMEDIATE exercises available, BEGINNER profile should fallback to INTERMEDIATE
        val ex = mutableListOf<ExerciseEntity>()
        ex += exercise("1", category = "chest", difficulty = "INTERMEDIATE")
        ex += exercise("2", category = "back", difficulty = "INTERMEDIATE")
        ex += exercise("3", category = "shoulders", difficulty = "INTERMEDIATE")
        ex += exercise("4", category = "upper legs", difficulty = "INTERMEDIATE")
        ex += exercise("5", category = "waist", difficulty = "INTERMEDIATE")
        ex += exercise("6", category = "upper arms", difficulty = "INTERMEDIATE")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.FULL_GYM, 3, Split.FULL_BODY)
        val result = engine.recommend(profile)
        // With fallback, should still return 6 items despite no BEGINNER items
        assertThat(result[0].items).hasSize(6)
    }

    @Test
    fun recommend_emptyBucket_fallsBackOneTier() = runTest {
        // BEGINNER bodyweight profile: waist bucket empty for BEGINNER, only INTERMEDIATE exists
        // Engine should relax one tier and return waist item via fallback
        val ex = mutableListOf<ExerciseEntity>()
        ex += exercise("c1", category = "chest", equipment = "body weight", difficulty = "BEGINNER")
        ex += exercise("b1", category = "back", equipment = "body weight", difficulty = "BEGINNER")
        ex += exercise("s1", category = "shoulders", equipment = "body weight", difficulty = "BEGINNER")
        ex += exercise("ul1", category = "upper legs", equipment = "body weight", difficulty = "BEGINNER")
        // waist only INTERMEDIATE — BEGINNER query returns empty, fallback should find it
        ex += exercise("w1", category = "waist", equipment = "body weight", difficulty = "INTERMEDIATE")
        ex += exercise("a1", category = "upper arms", equipment = "body weight", difficulty = "BEGINNER")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.BODYWEIGHT, 3, Split.FULL_BODY)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(1)
        assertThat(result[0].items).hasSize(6)
        assertThat(result[0].items.map { it.exerciseId }).contains("w1")
    }

    @Test
    fun recommend_emptyBucket_stillEmpty_showsShortList() = runTest {
        // No waist exercise at all even after fallback → short list (5 instead of 6)
        val ex = mutableListOf<ExerciseEntity>()
        ex += exercise("c1", category = "chest", equipment = "body weight", difficulty = "BEGINNER")
        ex += exercise("b1", category = "back", equipment = "body weight", difficulty = "BEGINNER")
        ex += exercise("s1", category = "shoulders", equipment = "body weight", difficulty = "BEGINNER")
        ex += exercise("ul1", category = "upper legs", equipment = "body weight", difficulty = "BEGINNER")
        // waist missing entirely
        ex += exercise("a1", category = "upper arms", equipment = "body weight", difficulty = "BEGINNER")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.BODYWEIGHT, 3, Split.FULL_BODY)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(1)
        // Should be 5, not 6, and no crash — UI will show placeholder for missing bucket
        assertThat(result[0].items).hasSize(5)
    }

    @Test
    fun recommend_matchesSecondaryMuscles() = runTest {
        // Exercise that only matches via secondaryMuscles (e.g., triceps via secondary)
        // Bucket for triceps (PPL Push triceps) should find it even if target/category are different
        val ex = mutableListOf<ExerciseEntity>()
        ex += exercise("ch1", category = "chest", difficulty = "BEGINNER")
        ex += exercise("ch2", category = "chest", difficulty = "BEGINNER")
        ex += exercise("sh1", category = "shoulders", difficulty = "BEGINNER")
        ex += exercise("sh2", category = "shoulders", difficulty = "BEGINNER")
        // triceps only via secondaryMuscles, not primary target
        ex += ExerciseEntity(
            id = "tri_sec",
            name = "ex tri_sec",
            category = "upper arms",
            target = "upper arms",
            muscleGroup = "upper arms",
            secondaryMuscles = listOf("triceps"),
            equipment = "body weight",
            difficulty = "BEGINNER",
            instructionsEn = emptyList(),
            imageUrl = "",
            gifUrl = "",
            mediaId = "",
            attribution = "",
        )
        // Fill remaining for Pull/Legs to make DAO not empty
        ex += exercise("bk1", category = "back", difficulty = "BEGINNER")
        ex += exercise("bk2", category = "back", difficulty = "BEGINNER")
        ex += exercise("bi1", category = "upper arms", target = "biceps", muscleGroup = "biceps", difficulty = "BEGINNER")
        ex += exercise("bi2", category = "upper arms", target = "biceps", muscleGroup = "biceps", difficulty = "BEGINNER")
        ex += exercise("ul1", category = "upper legs", difficulty = "BEGINNER")
        ex += exercise("ul2", category = "upper legs", difficulty = "BEGINNER")
        ex += exercise("ll1", category = "lower legs", difficulty = "BEGINNER")
        ex += exercise("wa1", category = "waist", difficulty = "BEGINNER")
        ex += exercise("wa2", category = "waist", difficulty = "BEGINNER")
        val fakeDao = FakeExerciseDao(ex)
        val engine = OnboardingRecommender(fakeDao)
        val profile = OnboardingProfile(Goal.STRENGTH, Experience.BEGINNER, EquipmentProfile.FULL_GYM, 3, Split.PPL)
        val result = engine.recommend(profile)
        assertThat(result).hasSize(3)
        // Push should have 5 items, including tri_sec via secondaryMuscles fallback
        val pushIds = result[0].items.map { it.exerciseId }
        assertThat(pushIds).contains("tri_sec")
    }
}
