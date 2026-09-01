package com.deepkush.reprange.ui.onboarding

import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.domain.onboarding.EquipmentProfile
import com.deepkush.reprange.domain.onboarding.Experience
import com.deepkush.reprange.domain.onboarding.Goal
import com.deepkush.reprange.domain.onboarding.OnboardingRecommender
import com.deepkush.reprange.domain.onboarding.Split
import com.deepkush.reprange.domain.onboarding.validForDays
import com.deepkush.reprange.ui.screens.onboarding.OnboardingViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakeExerciseDao : ExerciseDao {
    override suspend fun count(): Int = 0
    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(null)
    override suspend fun getById(id: String): ExerciseEntity? = null
    override suspend fun getByIds(ids: List<String>): List<ExerciseEntity> = emptyList()
    override fun search(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?, limit: Int, offset: Int): Flow<List<ExerciseEntity>> = flowOf(emptyList())
    override fun countSearch(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?): Flow<Int> = flowOf(0)
    override fun observeCategories(): Flow<List<String>> = flowOf(emptyList())
    override fun observeEquipment(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
}

class OnboardingViewModelTest {

    private fun createVm(): OnboardingViewModel {
        val dao = FakeExerciseDao()
        return OnboardingViewModel(dao)
    }

    @Test
    fun viewModel_stepper_days_clamps2to6() = runTest {
        val vm = createVm()
        vm.setDays(7)
        assertThat(vm.profile.value.days).isEqualTo(6)
        vm.setDays(1)
        assertThat(vm.profile.value.days).isEqualTo(2)
    }

    @Test
    fun viewModel_setDays_clampsUpperAndLower() = runTest {
        val vm = createVm()
        vm.setDays(10)
        assertThat(vm.profile.value.days).isEqualTo(6)
        vm.setDays(0)
        assertThat(vm.profile.value.days).isEqualTo(2)
        vm.setDays(4)
        assertThat(vm.profile.value.days).isEqualTo(4)
    }

    @Test
    fun viewModel_setGoal_updatesProfile() = runTest {
        val vm = createVm()
        vm.setGoal(Goal.STRENGTH)
        assertThat(vm.profile.value.goal).isEqualTo(Goal.STRENGTH)
        vm.setGoal(Goal.FAT_LOSS)
        assertThat(vm.profile.value.goal).isEqualTo(Goal.FAT_LOSS)
    }

    @Test
    fun viewModel_setExperience_updatesProfile() = runTest {
        val vm = createVm()
        vm.setExperience(Experience.ADVANCED)
        assertThat(vm.profile.value.experience).isEqualTo(Experience.ADVANCED)
    }

    @Test
    fun viewModel_setEquipment_updatesProfile() = runTest {
        val vm = createVm()
        vm.setEquipment(EquipmentProfile.BODYWEIGHT)
        assertThat(vm.profile.value.equipment).isEqualTo(EquipmentProfile.BODYWEIGHT)
    }

    @Test
    fun viewModel_setSplit_updatesProfile() = runTest {
        val vm = createVm()
        vm.setDays(6)
        vm.setSplit(Split.PPL)
        assertThat(vm.profile.value.split).isEqualTo(Split.PPL)
    }

    @Test
    fun viewModel_split_filteredByDays_autoCorrects() = runTest {
        val vm = createVm()
        vm.setDays(4)
        vm.setSplit(Split.UPPER_LOWER)
        assertThat(vm.profile.value.split).isEqualTo(Split.UPPER_LOWER)
        // Changing to 3 days should auto-correct UPPER_LOWER (requires >=4) to first valid e.g. FULL_BODY
        vm.setDays(3)
        assertThat(vm.profile.value.split.validForDays(3)).isTrue()
        assertThat(vm.profile.value.days).isEqualTo(3)
    }

    @Test
    fun viewModel_currentStep_navigation() = runTest {
        val vm = createVm()
        assertThat(vm.currentStep.value).isEqualTo(0)
        vm.nextStep()
        assertThat(vm.currentStep.value).isEqualTo(1)
        vm.nextStep()
        vm.nextStep()
        vm.nextStep()
        assertThat(vm.currentStep.value).isEqualTo(4)
        vm.nextStep()
        assertThat(vm.currentStep.value).isEqualTo(4) // clamp max 4
        vm.prevStep()
        assertThat(vm.currentStep.value).isEqualTo(3)
        vm.setStep(0)
        assertThat(vm.currentStep.value).isEqualTo(0)
        vm.setStep(10)
        assertThat(vm.currentStep.value).isEqualTo(4)
        vm.setStep(-1)
        assertThat(vm.currentStep.value).isEqualTo(0)
    }

    @Test
    fun viewModel_generatePreview_setsPreview() = runTest {
        // Use a fake dao that returns some data
        val dao = object : ExerciseDao by FakeExerciseDao() {
            override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
                // return minimal exercise per target to satisfy buckets
                return listOf(
                    exerciseEntity("1", "chest"),
                    exerciseEntity("2", "back"),
                    exerciseEntity("3", "shoulders"),
                    exerciseEntity("4", "upper legs"),
                    exerciseEntity("5", "waist"),
                    exerciseEntity("6", "upper arms"),
                ).filter { e -> targets.any { t -> e.category == t || e.target == t } }.take(limit)
            }
            override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
                return getByTargets(targets, difficulties, limit)
            }
        }
        // For FULL_GYM profile, recommender will query getByTargets
        val vm = OnboardingViewModel(dao)
        vm.setDays(3)
        // ensure split is FULL_BODY
        vm.setSplit(Split.FULL_BODY)
        vm.generatePreview()
        assertThat(vm.preview.value).isNotNull()
        // FULL_BODY should produce 1 template
        assertThat(vm.preview.value!!.size).isEqualTo(1)
    }

    @Test
    fun viewModel_swapExercise_replacesId() = runTest {
        val dao = object : ExerciseDao by FakeExerciseDao() {
            override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
                return listOf(
                    exerciseEntity("1", "chest"),
                    exerciseEntity("2", "back"),
                    exerciseEntity("3", "shoulders"),
                    exerciseEntity("4", "upper legs"),
                    exerciseEntity("5", "waist"),
                    exerciseEntity("6", "upper arms"),
                ).filter { e -> targets.any { t -> e.category == t || e.target == t } }.take(limit)
            }
            override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> {
                return getByTargets(targets, difficulties, limit)
            }
        }
        val vm = OnboardingViewModel(dao)
        vm.generatePreview()
        val before = vm.preview.value?.firstOrNull()?.items?.firstOrNull()?.exerciseId
        assertThat(before).isNotNull()
        vm.swapExercise(0, 0, "new-id")
        assertThat(vm.preview.value!![0].items[0].exerciseId).isEqualTo("new-id")
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
