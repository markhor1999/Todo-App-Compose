package com.codingwithsalman.apps.todo.app.compose.features.note.domain.repository

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>

    suspend fun getNoteById(id: Int): Note?

    suspend fun insertNote(note: Note)

    suspend fun deleteNote(note: Note)
}