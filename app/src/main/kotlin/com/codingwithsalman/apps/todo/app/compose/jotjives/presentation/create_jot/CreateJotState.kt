package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jot

import androidx.annotation.StringRes
import com.codingwithsalman.apps.todo.app.compose.R
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
    val showConfirmLeaveDialog: Boolean = false,
    val jotCategories: List<JotCategory> = JotCategory.entries,
    val selectedJotCategory: JotCategory = jotCategories.first()
)

enum class JotCategory(
    @StringRes
    val title: Int
) {
    NOTE(R.string.notes),
    TODO(R.string.todo)
}