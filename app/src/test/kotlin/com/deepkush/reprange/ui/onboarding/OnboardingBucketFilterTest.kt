package com.deepkush.reprange.ui.onboarding

import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.remote.DatasetApi
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.navigation.ExercisePickerRoute
import com.deepkush.reprange.ui.screens.library.bucketForExercise
import com.deepkush.reprange.viewmodels.LibraryViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class BucketFakeDao : ExerciseDao {
    override suspend fun count(): Int = 0
    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(null)
    override suspend fun getById(id: String): ExerciseEntity? = null
    override suspend fun getByIds(ids: List<String>): List<ExerciseEntity> = emptyList()
    override fun search(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?, limit: Int, offset: Int): Flow<List<ExerciseEntity>> = flowOf(emptyList())
    override fun countSearch(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?): Flow<Int> = flowOf(0)
    override fun observeCategories(): Flow<List<String>> = flowOf(listOf("chest", "back"))
    override fun observeEquipment(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getByTarget(target: String, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargetFiltered(target: String, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
}

private class BucketFakeRepository : ExerciseRepository(
    exerciseDao = BucketFakeDao(),
    seeder = DatasetSeeder(
        api = object : DatasetApi {
            override suspend fun exercises() = emptyList<com.deepkush.reprange.data.remote.DatasetExerciseDto>()
        },
        exerciseDao = BucketFakeDao(),
    ),
) {
    private val _state = MutableStateFlow<DatasetSeeder.SeedState>(DatasetSeeder.SeedState.Done)
    override val seedState: StateFlow<DatasetSeeder.SeedState> get() = _state
    override suspend fun seedIfNeeded() {}
}

private fun bucketExercise(id: String, category: String) = ExerciseEntity(
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

/**
 * Task 4: onboarding swap picker starts scoped to the row's muscle bucket/category,
 * while template/session callers remain unscoped by default.
 */
class OnboardingBucketFilterTest {

    @Test
    fun pickerRoute_templateAndSessionDefaultToUnscoped() {
        assertThat(ExercisePickerRoute("template").initialCategory).isNull()
        assertThat(ExercisePickerRoute("session").initialCategory).isNull()
    }

    @Test
    fun pickerRoute_preservesOnboardingBucket() {
        val route = ExercisePickerRoute("template", 0L, "chest")
        assertThat(route.initialCategory).isEqualTo("chest")
    }

    @Test
    fun bucketForExercise_resolvesRowCategory() {
        assertThat(bucketForExercise(bucketExercise("1", "chest"))).isEqualTo("chest")
        assertThat(bucketForExercise(null)).isNull()
        assertThat(bucketForExercise(bucketExercise("2", ""))).isNull()
    }

    @Test
    fun libraryViewModel_appliesBucketPrefilter() = runTest {
        val vm = LibraryViewModel(BucketFakeRepository())
        assertThat(vm.filters.value.category).isNull()
        vm.setCategory("chest")
        assertThat(vm.filters.value.category).isEqualTo("chest")
    }
}
