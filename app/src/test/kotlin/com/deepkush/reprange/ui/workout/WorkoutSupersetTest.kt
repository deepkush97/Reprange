package com.deepkush.reprange.ui.workout

import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.ui.screens.workout.SupersetGrouping
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun entry(
    id: Long,
    exerciseId: String,
    order: Int,
    group: String? = null,
    strategy: SetStrategy = SetStrategy.STANDARD,
) = SessionExerciseEntity(
    id = id,
    sessionId = 1L,
    exerciseId = exerciseId,
    orderIndex = order,
    supersetGroup = group,
    strategy = strategy.name,
)

class WorkoutSupersetTest {

    @Test
    fun isSupersetMember_requiresSuperSetStrategyAndNonBlankGroup() {
        assertThat(SupersetGrouping.isSupersetMember(entry(1, "bench", 0, "A", SetStrategy.SUPER_SET))).isTrue()
        // Same group string but STANDARD strategy is not a member.
        assertThat(SupersetGrouping.isSupersetMember(entry(2, "bench", 0, "A", SetStrategy.STANDARD))).isFalse()
        assertThat(SupersetGrouping.isSupersetMember(entry(3, "bench", 0, null, SetStrategy.SUPER_SET))).isFalse()
        assertThat(SupersetGrouping.isSupersetMember(entry(4, "bench", 0, "", SetStrategy.SUPER_SET))).isFalse()
        assertThat(SupersetGrouping.isSupersetMember(entry(5, "bench", 0, "  ", SetStrategy.SUPER_SET))).isFalse()
    }

    @Test
    fun supersetLabel_trimsGroupAndGatesOnStrategy() {
        assertThat(
            SupersetGrouping.supersetLabel(entry(1, "bench", 0, "A", SetStrategy.SUPER_SET)),
        ).isEqualTo("Superset A")
        assertThat(
            SupersetGrouping.supersetLabel(entry(2, "bench", 0, " A ", SetStrategy.SUPER_SET)),
        ).isEqualTo("Superset A")
        assertThat(
            SupersetGrouping.supersetLabel(entry(3, "bench", 0, "A", SetStrategy.STANDARD)),
        ).isNull()
        assertThat(
            SupersetGrouping.supersetLabel(entry(4, "bench", 0, null, SetStrategy.SUPER_SET)),
        ).isNull()
    }

    @Test
    fun partners_matchesTrimmedGroupAndExcludesNonSuperSetEntries() {
        val members = listOf(
            entry(1, "bench", 0, "A", SetStrategy.SUPER_SET),
            entry(2, "row", 1, " A ", SetStrategy.SUPER_SET),
            entry(3, "press", 2, "A", SetStrategy.STANDARD),
            entry(4, "squat", 3, null),
        )
        val result = SupersetGrouping.partners(members, members[0])
        assertThat(result.map { it.id }).containsExactly(2L)
    }

    @Test
    fun nextUp_alternatesWithinSupersetPair() {
        val entries = listOf(
            entry(1, "bench", 0, "A", SetStrategy.SUPER_SET),
            entry(2, "row", 1, "A", SetStrategy.SUPER_SET),
            entry(3, "squat", 2, null),
        )
        assertThat(SupersetGrouping.nextUp(entries, 1L)?.id).isEqualTo(2L)
        assertThat(SupersetGrouping.nextUp(entries, 2L)?.id).isEqualTo(1L)
    }

    @Test
    fun nextUp_cyclesForwardWithinLargerGroup() {
        val entries = listOf(
            entry(1, "a", 0, "A", SetStrategy.SUPER_SET),
            entry(2, "b", 1, "A", SetStrategy.SUPER_SET),
            entry(3, "c", 2, "A", SetStrategy.SUPER_SET),
            entry(4, "d", 3, null),
        )
        assertThat(SupersetGrouping.nextUp(entries, 1L)?.id).isEqualTo(2L)
        assertThat(SupersetGrouping.nextUp(entries, 2L)?.id).isEqualTo(3L)
        assertThat(SupersetGrouping.nextUp(entries, 3L)?.id).isEqualTo(1L)
    }

    @Test
    fun nextUp_supersetMemberLastInWorkoutReturnsNull() {
        val entries = listOf(
            entry(1, "squat", 0, null),
            entry(2, "bench", 1, "A", SetStrategy.SUPER_SET),
            entry(3, "row", 2, "A", SetStrategy.SUPER_SET),
        )
        assertThat(SupersetGrouping.nextUp(entries, 2L)?.id).isEqualTo(3L)
        assertThat(SupersetGrouping.nextUp(entries, 3L)).isNull()
    }

    @Test
    fun nextUp_loneSuperSetMemberReturnsNull() {
        val entries = listOf(
            entry(1, "bench", 0, "A", SetStrategy.SUPER_SET),
            entry(2, "squat", 1, null),
        )
        assertThat(SupersetGrouping.nextUp(entries, 1L)).isNull()
    }

    @Test
    fun nextUp_nonSupersetReturnsNextInOrderOrNullWhenLast() {
        val entries = listOf(
            entry(1, "bench", 0, "A", SetStrategy.SUPER_SET),
            entry(2, "row", 1, "A", SetStrategy.SUPER_SET),
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
