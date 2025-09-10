package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive

sealed interface CreateJiveEvent {
    data object FailedToSaveFile : CreateJiveEvent
    data object JiveSuccessfullySaved : CreateJiveEvent
}