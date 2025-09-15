package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jot

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi

sealed interface CreateJotAction {
    data object OnNavigateBackClick : CreateJotAction
    data class OnTitleTextChange(val text: String) : CreateJotAction
    data class OnAddTopicTextChange(val text: String) : CreateJotAction
    data class OnNoteTextChange(val text: String) : CreateJotAction
    data object OnSelectMoodClick : CreateJotAction
    data object OnDismissMoodSelector : CreateJotAction
    data class OnMoodClick(val moodUi: MoodUi) : CreateJotAction
    data object OnConfirmMood : CreateJotAction
    data class OnTopicClick(val topic: String) : CreateJotAction
    data object OnDismissTopicSuggestions : CreateJotAction
    data object OnCancelClick : CreateJotAction
    data object OnSaveClick : CreateJotAction
    data class OnRemoveTopicClick(val topic: String) : CreateJotAction
    data class OnJotCategoryUpdated(val category: JotCategory) : CreateJotAction
    data object OnGoBack : CreateJotAction
    data object OnDismissConfirmLeaveDialog : CreateJotAction
}