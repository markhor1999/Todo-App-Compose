package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.jotjive.core.presentation.util.UiText

data class JotJiveDaySection(
    val dateHeader: UiText,
    val jives: List<JiveUi>
)
