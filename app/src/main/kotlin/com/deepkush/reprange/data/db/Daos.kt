package com.deepkush.reprange.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class TemplateSummary(
    @Embedded val template: WorkoutTemplateEntity,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
)

data class SessionSummary(
    @Embedded val session: WorkoutSessionEntity,
    @ColumnInfo(name = "set_count") val setCount: Int,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
)

data class SessionExerciseWithSets(
    @Embedded val entry: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "session_exercise_id")
    val sets: List<CompletedSetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "session_id")
    val exercises: List<SessionExerciseWithSets>,
)

data class ExerciseWithTemplateMeta(
    @Embedded val exercise: ExerciseEntity,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "set_count") val setCount: Int,
    @ColumnInfo(name = "strategy", defaultValue = "STANDARD") val strategy: String,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double?,
    @ColumnInfo(name = "target_reps") val targetReps: Int?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "superset_group") val supersetGroup: String?,
)

@Dao
interface ExerciseDao {

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExerciseEntity>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:query IS NULL OR id IN (SELECT rowid FROM exercise_fts WHERE exercise_fts MATCH :fts))
          AND (:category IS NULL OR category = :category)
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
        ORDER BY name
        LIMIT :limit OFFSET :offset
        """,
    )
    fun search(
        query: String?,
        fts: String?,
        category: String?,
        equipment: String?,
        difficulty: String?,
        limit: Int,
        offset: Int,
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM exercises
        WHERE (:query IS NULL OR id IN (SELECT rowid FROM exercise_fts WHERE exercise_fts MATCH :fts))
          AND (:category IS NULL OR category = :category)
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
        """,
    )
    fun countSearch(
        query: String?,
        fts: String?,
        category: String?,
        equipment: String?,
        difficulty: String?,
    ): Flow<Int>

    @Query("SELECT DISTINCT category FROM exercises ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM exercises ORDER BY equipment")
    fun observeEquipment(): Flow<List<String>>
}

@Dao
interface TemplateDao {

    @Transaction
    @Query(
        """
        SELECT t.*, COUNT(te.id) AS exercise_count
        FROM workout_templates t LEFT JOIN template_exercises te ON te.template_id = t.id
        GROUP BY t.id
        ORDER BY t.created_at DESC
        """,
    )
    fun observeTemplates(): Flow<List<TemplateSummary>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutTemplateEntity?>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    fun observeWithExercises(id: Long): Flow<TemplateWithExercises?>

    @Upsert
    suspend fun upsertTemplate(template: WorkoutTemplateEntity): Long

    @Insert
    suspend fun insertExercises(exercises: List<TemplateExerciseEntity>)

    @Update
    suspend fun updateExercise(entry: TemplateExerciseEntity)

    @Delete
    suspend fun deleteExercise(entry: TemplateExerciseEntity)

    @Query("DELETE FROM template_exercises WHERE template_id = :templateId")
    suspend fun deleteExercisesOf(templateId: Long)

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)
}

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "template_id")
    val entries: List<TemplateExerciseEntity>,
)

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Insert
    suspend fun insertEntry(entry: SessionExerciseEntity): Long

    @Insert
    suspend fun insertEntries(entries: List<SessionExerciseEntity>)

    @Delete
    suspend fun deleteEntry(entry: SessionExerciseEntity)

    @Insert
    suspend fun insertSet(set: CompletedSetEntity): Long

    @Delete
    suspend fun deleteSet(set: CompletedSetEntity)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    fun observeActive(): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    suspend fun getActive(): SessionWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<SessionWithExercises?>

    @Query(
        """
        SELECT s.*, COUNT(cs.id) AS set_count,
               (SELECT COUNT(DISTINCT cs2.exercise_id) FROM completed_sets cs2 WHERE cs2.session_id = s.id) AS exercise_count
        FROM workout_sessions s LEFT JOIN completed_sets cs ON cs.session_id = s.id
        WHERE s.ended_at IS NOT NULL
        GROUP BY s.id
        ORDER BY s.started_at DESC
        LIMIT :limit
        """,
    )
    fun observeRecentSessions(limit: Int = 10): Flow<List<SessionSummary>>

    /** Previous sets for an exercise, most recent first - powers auto-populate + last-session line. */
    @Query(
        """
        SELECT cs.* FROM completed_sets cs
        JOIN workout_sessions s ON s.id = cs.session_id
        WHERE cs.exercise_id = :exerciseId AND s.started_at < :beforeMillis
        ORDER BY cs.completed_at DESC
        LIMIT :limit
        """,
    )
    suspend fun previousSets(exerciseId: String, beforeMillis: Long, limit: Int = 24): List<CompletedSetEntity>

    @Query(
        """
        SELECT cs.weight_kg AS weightKg, cs.reps AS reps, s.started_at AS ts
        FROM completed_sets cs
        JOIN workout_sessions s ON s.id = cs.session_id
        WHERE cs.exercise_id = :exerciseId AND cs.set_type = 'NORMAL' AND s.ended_at IS NOT NULL AND s.started_at >= :since
        ORDER BY cs.completed_at ASC
        """,
    )
    suspend fun normalSetsSince(exerciseId: String, since: Long): List<RawSetPoint>

    @Query(
        """
        SELECT cs.weight_kg AS weightKg, cs.reps AS reps, s.started_at AS ts
        FROM completed_sets cs
        JOIN workout_sessions s ON s.id = cs.session_id
        WHERE s.ended_at IS NOT NULL AND s.started_at >= :since
        ORDER BY cs.completed_at ASC
        """,
    )
    suspend fun allNormalSetsSince(since: Long): List<DatedVolumePoint>

    @Query(
        """
        SELECT cs.exercise_id AS exerciseId, MAX(cs.weight_kg) AS bestWeightKg,
               MAX(cs.reps) AS bestReps, MAX(cs.weight_kg * (1.0 + cs.reps / 30.0)) AS bestEst1Rm,
               MAX(cs.completed_at) AS lastAt
        FROM completed_sets cs
        JOIN workout_sessions s ON s.id = cs.session_id
        WHERE s.ended_at IS NOT NULL
        GROUP BY cs.exercise_id
        """,
    )
    fun observePrTable(): Flow<List<PrRow>>

    @Query(
        """
        SELECT COALESCE(SUM(cs.weight_kg * cs.reps), 0.0) FROM completed_sets cs
        JOIN workout_sessions s ON s.id = cs.session_id
        WHERE s.ended_at IS NOT NULL AND s.started_at BETWEEN :from AND :to
        """,
    )
    fun observeVolumeBetween(from: Long, to: Long): Flow<Double>

    @Query(
        """
        SELECT COUNT(*) FROM completed_sets cs
        JOIN workout_sessions s ON s.id = cs.session_id
        WHERE s.ended_at IS NOT NULL AND s.started_at BETWEEN :from AND :to
        """,
    )
    fun observeSetCountBetween(from: Long, to: Long): Flow<Int>
}

data class RawSetPoint(val weightKg: Double, val reps: Int, val ts: Long)

data class DatedVolumePoint(val weightKg: Double, val reps: Int, val ts: Long)

data class PrRow(
    val exerciseId: String,
    val bestWeightKg: Double,
    val bestReps: Int,
    val bestEst1Rm: Double,
    val lastAt: Long,
)
