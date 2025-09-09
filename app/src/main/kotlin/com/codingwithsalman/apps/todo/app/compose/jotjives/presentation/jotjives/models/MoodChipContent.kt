package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models

import com.codingwithsalman.apps.todo.app.compose.R
import com.codingwithsalman.jotjive.core.presentation.util.UiText

data class MoodChipContent(
    val iconsRes: List<Int> = emptyList(),
    val title: UiText = UiText.StringResource(R.string.all_moods)
)
