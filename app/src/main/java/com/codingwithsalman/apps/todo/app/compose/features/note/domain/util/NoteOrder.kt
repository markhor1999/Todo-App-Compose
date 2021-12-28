package com.codingwithsalman.apps.todo.app.compose.features.note.domain.util

sealed class NoteOrder(val orderType: OrderType) {
    class Title(orderType: OrderType) : NoteOrder(orderType)
    class Date(orderType: OrderType) : NoteOrder(orderType)
    class Color(orderType: OrderType) : NoteOrder(orderType)

    fun copy(orderType: OrderType): NoteOrder {
        return when (this) {
            is Date -> Date(orderType = orderType)
            is Title -> Title(orderType = orderType)
            is Color -> Color(orderType = orderType)
        }
    }
}
