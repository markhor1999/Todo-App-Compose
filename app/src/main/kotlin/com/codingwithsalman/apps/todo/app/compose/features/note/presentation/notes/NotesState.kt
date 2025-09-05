package com.codingwithsalman.apps.todo.app.compose.features.note.presentation.notes

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.model.Note
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.util.NoteOrder
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.util.OrderType

data class NotesState(
    val notes: List<Note> = emptyList(),
    val noteOrder: NoteOrder = NoteOrder.Date(OrderType.Descending),
    val isOrderSectionVisible: Boolean = false
)