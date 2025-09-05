package com.codingwithsalman.apps.todo.app.compose.features.note.data.repository

import com.codingwithsalman.apps.todo.app.compose.features.note.data.data_source.NoteDao
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.model.Note
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {
    override fun getNotes(): Flow<List<Note>> = noteDao.getNotes()

    override suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    override suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}