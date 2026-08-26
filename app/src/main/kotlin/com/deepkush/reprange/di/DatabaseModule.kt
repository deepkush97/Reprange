package com.deepkush.reprange.di

import android.content.Context
import androidx.room.Room
import com.deepkush.reprange.data.db.ExerciseDao
import com.deepkush.reprange.data.db.ReprangeDatabase
import com.deepkush.reprange.data.db.SessionDao
import com.deepkush.reprange.data.db.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReprangeDatabase =
        Room.databaseBuilder(context, ReprangeDatabase::class.java, "reprange.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideExerciseDao(db: ReprangeDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideTemplateDao(db: ReprangeDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideSessionDao(db: ReprangeDatabase): SessionDao = db.sessionDao()
}
