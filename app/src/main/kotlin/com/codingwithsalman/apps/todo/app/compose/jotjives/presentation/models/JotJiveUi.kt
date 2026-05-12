package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models

sealed interface JotJiveUi {
    val id: Int
    val title: String

    data class Jot(
        val jotUi: JotUi
    ) : JotJiveUi {
        override val id: Int
            get() = jotUi.id

        override val title: String
            get() = jotUi.title
    }

    data class Jive(
        val jiveUi: JiveUi
    ) : JotJiveUi {
        override val id: Int
            get() = jiveUi.id

        override val title: String
            get() = jiveUi.title
    }
}