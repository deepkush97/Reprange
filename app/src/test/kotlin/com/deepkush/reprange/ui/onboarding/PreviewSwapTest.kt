package com.deepkush.reprange.ui.onboarding

import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.remote.DatasetApi
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.domain.onboarding.EquipmentProfile
import com.deepkush.reprange.domain.onboarding.Experience
import com.deepkush.reprange.domain.onboarding.Goal
import com.deepkush.reprange.domain.onboarding.OnboardingRecommender
import com.deepkush.reprange.domain.onboarding.Split
import com.deepkush.reprange.ui.screens.onboarding.OnboardingViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakeExerciseDaoForPreview : ExerciseDao {
    override suspend fun count(): Int = 0
    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(null)
    override suspend fun getById(id: String): ExerciseEntity? = null
    override suspend fun getByIds(ids: List<String>): List<ExerciseEntity> = emptyList()
    override fun search(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?, limit: Int, offset: Int): Flow<List<ExerciseEntity>> = flowOf(emptyList())
    override fun countSearch(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?): Flow<Int> = flowOf(0)
    override fun observeCategories(): Flow<List<String>> = flowOf(emptyList())
    override fun observeEquipment(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getByTarget(target: String, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        val all = listOf(
            exerciseEntity("1", "chest"),
            exerciseEntity("2", "back"),
            exerciseEntity("3", "shoulders"),
            exerciseEntity("4", "upper legs"),
            exerciseEntity("5", "waist"),
            exerciseEntity("6", "upper arms"),
            exerciseEntity("7", "lower legs"),
            exerciseEntity("8", "chest2"),
            exerciseEntity("9", "back2"),
            exerciseEntity("10", "triceps"),
            exerciseEntity("11", "biceps"),
        )
        return all.filter { e -> e.category.equals(target, ignoreCase = true) || e.target.equals(target, ignoreCase = true) }.take(limit)
    }
    override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        val all = listOf(
            exerciseEntity("1", "chest"),
            exerciseEntity("2", "back"),
            exerciseEntity("3", "shoulders"),
            exerciseEntity("4", "upper legs"),
            exerciseEntity("5", "waist"),
            exerciseEntity("6", "upper arms"),
            exerciseEntity("7", "lower legs"),
            exerciseEntity("8", "chest2"),
            exerciseEntity("9", "back2"),
            exerciseEntity("10", "triceps"),
            exerciseEntity("11", "biceps"),
        )
        return all.filter { e -> targets.any { t -> e.category.equals(t, ignoreCase = true) || e.target.equals(t, ignoreCase = true) } }.take(limit)
    }
    override suspend fun getByTargetFiltered(target: String, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        return getByTarget(target, difficulties, limit)
    }
    override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
        return getByTargets(targets, difficulties, limit)
    }
    private fun exerciseEntity(id: String, category: String): ExerciseEntity = ExerciseEntity(
        id = id,
        name = "ex $id",
        category = category,
        target = category,
        muscleGroup = category,
        secondaryMuscles = emptyList(),
        equipment = "body weight",
        difficulty = "BEGINNER",
        instructionsEn = emptyList(),
        imageUrl = "",
        gifUrl = "",
        mediaId = "",
        attribution = "",
    )
}

private class FakeExerciseRepositoryPreview : ExerciseRepository(
    exerciseDao = FakeExerciseDaoForPreview(),
    seeder = DatasetSeeder(
        api = object : DatasetApi { override suspend fun exercises() = emptyList<com.deepkush.reprange.data.remote.DatasetExerciseDto>() },
        exerciseDao = FakeExerciseDaoForPreview(),
    ),
) {
    private val _state = MutableStateFlow(DatasetSeeder.SeedState.Done)
    override val seedState: StateFlow<DatasetSeeder.SeedState> get() = _state
    override suspend fun seedIfNeeded() { _state.value = DatasetSeeder.SeedState.Done }
}

class PreviewSwapTest {
    @Test fun preview_swap_replacesExercise() = runTest {
        val dao = FakeExerciseDaoForPreview()
        val engine = OnboardingRecommender(dao)
        val vm = OnboardingViewModel(engine, FakeExerciseRepositoryPreview())
        vm.setGoal(Goal.HYPERTROPHY)
        vm.setExperience(Experience.BEGINNER)
        vm.setEquipment(EquipmentProfile.FULL_GYM)
        vm.setDays(3)
        vm.setSplit(Split.FULL_BODY)
        vm.generatePreview()
        val before = vm.preview.value!![0].items[0].exerciseId
        // ensure before is not new-id
        assertThat(before).isNotEqualTo("new-id")
        vm.swapExercise(templateIndex = 0, itemIndex = 0, newExerciseId = "new-id")
        assertThat(vm.preview.value!![0].items[0].exerciseId).isEqualTo("new-id")
    }
}
