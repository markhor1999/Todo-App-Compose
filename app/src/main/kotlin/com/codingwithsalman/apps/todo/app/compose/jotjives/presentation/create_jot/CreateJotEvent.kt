package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jot

sealed interface CreateJotEvent {
    data object JotSuccessfullySaved : CreateJotEvent
}