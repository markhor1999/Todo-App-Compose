package com.codingwithsalman.apps.todo.app.compose.features.note.domain.util

sealed class OrderType{
    object Ascending: OrderType()
    object Descending: OrderType()
}
