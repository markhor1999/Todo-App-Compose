package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.settings

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi

sealed interface SettingsAction {
    data class OnSearchTextChange(val text: String) : SettingsAction
    data class OnSelectTopicClick(val topic: String) : SettingsAction
    data class OnRemoveTopicClick(val topic: String) : SettingsAction
    data object OnBackClick : SettingsAction
    data object OnDismissTopicDropDown : SettingsAction
    data object OnAddButtonClick : SettingsAction
    data class OnMoodClick(val mood: MoodUi) : SettingsAction
}