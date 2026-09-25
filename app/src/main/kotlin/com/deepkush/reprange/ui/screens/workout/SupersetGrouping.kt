package com.deepkush.reprange.ui.screens.workout

import com.deepkush.reprange.data.db.SessionExerciseEntity

/**
 * Pure superset grouping/sequence logic for the active workout.
 *
 * Groups session entries by [SessionExerciseEntity.supersetGroup] (ordered by
 * [SessionExerciseEntity.orderIndex]) and computes the alternating
 * next-exercise recommendation ("Next Up"):
 * - superset member → next partner in the same group, wrapping around
 *   (pair A→B→A alternates; larger groups cycle forward);
 * - standalone entry → next entry in workout order, or null when last.
 */
object SupersetGrouping {

    fun isSupersetMember(supersetGroup: String?): Boolean = !supersetGroup.isNullOrBlank()

    fun supersetLabel(supersetGroup: String?): String? =
        supersetGroup?.takeIf { it.isNotBlank() }?.let { "Superset $it" }

    /** Same-group members excluding [current], ordered by orderIndex. */
    fun partners(
        entries: List<SessionExerciseEntity>,
        current: SessionExerciseEntity,
    ): List<SessionExerciseEntity> {
        val group = current.supersetGroup?.takeIf { it.isNotBlank() } ?: return emptyList()
        return entries
            .filter { it.id != current.id && it.supersetGroup == group }
            .sortedBy { it.orderIndex }
    }

    /** Alternating next-exercise recommendation for [currentId], or null. */
    fun nextUp(
        entries: List<SessionExerciseEntity>,
        currentId: Long,
    ): SessionExerciseEntity? {
        if (entries.isEmpty()) return null
        val ordered = entries.sortedBy { it.orderIndex }
        val current = ordered.firstOrNull { it.id == currentId } ?: return null
        if (isSupersetMember(current.supersetGroup)) {
            val groupMembers = ordered.filter { it.supersetGroup == current.supersetGroup }
            if (groupMembers.size < 2) return null
            val idx = groupMembers.indexOfFirst { it.id == currentId }
            return groupMembers[(idx + 1) % groupMembers.size]
        }
        val idx = ordered.indexOfFirst { it.id == currentId }
        return ordered.getOrNull(idx + 1)
    }
}
