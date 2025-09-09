package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives

import com.codingwithsalman.jotjive.core.domain.recording.RecordingDetails

sealed interface JotJivesEvent {
    data object RequestAudioPermission : JotJivesEvent
    data object RecordingTooShort : JotJivesEvent
    data class OnDoneRecording(val details: RecordingDetails) : JotJivesEvent
}