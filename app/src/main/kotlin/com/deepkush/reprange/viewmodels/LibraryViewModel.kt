package com.deepkush.reprange.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryFilters(
    val query: String = "",
    val category: String? = null,
    val equipment: String? = null,
    val difficulty: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val filters = MutableStateFlow(LibraryFilters())
    val seedState: StateFlow<DatasetSeeder.SeedState>
        get() = exerciseRepository.seedState

    val categories: StateFlow<List<String>> = exerciseRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipmentOptions: StateFlow<List<String>> = exerciseRepository.observeEquipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<ExerciseEntity>> = filters
        .debounce(150)
        .flatMapLatest { f ->
            exerciseRepository.search(f.query, f.category, f.equipment, f.difficulty)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        filters.value = filters.value.copy(query = q)
    }

    fun setCategory(c: String?) {
        filters.value = filters.value.copy(category = c)
    }

    fun setEquipment(e: String?) {
        filters.value = filters.value.copy(equipment = e)
    }

    fun setDifficulty(d: String?) {
        filters.value = filters.value.copy(difficulty = d)
    }

    fun clearFilters() {
        val q = filters.value.query
        filters.value = LibraryFilters(query = q)
    }

    fun retrySeed() {
        viewModelScope.launch { exerciseRepository.seedIfNeeded() }
    }

    init {
        viewModelScope.launch { exerciseRepository.seedIfNeeded() }
    }
}
