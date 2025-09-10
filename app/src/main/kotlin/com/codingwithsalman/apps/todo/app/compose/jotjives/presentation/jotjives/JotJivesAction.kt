package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.JotJiveFilterChip
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi

sealed interface JotJivesAction {
    data object OnMoodChipClick : JotJivesAction
    data object OnDismissMoodDropDown : JotJivesAction
    data class OnFilterByMoodClick(val moodUi: MoodUi) : JotJivesAction
    data object OnTopicChipClick : JotJivesAction
    data object OnDismissTopicDropDown : JotJivesAction
    data class OnFilterByTopicClick(val topic: String) : JotJivesAction
    data object OnRecordFabClick : JotJivesAction
    data object OnRequestPermissionQuickRecording : JotJivesAction
    data object OnRecordButtonLongClick : JotJivesAction
    data object OnSettingsClick : JotJivesAction
    data object OnAddJotClick : JotJivesAction
    data object OnPauseRecordingClick : JotJivesAction
    data object OnResumeRecordingClick : JotJivesAction
    data object OnCompleteRecording : JotJivesAction
    data object OnPauseAudioClick : JotJivesAction
    data class OnTrackSizeAvailable(val trackSizeInfo: TrackSizeInfo) : JotJivesAction
    data class OnRemoveFilters(val filterType: JotJiveFilterChip) : JotJivesAction
    data class OnPlayJiveClick(val jiveId: Int) : JotJivesAction
    data object OnAudioPermissionGranted : JotJivesAction
    data object OnCancelRecording : JotJivesAction
}