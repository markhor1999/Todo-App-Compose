package com.codingwithsalman.jotjive.core.domain.recording

import kotlinx.coroutines.flow.StateFlow

interface VoiceRecorder {
    val recordingDetails: StateFlow<RecordingDetails>
    fun start()
    fun pause()
    fun stop()
    fun resume()
    fun cancel()
}