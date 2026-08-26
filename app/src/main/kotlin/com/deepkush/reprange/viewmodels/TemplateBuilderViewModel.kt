package com.deepkush.reprange.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.data.repo.TemplateRepository
import com.deepkush.reprange.domain.usecase.CreateWorkoutPlanUseCase
import com.deepkush.reprange.domain.usecase.PlanItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class DraftEntry(
    val exerciseId: String,
    val name: String,
    val imageUrl: String,
    val setCount: Int = 3,
    val strategy: SetStrategy = SetStrategy.STANDARD,
    val targetWeightKg: Double? = null,
    val targetReps: Int? = null,
    val restSeconds: Int? = null,
    val supersetGroup: String? = null,
)

data class TemplateDraft(
    val templateId: Long?,
    val title: String = "",
    val notes: String = "",
    val entries: List<DraftEntry> = emptyList(),
)

/** Holds the in-memory routine draft while the builder screen is open. */
@HiltViewModel
class TemplateBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val templateRepository: TemplateRepository,
    private val exerciseRepository: ExerciseRepository,
    private val createWorkoutPlanUseCase: CreateWorkoutPlanUseCase,
) : ViewModel() {

    private val templateId: Long? = runCatching { savedStateHandle.get<Long?>("templateId") }.getOrNull()?.takeIf { it > 0 }

    private val _draft = MutableStateFlow(TemplateDraft(templateId = templateId))
    val draft: StateFlow<TemplateDraft> = _draft

    init {
        viewModelScope.launch {
            if (templateId != null) {
                val withEntries = templateRepository.observeTemplate(templateId).firstOrNull() ?: return@launch
                val exercises = exerciseRepository.getExercises(withEntries.entries.map { it.exerciseId })
                    .associateBy { it.id }
                _draft.value = TemplateDraft(
                    templateId = templateId,
                    title = withEntries.template.title,
                    notes = withEntries.template.notes,
                    entries = withEntries.entries.sortedBy { it.orderIndex }.mapNotNull { e ->
                        exercises[e.exerciseId]?.let { ex ->
                            DraftEntry(
                                exerciseId = e.exerciseId,
                                name = ex.name,
                                imageUrl = ex.imageUrl,
                                setCount = e.setCount,
                                strategy = runCatching { SetStrategy.valueOf(e.strategy) }.getOrDefault(SetStrategy.STANDARD),
                                targetWeightKg = e.targetWeightKg,
                                targetReps = e.targetReps,
                                restSeconds = e.restSeconds,
                                supersetGroup = e.supersetGroup,
                            )
                        }
                    },
                )
            }
        }
    }

    fun setTitle(t: String) {
        _draft.value = _draft.value.copy(title = t)
    }

    fun setNotes(n: String) {
        _draft.value = _draft.value.copy(notes = n)
    }

    fun addExercise(exerciseId: String) {
        viewModelScope.launch {
            val ex = exerciseRepository.getExercise(exerciseId) ?: return@launch
            val current = _draft.value
            if (current.entries.any { it.exerciseId == exerciseId }) return@launch
            _draft.value = current.copy(
                entries = current.entries + DraftEntry(
                    exerciseId = exerciseId,
                    name = ex.name,
                    imageUrl = ex.imageUrl,
                ),
            )
        }
    }

    fun removeAt(index: Int) {
        val e = _draft.value.entries.toMutableList()
        if (index in e.indices) {
            e.removeAt(index)
            _draft.value = _draft.value.copy(entries = e)
        }
    }

    fun move(index: Int, delta: Int) {
        val entries = _draft.value.entries.toMutableList()
        val target = index + delta
        if (index !in entries.indices || target !in entries.indices) return
        val item = entries.removeAt(index)
        entries.add(target, item)
        _draft.value = _draft.value.copy(entries = entries)
    }

    fun changeSetCount(index: Int, delta: Int) {
        val entries = _draft.value.entries.toMutableList()
        val e = entries.getOrNull(index) ?: return
        entries[index] = e.copy(setCount = (e.setCount + delta).coerceIn(1, 12))
        _draft.value = _draft.value.copy(entries = entries)
    }

    fun setStrategy(index: Int, strategy: SetStrategy) {
        val entries = _draft.value.entries.toMutableList()
        val e = entries.getOrNull(index) ?: return
        entries[index] = e.copy(strategy = strategy)
        _draft.value = _draft.value.copy(entries = entries)
    }

    /** Toggles pairing with the previous exercise as a super set. */
    fun toggleSupersetWithPrevious(index: Int) {
        val entries = _draft.value.entries.toMutableList()
        if (index <= 0 || index >= entries.size) return
        val prev = entries[index - 1]
        val cur = entries[index]
        val newGroup = if (cur.supersetGroup != null && cur.supersetGroup == prev.supersetGroup) {
            null
        } else {
            prev.supersetGroup ?: "A"
        }
        entries[index] = cur.copy(supersetGroup = newGroup, strategy = SetStrategy.SUPER_SET.takeIf { newGroup != null } ?: cur.strategy)
        if (newGroup != null && prev.supersetGroup == null) {
            entries[index - 1] = prev.copy(supersetGroup = newGroup)
        }
        _draft.value = _draft.value.copy(entries = entries)
    }

    fun save(onSaved: (Long?) -> Unit) {
        val d = _draft.value
        if (d.title.isBlank() || d.entries.isEmpty()) {
            onSaved(null)
            return
        }
        viewModelScope.launch {
            val id = createWorkoutPlanUseCase(
                templateId = d.templateId,
                title = d.title,
                notes = d.notes,
                items = d.entries.map {
                    PlanItem(
                        exerciseId = it.exerciseId,
                        setCount = it.setCount,
                        strategy = it.strategy,
                        targetWeightKg = it.targetWeightKg,
                        targetReps = it.targetReps,
                        restSeconds = it.restSeconds,
                        supersetGroup = it.supersetGroup,
                    )
                },
            )
            onSaved(id)
        }
    }
}
