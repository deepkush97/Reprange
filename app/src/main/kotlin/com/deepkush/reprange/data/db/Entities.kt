package com.deepkush.reprange.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** Set strategies applied at planning time (§ brief: Standard, Step-up/Pyramid, Drop, Super, Failure). */
enum class SetStrategy(val label: String) {
    STANDARD("Standard"),
    STEP_UP("Step-up"),
    DROP_SET("Drop set"),
    SUPER_SET("Super set"),
    FAILURE("Failure"),
}

/** Set types recorded per logged set. */
enum class LoggedSetType(val label: String, val shortLabel: String) {
    NORMAL("Normal", "N"),
    WARMUP("Warmup", "W"),
    DROP_SET("Drop set", "D"),
    FAILURE("Failure", "F"),
}

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val target: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String,
    @ColumnInfo(name = "secondary_muscles") val secondaryMuscles: List<String>,
    val equipment: String,
    val difficulty: String,
    @ColumnInfo(name = "instructions_en") val instructionsEn: List<String>,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    @ColumnInfo(name = "gif_url") val gifUrl: String,
    @ColumnInfo(name = "media_id") val mediaId: String,
    val attribution: String,
)

@Fts4(contentEntity = ExerciseEntity::class)
@Entity(tableName = "exercise_fts")
data class ExerciseFts(
    val name: String,
    val category: String,
    val target: String,
    val equipment: String,
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("template_id"), Index("exercise_id")],
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "template_id") val templateId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "set_count") val setCount: Int = 3,
    val strategy: String = SetStrategy.STANDARD.name,
    @ColumnInfo(name = "superset_group") val supersetGroup: String? = null,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double? = null,
    @ColumnInfo(name = "target_reps") val targetReps: Int? = null,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int? = null,
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "template_id") val templateId: Long? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "total_volume_kg") val totalVolumeKg: Double = 0.0,
    val notes: String = "",
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id"), Index("exercise_id")],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int = 90,
    @ColumnInfo(name = "superset_group") val supersetGroup: String? = null,
    val strategy: String = SetStrategy.STANDARD.name,
)

@Entity(
    tableName = "completed_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id"), Index("exercise_id"), Index("session_exercise_id")],
)
data class CompletedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "session_exercise_id") val sessionExerciseId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "set_number") val setNumber: Int,
    @ColumnInfo(name = "set_type") val setType: String = LoggedSetType.NORMAL.name,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    val reps: Int,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
)

private val converterJson = kotlinx.serialization.json.Json
private val stringListSerializer: kotlinx.serialization.KSerializer<List<String>> =
    kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>())

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String =
        converterJson.encodeToString(stringListSerializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { converterJson.decodeFromString(stringListSerializer, value) }
            .getOrDefault(emptyList())
}
