package com.codingwithsalman.apps.todo.app.compose.di

import android.content.Context
import androidx.room.Room
import com.codingwithsalman.apps.todo.app.compose.features.note.data.data_source.NoteDao
import com.codingwithsalman.apps.todo.app.compose.features.note.data.data_source.NoteDatabase
import com.codingwithsalman.apps.todo.app.compose.features.note.data.data_source.NoteDatabase.Companion.NOTE_DATABASE_NAME
import com.codingwithsalman.apps.todo.app.compose.features.note.data.repository.NoteRepositoryImpl
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.repository.NoteRepository
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesNoteDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context,
        NoteDatabase::class.java,
        NOTE_DATABASE_NAME
    ).build()

    @Provides
    @Singleton
    fun providesNoteDao(
        noteDatabase: NoteDatabase
    ) = noteDatabase.noteDao

    @Provides
    @Singleton
    fun providesNoteRepository(noteDao: NoteDao): NoteRepository = NoteRepositoryImpl(noteDao)

    @Provides
    @Singleton
    fun providesNoteUseCases(
        repository: NoteRepository
    ): NoteUseCases = NoteUseCases(
        getNotes = GetNotes(repository = repository),
        deleteNote = DeleteNote(repository = repository),
        addNote = AddNote(repository = repository),
        getNote = GetNote(repository = repository)
    )
}