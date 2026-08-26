package com.deepkush.reprange.data.repo

import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.remote.DatasetApi
import com.deepkush.reprange.data.remote.DatasetMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    companion object {
        fun fromName(name: String): Difficulty =
            entries.firstOrNull { it.name == name } ?: BEGINNER

        /**
         * Heuristic derived from dataset fields (the dataset has no explicit difficulty):
         * advanced skill keywords > barbell/smith/machine compounds > everything else.
         */
        fun derive(equipment: String, name: String, category: String): Difficulty {
            val n = name.lowercase()
            val advancedKeywords = listOf(
                "muscle up", "muscle-up", "snatch", "clean and jerk", "pistol", "dragon",
                "planche", "front lever", "back lever", "handstand",
            )
            if (advancedKeywords.any { it in n }) return ADVANCED
            val compoundEquipment = setOf("barbell", "ez barbell", "smith machine", "leverage machine", "weighted")
            val compoundKeywords = listOf("squat", "deadlift", "press", "pull-up", "pullup", "chin-up", "dip", "row")
            return when {
                equipment.lowercase() in compoundEquipment && compoundKeywords.any { it in n } -> INTERMEDIATE
                compoundKeywords.any { k -> k in n } -> INTERMEDIATE
                else -> BEGINNER
            }
        }
    }
}

/**
 * Ingests the raw JSON from github.com/hasaneyldrm/exercises-dataset via Retrofit and seeds
 * the Room database on first launch.
 */
@Singleton
class DatasetSeeder @Inject constructor(
    private val api: DatasetApi,
    private val exerciseDao: ExerciseDao,
) {

    sealed interface SeedState {
        data object Idle : SeedState
        data object Loading : SeedState
        data object Done : SeedState
        data class Error(val message: String) : SeedState
    }

    private val _state = MutableStateFlow<SeedState>(SeedState.Idle)
    val state: StateFlow<SeedState> = _state

    private val seeding = AtomicBoolean(false)

    suspend fun seedIfNeeded() {
        if (exerciseDao.count() > 0) {
            _state.value = SeedState.Done
            return
        }
        if (!seeding.compareAndSet(false, true)) return
        try {
            if (exerciseDao.count() > 0) {
                _state.value = SeedState.Done
                return
            }
            _state.value = SeedState.Loading
            _state.value = runCatching {
                val dtos = api.exercises()
                val entities = dtos.map { dto ->
                    val steps = dto.instructionSteps?.get("en").orEmpty()
                        .ifEmpty {
                            dto.instructions?.en
                                ?.split(Regex("\\n+"))
                                ?.filter { it.isNotBlank() }
                                .orEmpty()
                        }
                    ExerciseEntity(
                        id = dto.id,
                        name = dto.name.trim(),
                        category = (dto.bodyPart.ifBlank { dto.category }).lowercase().trim(),
                        target = dto.target.lowercase().trim(),
                        muscleGroup = dto.muscleGroup.lowercase().trim(),
                        secondaryMuscles = dto.secondaryMuscles.map { it.lowercase().trim() },
                        equipment = dto.equipment.lowercase().trim(),
                        difficulty = Difficulty.derive(dto.equipment, dto.name, dto.category).name,
                        instructionsEn = steps,
                        imageUrl = DatasetMedia.thumbnailUrl(dto.image),
                        gifUrl = DatasetMedia.gifUrl(dto.gifUrl),
                        mediaId = dto.mediaId,
                        attribution = dto.attribution.ifBlank { "© Gym visual — https://gymvisual.com/" },
                    )
                }
                exerciseDao.insertAll(entities)
                SeedState.Done
            }.getOrElse { t -> SeedState.Error(t.message ?: "Network error") }
        } finally {
            seeding.set(false)
        }
    }

    suspend fun retry() = seedIfNeeded()
}
