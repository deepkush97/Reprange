package com.deepkush.reprange.ui.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.ui.screens.progress.ExerciseOneRmSection
import com.deepkush.reprange.ui.screens.progress.OneRmChart
import com.deepkush.reprange.viewmodels.PrWithExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

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
}

// Keep compose-test import referenced even if runner swaps semantics lookup.
private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onAllNodesWithTag(testTag: String) =
    onAllNodes(androidx.compose.ui.test.hasTestTag(testTag))
