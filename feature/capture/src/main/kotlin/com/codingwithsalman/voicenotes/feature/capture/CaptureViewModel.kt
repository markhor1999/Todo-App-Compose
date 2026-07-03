package com.codingwithsalman.voicenotes.feature.capture

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin screen adapter over [RecordingSessionManager] — the session outlives this
 * ViewModel (navigation, backgrounding), so no recording state lives here.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val sessionManager: RecordingSessionManager,
) : ViewModel() {

    val state: StateFlow<RecordingSessionState> = sessionManager.state
    val events: SharedFlow<RecordingSessionEvent> = sessionManager.events

    /** @return false when the recorder could not start (e.g. mic busy). */
    fun startRecording(): Boolean = sessionManager.startRecording()

    fun stopAndSave() = sessionManager.stopAndSave()

    fun pauseResume() {
        if (sessionManager.state.value.isPaused) sessionManager.resumeRecording()
        else sessionManager.pauseRecording()
    }

    fun discard() = sessionManager.discard()
}
