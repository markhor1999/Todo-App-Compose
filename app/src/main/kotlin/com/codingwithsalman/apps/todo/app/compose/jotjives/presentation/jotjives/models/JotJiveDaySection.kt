package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotJiveUi
import com.codingwithsalman.jotjive.core.presentation.util.UiText

data class JotJiveDaySection(
    val dateHeader: UiText,
    val jotJives: List<JotJiveUi>
)
