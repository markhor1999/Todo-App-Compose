package com.codingwithsalman.apps.todo.app.compose.features.note.presentation.notes

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.util.NoteOrder
import com.codingwithsalman.jotjive.core.domain.jot.Jot

sealed class NotesEvent {
    data class Order(val noteOrder: NoteOrder) : NotesEvent()
    data class DeleteNote(val note: Jot) : NotesEvent()
    object RestoreNote : NotesEvent()
    object ToggleOrderSection : NotesEvent()
}
