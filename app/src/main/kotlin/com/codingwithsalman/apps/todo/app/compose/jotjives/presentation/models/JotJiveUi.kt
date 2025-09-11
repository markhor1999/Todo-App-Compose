package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models

sealed interface JotJiveUi {
    val id: Int

    data class Jot(
        val jotUi: JotUi
    ) : JotJiveUi {
        override val id: Int
            get() = jotUi.id
    }

    data class Jive(
        val jiveUi: JiveUi
    ) : JotJiveUi {
        override val id: Int
            get() = jiveUi.id

    }
}