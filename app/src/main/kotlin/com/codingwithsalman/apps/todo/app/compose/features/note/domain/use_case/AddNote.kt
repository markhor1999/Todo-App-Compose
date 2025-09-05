package com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case

import com.codingwithsalman.jotjive.core.database.jot.InvalidNoteException
import com.codingwithsalman.jotjive.core.domain.jot.Jot
import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource

class AddNote(
    private val jotDataSource: JotDataSource
) {
    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(jot: Jot) {
        if (jot.title.isBlank()) {
            throw InvalidNoteException("The title of the note can't by empty.")
        }
        if (jot.content.isBlank()) {
            throw InvalidNoteException("The content of the note can't by empty.")
        }
        jotDataSource.insertJot(jot)
    }
}