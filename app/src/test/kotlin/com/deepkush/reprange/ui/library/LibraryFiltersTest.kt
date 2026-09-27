package com.deepkush.reprange.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.remote.DatasetApi
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.ui.screens.library.EquipmentFilterRow
import com.deepkush.reprange.viewmodels.LibraryViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class FakeLibraryDao : ExerciseDao {
    override suspend fun count(): Int = 0
    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(null)
    override suspend fun getById(id: String): ExerciseEntity? = null
    override suspend fun getByIds(ids: List<String>): List<ExerciseEntity> = emptyList()
    override fun search(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?, limit: Int, offset: Int): Flow<List<ExerciseEntity>> = flowOf(emptyList())
    override fun countSearch(query: String?, fts: String?, category: String?, equipment: String?, difficulty: String?): Flow<Int> = flowOf(0)
    override fun observeCategories(): Flow<List<String>> = flowOf(emptyList())
    override fun observeEquipment(): Flow<List<String>> = flowOf(listOf("barbell", "dumbbell", "body weight"))
    override suspend fun getByTarget(target: String, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargets(targets: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargetFiltered(target: String, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargetsFiltered(targets: List<String>, equipment: List<String>, difficulties: List<String>, limit: Int): List<ExerciseEntity> = emptyList()
}

private class FakeLibraryRepository : ExerciseRepository(
    exerciseDao = FakeLibraryDao(),
    seeder = DatasetSeeder(
        api = object : DatasetApi {
            override suspend fun exercises() = emptyList<com.deepkush.reprange.data.remote.DatasetExerciseDto>()
        },
        exerciseDao = FakeLibraryDao(),
    ),
) {
    private val _state = MutableStateFlow<DatasetSeeder.SeedState>(DatasetSeeder.SeedState.Done)
    override val seedState: StateFlow<DatasetSeeder.SeedState> get() = _state
    override suspend fun seedIfNeeded() {}
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LibraryFiltersTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun viewModel_setEquipment_updatesFilters() = runTest {
        val vm = LibraryViewModel(FakeLibraryRepository())
        assertThat(vm.filters.value.equipment).isNull()
        vm.setEquipment("barbell")
        assertThat(vm.filters.value.equipment).isEqualTo("barbell")
        vm.setEquipment(null)
        assertThat(vm.filters.value.equipment).isNull()
    }

    @Test
    fun viewModel_clearFilters_clearsEquipmentButKeepsQuery() = runTest {
        val vm = LibraryViewModel(FakeLibraryRepository())
        vm.setQuery("press")
        vm.setCategory("chest")
        vm.setEquipment("dumbbell")
        vm.setDifficulty("BEGINNER")
        vm.clearFilters()
        assertThat(vm.filters.value.query).isEqualTo("press")
        assertThat(vm.filters.value.category).isNull()
        assertThat(vm.filters.value.equipment).isNull()
        assertThat(vm.filters.value.difficulty).isNull()
    }

    @Test
    fun equipmentRow_showsOptionsAndSelection() {
        composeRule.setContent {
            EquipmentFilterRow(
                options = listOf("barbell", "dumbbell"),
                selected = "barbell",
                onSelect = {},
                onClear = {},
                hasActiveFilters = true,
            )
        }
        composeRule.onNodeWithTag("equipment_filter_row").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell").assertIsDisplayed()
        composeRule.onNodeWithText("Dumbbell").assertIsDisplayed()
        composeRule.onNodeWithTag("library_clear_filters").assertIsDisplayed()
    }

    @Test
    fun equipmentRow_clickSelect_invokesCallback() {
        var selected: String? = "unSET-sentinel"
        composeRule.setContent {
            EquipmentFilterRow(
                options = listOf("barbell", "dumbbell"),
                selected = null,
                onSelect = { selected = it },
                onClear = {},
                hasActiveFilters = false,
            )
        }
        composeRule.onNodeWithTag("equipment_chip_dumbbell").performClick()
        composeRule.waitForIdle()
        assertThat(selected).isEqualTo("dumbbell")
    }

    @Test
    fun equipmentRow_hidesClearWhenNoActiveFilters() {
        composeRule.setContent {
            EquipmentFilterRow(
                options = listOf("barbell"),
                selected = null,
                onSelect = {},
                onClear = {},
                hasActiveFilters = false,
            )
        }
        composeRule.onNodeWithTag("equipment_filter_row").assertIsDisplayed()
        assertThat(
            composeRule.onAllNodesWithTag("library_clear_filters").fetchSemanticsNodes(),
        ).isEmpty()
    }
}

// Keep compose-test import referenced even if runner swaps semantics lookup.
private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onAllNodesWithTag(testTag: String) =
    onAllNodes(androidx.compose.ui.test.hasTestTag(testTag))
