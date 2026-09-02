package com.deepkush.reprange.data.repo

import com.deepkush.reprange.data.db.CompletedSetEntity
import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.db.PrRow
import com.deepkush.reprange.data.db.RawSetPoint
import com.deepkush.reprange.data.db.SessionDao
import com.deepkush.reprange.data.db.TemplateDao
import kotlinx.coroutines.flow.Flow

/** Marker so DI can inject the DAO triple cleanly. */
annotation class DaosModuleMarker

@javax.inject.Singleton
open class ExerciseRepository @javax.inject.Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val seeder: DatasetSeeder,
) {
    open val seedState: kotlinx.coroutines.flow.StateFlow<DatasetSeeder.SeedState> get() = seeder.state

    open suspend fun seedIfNeeded() = seeder.seedIfNeeded()

    fun search(query: String, category: String?, equipment: String?, difficulty: String?): Flow<List<ExerciseEntity>> {
        val q = query.trim()
        return exerciseDao.search(
            query = q.ifBlank { null },
            fts = q.ifBlank { null }?.let { "$it*" },
            category = category,
            equipment = equipment,
            difficulty = difficulty,
            limit = 60,
            offset = 0,
        )
    }

    fun countSearch(query: String, category: String?, equipment: String?, difficulty: String?): Flow<Int> {
        val q = query.trim()
        return exerciseDao.countSearch(
            query = q.ifBlank { null },
            fts = q.ifBlank { null }?.let { "$it*" },
            category = category,
            equipment = equipment,
            difficulty = difficulty,
        )
    }

    fun observeExercise(id: String): Flow<ExerciseEntity?> = exerciseDao.observeById(id)

    suspend fun getExercise(id: String): ExerciseEntity? = exerciseDao.getById(id)

    suspend fun getExercises(ids: List<String>): List<ExerciseEntity> = exerciseDao.getByIds(ids)

    fun observeCategories(): Flow<List<String>> = exerciseDao.observeCategories()

    fun observeEquipment(): Flow<List<String>> = exerciseDao.observeEquipment()
}

@javax.inject.Singleton
class TemplateRepository @javax.inject.Inject constructor(private val templateDao: TemplateDao) {
    fun observeTemplates(): Flow<List<com.deepkush.reprange.data.db.TemplateSummary>> =
        templateDao.observeTemplates()

    fun observeTemplate(id: Long): Flow<com.deepkush.reprange.data.db.TemplateWithExercises?> =
        templateDao.observeWithExercises(id)

    suspend fun upsertTemplate(template: com.deepkush.reprange.data.db.WorkoutTemplateEntity): Long =
        templateDao.upsertTemplate(template)

    suspend fun replaceExercises(templateId: Long, entries: List<com.deepkush.reprange.data.db.TemplateExerciseEntity>) {
        templateDao.deleteExercisesOf(templateId)
        templateDao.insertExercises(entries)
    }

    suspend fun deleteTemplate(id: Long) = templateDao.deleteTemplate(id)
}

@javax.inject.Singleton
class SessionRepository @javax.inject.Inject constructor(private val sessionDao: SessionDao) {
    fun observeActiveSession(): Flow<com.deepkush.reprange.data.db.SessionWithExercises?> =
        sessionDao.observeActive()

    suspend fun getActiveSession(): com.deepkush.reprange.data.db.SessionWithExercises? =
        sessionDao.getActive()

    fun observeRecentSessions(limit: Int = 10): Flow<List<com.deepkush.reprange.data.db.SessionSummary>> =
        sessionDao.observeRecentSessions(limit)

    suspend fun insertSession(session: com.deepkush.reprange.data.db.WorkoutSessionEntity): Long =
        sessionDao.insertSession(session)

    suspend fun insertEntries(entries: List<com.deepkush.reprange.data.db.SessionExerciseEntity>) =
        sessionDao.insertEntries(entries)

    suspend fun deleteEntry(entry: com.deepkush.reprange.data.db.SessionExerciseEntity) =
        sessionDao.deleteEntry(entry)

    suspend fun insertSet(set: CompletedSetEntity): Long = sessionDao.insertSet(set)

    suspend fun deleteSet(set: CompletedSetEntity) = sessionDao.deleteSet(set)

    suspend fun updateSession(session: com.deepkush.reprange.data.db.WorkoutSessionEntity) =
        sessionDao.updateSession(session)

    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)

    suspend fun previousSets(exerciseId: String, beforeMillis: Long): List<CompletedSetEntity> =
        sessionDao.previousSets(exerciseId, beforeMillis)

    suspend fun normalSetsSince(exerciseId: String, since: Long): List<RawSetPoint> =
        sessionDao.normalSetsSince(exerciseId, since)

    suspend fun allNormalSetsSince(since: Long): List<com.deepkush.reprange.data.db.DatedVolumePoint> =
        sessionDao.allNormalSetsSince(since)

    fun observePrTable(): Flow<List<PrRow>> = sessionDao.observePrTable()

    fun observeVolumeBetween(from: Long, to: Long): Flow<Double> = sessionDao.observeVolumeBetween(from, to)

    fun observeSetCountBetween(from: Long, to: Long): Flow<Int> = sessionDao.observeSetCountBetween(from, to)
}
