package com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.repository.NoteRepository

class GetNote(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Int) = repository.getNoteById(id)
}