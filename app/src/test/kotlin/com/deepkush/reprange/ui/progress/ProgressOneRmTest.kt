package com.deepkush.reprange.ui.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.data.db.CompletedSetEntity
import com.deepkush.reprange.data.db.DatedVolumePoint
import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.db.PrRow
import com.deepkush.reprange.data.db.RawSetPoint
import com.deepkush.reprange.data.db.SessionDao
import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.data.db.SessionSummary
import com.deepkush.reprange.data.db.SessionWithExercises
import com.deepkush.reprange.data.db.WorkoutSessionEntity
import com.deepkush.reprange.data.remote.DatasetApi
import com.deepkush.reprange.data.remote.DatasetExerciseDto
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.data.repo.SessionRepository
import com.deepkush.reprange.domain.usecase.CalculateProgressUseCase
import com.deepkush.reprange.viewmodels.ProgressViewModel
import com.deepkush.reprange.ui.screens.progress.ExerciseOneRmSection
import com.deepkush.reprange.ui.screens.progress.OneRmChart
import com.deepkush.reprange.viewmodels.PrWithExercise
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs

private fun pr(id: String, name: String, est1Rm: Double = 100.0) =
    PrWithExercise(
        exerciseId = id,
        name = name,
        bestWeightKg = 80.0,
        bestReps = 5,
        bestEst1Rm = est1Rm,
    )

private const val DAY = 24L * 3600_000L

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProgressOneRmTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultSelection_returnsTopPrExercise() {
        val prs = listOf(pr("bench", "Bench press", 120.0), pr("squat", "Squat", 100.0))
        assertThat(OneRmChart.defaultSelection(prs)).isEqualTo("bench")
    }

    @Test
    fun defaultSelection_empty_returnsNull() {
        assertThat(OneRmChart.defaultSelection(emptyList())).isNull()
    }

    @Test
    fun normalized_empty_returnsEmpty() {
        assertThat(OneRmChart.normalized(emptyList())).isEmpty()
    }

    @Test
    fun normalized_flatSeries_returnsFullHeight() {
        assertThat(OneRmChart.normalized(listOf(100.0, 100.0))).containsExactly(1f, 1f)
    }

    @Test
    fun normalized_ascending_mapsMinToFloorMaxToOne() {
        val out = OneRmChart.normalized(listOf(80.0, 90.0, 100.0))
        assertThat(out).hasSize(3)
        assertThat(out[0]).isWithin(0.001f).of(0.15f)
        assertThat(out[2]).isWithin(0.001f).of(1f)
        assertThat(out[0]).isLessThan(out[1])
        assertThat(out[1]).isLessThan(out[2])
    }

    @Test
    fun section_rendersChipsAndChart() {
        composeRule.setContent {
            ExerciseOneRmSection(
                prs = listOf(pr("bench", "Bench press"), pr("squat", "Squat")),
                selectedId = "bench",
                series = listOf(0L to 90.0, DAY to 100.0),
                unit = WeightUnit.KG,
                onSelect = {},
            )
        }
        composeRule.onNodeWithTag("one_rm_exercise_row").assertIsDisplayed()
        composeRule.onNodeWithText("Bench press").assertIsDisplayed()
        composeRule.onNodeWithText("Squat").assertIsDisplayed()
        composeRule.onNodeWithTag("one_rm_chart").assertIsDisplayed()
    }

    @Test
    fun section_chipClick_invokesOnSelect() {
        var selected: String? = null
        composeRule.setContent {
            ExerciseOneRmSection(
                prs = listOf(pr("bench", "Bench press"), pr("squat", "Squat")),
                selectedId = "bench",
                series = listOf(0L to 90.0),
                unit = WeightUnit.KG,
                onSelect = { selected = it },
            )
        }
        composeRule.onNodeWithTag("one_rm_chip_squat").performClick()
        composeRule.waitForIdle()
        assertThat(selected).isEqualTo("squat")
    }

    @Test
    fun section_emptySeries_showsEmptyText() {
        composeRule.setContent {
            ExerciseOneRmSection(
                prs = listOf(pr("bench", "Bench press")),
                selectedId = "bench",
                series = emptyList(),
                unit = WeightUnit.KG,
                onSelect = {},
            )
        }
        composeRule.onNodeWithTag("one_rm_empty").assertIsDisplayed()
        assertThat(
            composeRule.onAllNodesWithTag("one_rm_chart").fetchSemanticsNodes(),
        ).isEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModel_selectExercise_loadsSeriesWith90DayWindow() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val now = System.currentTimeMillis()
            val sessionDao = ProgressFakeSessionDao(
                points = { listOf(RawSetPoint(weightKg = 100.0, reps = 5, ts = now - DAY)) },
            )
            val vm = progressViewModel(ProgressFakeExerciseDao(), sessionDao)
            advanceUntilIdle()

            vm.selectExercise("squat")
            advanceUntilIdle()

            assertThat(vm.selectedExerciseId.value).isEqualTo("squat")
            assertThat(vm.oneRmSeries.value).isNotEmpty()
            assertThat(ProgressViewModel.ONE_RM_HISTORY_MILLIS).isEqualTo(90L * 24L * 3600_000L)
            val since = sessionDao.capturedSince["squat"]
            assertThat(since).isNotNull()
            val expected = System.currentTimeMillis() - ProgressViewModel.ONE_RM_HISTORY_MILLIS
            assertThat(abs(since!! - expected)).isAtMost(120_000L)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModel_autoSelectsTopPrOnFirstEmission() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val sessionDao = ProgressFakeSessionDao(
                initialPrs = listOf(
                    PrRow("squat", 80.0, 5, 100.0, 1L),
                    PrRow("bench", 100.0, 5, 120.0, 2L),
                ),
            )
            val vm = progressViewModel(ProgressFakeExerciseDao(), sessionDao)
            advanceUntilIdle()

            assertThat(vm.personalRecords.value.map { it.exerciseId })
                .containsExactly("bench", "squat")
            assertThat(vm.selectedExerciseId.value).isEqualTo("bench")
            assertThat(sessionDao.capturedSince.keys).contains("bench")
            assertThat(vm.oneRmSeries.value).isNotEmpty()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModel_explicitSelectionSurvivesPrReemission() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val sessionDao = ProgressFakeSessionDao(
                initialPrs = listOf(
                    PrRow("squat", 80.0, 5, 100.0, 1L),
                    PrRow("bench", 100.0, 5, 120.0, 2L),
                ),
            )
            val vm = progressViewModel(ProgressFakeExerciseDao(), sessionDao)
            advanceUntilIdle()
            assertThat(vm.selectedExerciseId.value).isEqualTo("bench")

            vm.selectExercise("squat")
            advanceUntilIdle()
            assertThat(vm.selectedExerciseId.value).isEqualTo("squat")

            // New sets land: a stronger PR arrives without any re-select.
            sessionDao.prFlow.value = listOf(
                PrRow("squat", 80.0, 5, 100.0, 1L),
                PrRow("bench", 100.0, 5, 120.0, 2L),
                PrRow("deadlift", 120.0, 5, 140.0, 3L),
            )
            advanceUntilIdle()

            assertThat(vm.personalRecords.value.map { it.exerciseId })
                .containsExactly("deadlift", "bench", "squat")
            assertThat(vm.selectedExerciseId.value).isEqualTo("squat")
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModel_deletedExerciseSelectionIsPreserved() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val sessionDao = ProgressFakeSessionDao(
                initialPrs = listOf(
                    PrRow("squat", 80.0, 5, 100.0, 1L),
                    PrRow("bench", 100.0, 5, 120.0, 2L),
                ),
            )
            val vm = progressViewModel(ProgressFakeExerciseDao(), sessionDao)
            advanceUntilIdle()

            vm.selectExercise("squat")
            advanceUntilIdle()

            // The selected exercise disappears from the PR table (e.g. sets deleted).
            sessionDao.prFlow.value = listOf(
                PrRow("bench", 100.0, 5, 120.0, 2L),
            )
            advanceUntilIdle()

            assertThat(vm.selectedExerciseId.value).isEqualTo("squat")
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModel_emptyPrs_noAutoSelect() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val sessionDao = ProgressFakeSessionDao()
            val vm = progressViewModel(ProgressFakeExerciseDao(), sessionDao)
            advanceUntilIdle()

            assertThat(vm.personalRecords.value).isEmpty()
            assertThat(vm.selectedExerciseId.value).isNull()
            assertThat(vm.oneRmSeries.value).isEmpty()
            assertThat(sessionDao.capturedSince).isEmpty()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

// Keep compose-test import referenced even if runner swaps semantics lookup.
private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onAllNodesWithTag(testTag: String) =
    onAllNodes(androidx.compose.ui.test.hasTestTag(testTag))

private fun progressExercise(id: String, name: String) = ExerciseEntity(
    id = id,
    name = name,
    category = "chest",
    target = "chest",
    muscleGroup = "chest",
    secondaryMuscles = emptyList(),
    equipment = "barbell",
    difficulty = "BEGINNER",
    instructionsEn = emptyList(),
    imageUrl = "",
    gifUrl = "",
    mediaId = "",
    attribution = "",
)

private class ProgressFakeExerciseDao(
    private val names: Map<String, String> = mapOf(
        "bench" to "Bench press",
        "squat" to "Squat",
        "deadlift" to "Deadlift",
    ),
) : ExerciseDao {
    override suspend fun count(): Int = 0
    override suspend fun insertAll(exercises: List<ExerciseEntity>) {}
    override fun observeById(id: String): Flow<ExerciseEntity?> = flowOf(null)
    override suspend fun getById(id: String): ExerciseEntity? =
        names[id]?.let { progressExercise(id, it) }
    override suspend fun getByIds(ids: List<String>): List<ExerciseEntity> =
        ids.mapNotNull { id -> names[id]?.let { progressExercise(id, it) } }
    override fun search(
        query: String?,
        fts: String?,
        category: String?,
        equipment: String?,
        difficulty: String?,
        limit: Int,
        offset: Int,
    ): Flow<List<ExerciseEntity>> = flowOf(emptyList())
    override fun countSearch(
        query: String?,
        fts: String?,
        category: String?,
        equipment: String?,
        difficulty: String?,
    ): Flow<Int> = flowOf(0)
    override fun observeCategories(): Flow<List<String>> = flowOf(emptyList())
    override fun observeEquipment(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getByTarget(
        target: String,
        difficulties: List<String>,
        limit: Int,
    ): List<ExerciseEntity> = emptyList()
    override suspend fun getByTargetFiltered(
        target: String,
        equipment: List<String>,
        difficulties: List<String>,
        limit: Int,
    ): List<ExerciseEntity> = emptyList()
}

private class ProgressFakeSessionDao(
    initialPrs: List<PrRow> = emptyList(),
    var points: (String) -> List<RawSetPoint> = {
        listOf(RawSetPoint(weightKg = 100.0, reps = 5, ts = System.currentTimeMillis() - DAY))
    },
) : SessionDao {
    val prFlow = MutableStateFlow(initialPrs)
    val capturedSince = mutableMapOf<String, Long>()

    override suspend fun insertSession(session: WorkoutSessionEntity): Long = 0
    override suspend fun updateSession(session: WorkoutSessionEntity) {}
    override suspend fun deleteSession(id: Long) {}
    override suspend fun insertEntry(entry: SessionExerciseEntity): Long = 0
    override suspend fun insertEntries(entries: List<SessionExerciseEntity>) {}
    override suspend fun deleteEntry(entry: SessionExerciseEntity) {}
    override suspend fun insertSet(set: CompletedSetEntity): Long = 0
    override suspend fun deleteSet(set: CompletedSetEntity) {}
    override fun observeActive(): Flow<SessionWithExercises?> = flowOf(null)
    override suspend fun getActive(): SessionWithExercises? = null
    override fun observeById(id: Long): Flow<SessionWithExercises?> = flowOf(null)
    override fun observeRecentSessions(limit: Int): Flow<List<SessionSummary>> = flowOf(emptyList())
    override suspend fun previousSets(
        exerciseId: String,
        beforeMillis: Long,
        limit: Int,
    ): List<CompletedSetEntity> = emptyList()
    override suspend fun normalSetsSince(exerciseId: String, since: Long): List<RawSetPoint> {
        capturedSince[exerciseId] = since
        return points(exerciseId)
    }
    override suspend fun allNormalSetsSince(since: Long): List<DatedVolumePoint> = emptyList()
    override fun observePrTable(): Flow<List<PrRow>> = prFlow
    override fun observeVolumeBetween(from: Long, to: Long): Flow<Double> = flowOf(0.0)
    override fun observeSetCountBetween(from: Long, to: Long): Flow<Int> = flowOf(0)
}

private fun progressViewModel(
    exerciseDao: ExerciseDao,
    sessionDao: SessionDao,
): ProgressViewModel {
    val seeder = DatasetSeeder(
        api = object : DatasetApi {
            override suspend fun exercises() = emptyList<DatasetExerciseDto>()
        },
        exerciseDao = exerciseDao,
    )
    return ProgressViewModel(
        ExerciseRepository(exerciseDao = exerciseDao, seeder = seeder),
        CalculateProgressUseCase(SessionRepository(sessionDao)),
    )
}
