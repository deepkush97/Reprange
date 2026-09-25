package com.deepkush.reprange.ui.workout

import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.ui.screens.workout.SupersetGrouping
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun entry(id: Long, exerciseId: String, order: Int, group: String? = null) =
    SessionExerciseEntity(
        id = id,
        sessionId = 1L,
        exerciseId = exerciseId,
        orderIndex = order,
        supersetGroup = group,
    )

class WorkoutSupersetTest {

    @Test
    fun isSupersetMember_trueOnlyWhenGroupPresent() {
        assertThat(SupersetGrouping.isSupersetMember("A")).isTrue()
        assertThat(SupersetGrouping.isSupersetMember(null)).isFalse()
        assertThat(SupersetGrouping.isSupersetMember("")).isFalse()
        assertThat(SupersetGrouping.isSupersetMember("  ")).isFalse()
    }

    @Test
    fun supersetLabel_formatsGroup() {
        assertThat(SupersetGrouping.supersetLabel("A")).isEqualTo("Superset A")
        assertThat(SupersetGrouping.supersetLabel(null)).isNull()
        assertThat(SupersetGrouping.supersetLabel("")).isNull()
    }

    @Test
    fun partners_returnsSameGroupMembersExcludingCurrentInOrder() {
        val entries = listOf(
            entry(1, "bench", 0, "A"),
            entry(2, "row", 1, "A"),
            entry(3, "squat", 2, null),
        )
        val result = SupersetGrouping.partners(entries, entries[0])
        assertThat(result.map { it.id }).containsExactly(2L)
    }

    @Test
    fun nextUp_alternatesWithinSupersetPair() {
        val entries = listOf(
            entry(1, "bench", 0, "A"),
            entry(2, "row", 1, "A"),
            entry(3, "squat", 2, null),
        )
        assertThat(SupersetGrouping.nextUp(entries, 1L)?.id).isEqualTo(2L)
        assertThat(SupersetGrouping.nextUp(entries, 2L)?.id).isEqualTo(1L)
    }

    @Test
    fun nextUp_cyclesForwardWithinLargerGroup() {
        val entries = listOf(
            entry(1, "a", 0, "A"),
            entry(2, "b", 1, "A"),
            entry(3, "c", 2, "A"),
        )
        assertThat(SupersetGrouping.nextUp(entries, 1L)?.id).isEqualTo(2L)
        assertThat(SupersetGrouping.nextUp(entries, 2L)?.id).isEqualTo(3L)
        assertThat(SupersetGrouping.nextUp(entries, 3L)?.id).isEqualTo(1L)
    }

    @Test
    fun nextUp_nonSupersetReturnsNextInOrderOrNullWhenLast() {
        val entries = listOf(
            entry(1, "bench", 0, "A"),
            entry(2, "row", 1, "A"),
            entry(3, "squat", 2, null),
            entry(4, "curl", 3, null),
        )
        assertThat(SupersetGrouping.nextUp(entries, 3L)?.id).isEqualTo(4L)
        assertThat(SupersetGrouping.nextUp(entries, 4L)).isNull()
    }

    @Test
    fun nextUp_unknownIdReturnsNull() {
        val entries = listOf(entry(1, "bench", 0, null))
        assertThat(SupersetGrouping.nextUp(entries, 999L)).isNull()
    }
}
