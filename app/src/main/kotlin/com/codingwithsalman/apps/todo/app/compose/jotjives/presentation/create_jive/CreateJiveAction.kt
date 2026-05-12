package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi

sealed interface CreateJiveAction {
    data object OnNavigateBackClick : CreateJiveAction
    data class OnTitleTextChange(val text: String) : CreateJiveAction
    data class OnAddTopicTextChange(val text: String) : CreateJiveAction
    data class OnNoteTextChange(val text: String) : CreateJiveAction
    data object OnSelectMoodClick : CreateJiveAction
    data object OnDismissMoodSelector : CreateJiveAction
    data class OnMoodClick(val moodUi: MoodUi) : CreateJiveAction
    data object OnConfirmMood : CreateJiveAction
    data class OnTopicClick(val topic: String) : CreateJiveAction
    data object OnDismissTopicSuggestions : CreateJiveAction
    data object OnCancelClick : CreateJiveAction
    data object OnSaveClick : CreateJiveAction
    data object OnPlayAudioClick : CreateJiveAction
    data object OnPauseAudioClick : CreateJiveAction
    data class OnTrackSizeAvailable(val trackSizeInfo: TrackSizeInfo) : CreateJiveAction
    data class OnRemoveTopicClick(val topic: String) : CreateJiveAction
    data object OnGoBack : CreateJiveAction
    data object OnDismissConfirmLeaveDialog : CreateJiveAction
}