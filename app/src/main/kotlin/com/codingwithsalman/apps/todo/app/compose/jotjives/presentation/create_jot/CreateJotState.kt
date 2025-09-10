package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jot

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable

data class CreateJotState(
    val titleText: String = "",
    val addTopicText: String = "",
    val topics: List<String> = listOf(),
    val noteText: String = "",
    val showMoodSelector: Boolean = true,
    val selectedMood: MoodUi = MoodUi.NEUTRAL,
    val showTopicSuggestions: Boolean = false,
    val mood: MoodUi? = null,
    val searchResults: List<Selectable<String>> = emptyList(),
    val showCreateTopicOption: Boolean = true,
    val canSaveJot: Boolean = false,
    val showConfirmLeaveDialog: Boolean = false
)
