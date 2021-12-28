package com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.model.InvalidNoteException
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.model.Note
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.repository.NoteRepository

class AddNote(
    private val repository: NoteRepository
) {
    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(note: Note) {
        if (note.title.isBlank()) {
            throw InvalidNoteException("The title of the note can't by empty.")
        }
        if (note.content.isBlank()) {
            throw InvalidNoteException("The content of the note can't by empty.")
        }
        repository.insertNote(note)
    }
}