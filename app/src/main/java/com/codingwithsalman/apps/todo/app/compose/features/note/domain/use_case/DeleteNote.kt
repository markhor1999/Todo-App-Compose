package com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.model.Note
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.repository.NoteRepository

class DeleteNote(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}