package com.deepkush.reprange.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseFts::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        CompletedSetEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ReprangeDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun sessionDao(): SessionDao
}
