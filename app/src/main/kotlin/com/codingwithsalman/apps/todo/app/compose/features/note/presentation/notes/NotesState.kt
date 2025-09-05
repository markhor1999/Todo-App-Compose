package com.codingwithsalman.apps.todo.app.compose.features.note.presentation.notes

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.util.NoteOrder
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.util.OrderType
import com.codingwithsalman.jotjive.core.domain.jot.Jot

data class NotesState(
    val notes: List<Jot> = emptyList(),
    val noteOrder: NoteOrder = NoteOrder.Date(OrderType.Descending),
    val isOrderSectionVisible: Boolean = false
)