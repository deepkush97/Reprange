package com.deepkush.reprange.ui.screens.workout

import com.deepkush.reprange.data.db.SessionExerciseEntity
import com.deepkush.reprange.data.db.SetStrategy

/**
 * Pure superset grouping/sequence logic for the active workout.
 *
 * An entry is a superset member only when its [SessionExerciseEntity.strategy] is
 * [SetStrategy.SUPER_SET] **and** its [SessionExerciseEntity.supersetGroup] is non-blank
 * after trimming. Group IDs compare by trimmed value, so `"A"` and `" A "` match.
 *
 * Next-exercise recommendation ("Next Up"):
 * - hidden (null) when the entry is last in workout order, for both superset
 *   members and standalone entries;
 * - otherwise a superset member alternates to the next partner in the same group,
 *   wrapping around (pair A→B→A; larger groups cycle forward);
 * - otherwise a standalone entry points at the next entry in workout order.
 */
object SupersetGrouping {

    /** Trimmed group ID, or null when blank. */
    fun normalizedGroup(supersetGroup: String?): String? =
        supersetGroup?.trim()?.takeIf { it.isNotEmpty() }

    fun isSupersetMember(entry: SessionExerciseEntity): Boolean =
        entry.strategy == SetStrategy.SUPER_SET.name && normalizedGroup(entry.supersetGroup) != null

    fun supersetLabel(entry: SessionExerciseEntity): String? =
        if (isSupersetMember(entry)) "Superset ${normalizedGroup(entry.supersetGroup)}" else null

    /** Same-group superset members excluding [current], ordered by orderIndex. */
    fun partners(
        entries: List<SessionExerciseEntity>,
        current: SessionExerciseEntity,
    ): List<SessionExerciseEntity> {
        if (!isSupersetMember(current)) return emptyList()
        val group = normalizedGroup(current.supersetGroup) ?: return emptyList()
        return entries
            .filter { it.id != current.id && isSupersetMember(it) && normalizedGroup(it.supersetGroup) == group }
            .sortedBy { it.orderIndex }
    }

    /** Alternating next-exercise recommendation for [currentId], or null when none. */
    fun nextUp(
        entries: List<SessionExerciseEntity>,
        currentId: Long,
    ): SessionExerciseEntity? {
        if (entries.isEmpty()) return null
        val ordered = entries.sortedBy { it.orderIndex }
        val idx = ordered.indexOfFirst { it.id == currentId }
        if (idx == -1 || idx == ordered.lastIndex) return null
        val current = ordered[idx]
        if (isSupersetMember(current)) {
            val group = normalizedGroup(current.supersetGroup) ?: return null
            val groupMembers = ordered.filter { isSupersetMember(it) && normalizedGroup(it.supersetGroup) == group }
            if (groupMembers.size < 2) return null
            val memberIdx = groupMembers.indexOfFirst { it.id == currentId }
            return groupMembers[(memberIdx + 1) % groupMembers.size]
        }
        return ordered[idx + 1]
    }
}
