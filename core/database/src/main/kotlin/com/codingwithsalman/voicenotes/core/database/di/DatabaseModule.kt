package com.codingwithsalman.voicenotes.core.database.di

import android.content.Context
import androidx.room.Room
import com.codingwithsalman.voicenotes.core.database.ALL_MIGRATIONS
import com.codingwithsalman.voicenotes.core.database.NotesDao
import com.codingwithsalman.voicenotes.core.database.VoiceNotesDatabase
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
    fun providesDatabase(@ApplicationContext context: Context): VoiceNotesDatabase =
        Room.databaseBuilder(
            context,
            VoiceNotesDatabase::class.java,
            // Fresh DB name on purpose: the JotJive-era schema is unrelated and the
            // rewrite starts clean (the old app had ~0 users).
            "voicenotes.db",
        )
            // v3 is the release baseline — every schema change from here ships
            // with a real Migration (no destructive fallback).
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun providesNotesDao(database: VoiceNotesDatabase): NotesDao = database.notesDao()
}
