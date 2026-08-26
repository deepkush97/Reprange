package com.deepkush.reprange.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.ExerciseRepository
import com.deepkush.reprange.data.repo.SessionRepository
import com.deepkush.reprange.data.repo.TemplateRepository
import com.deepkush.reprange.domain.usecase.StartWorkoutSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val templateRepository: TemplateRepository,
    private val sessionRepository: SessionRepository,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
) : ViewModel() {

    val templates: StateFlow<List<com.deepkush.reprange.data.db.TemplateSummary>> =
        templateRepository.observeTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<com.deepkush.reprange.data.db.SessionSummary>> =
        sessionRepository.observeRecentSessions(5)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seedState: StateFlow<DatasetSeeder.SeedState>
        get() = exerciseRepository.seedState

    init {
        viewModelScope.launch { exerciseRepository.seedIfNeeded() }
    }

    fun retrySeed() {
        viewModelScope.launch { exerciseRepository.seedIfNeeded() }
    }

    fun startBlank(onStarted: (Long) -> Unit) {
        viewModelScope.launch { onStarted(startWorkoutSessionUseCase.blank()) }
    }

    fun startTemplate(templateId: Long, onStarted: (Long?) -> Unit) {
        viewModelScope.launch { onStarted(startWorkoutSessionUseCase.fromTemplate(templateId)) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { templateRepository.deleteTemplate(id) }
    }
}
