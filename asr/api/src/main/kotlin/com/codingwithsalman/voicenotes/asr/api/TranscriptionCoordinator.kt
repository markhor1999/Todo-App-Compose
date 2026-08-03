@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.asr.api

import com.tricodestudio.voicekit.AsrModelSpec

import kotlinx.coroutines.flow.StateFlow

sealed interface EngineState {
    /** Model files not on device; [spec] is what ensureModel() would fetch. */
    data class NotInstalled(val spec: AsrModelSpec) : EngineState

    data class Downloading(val spec: AsrModelSpec, val progress: Float) : EngineState

    data class DownloadFailed(val spec: AsrModelSpec, val message: String) : EngineState

    data class Ready(val spec: AsrModelSpec) : EngineState
}

/**
 * Process-wide transcription front door. Owns the model lifecycle and a serial
 * job queue; screens observe state and fire requests — nothing here blocks the UI.
 * (WorkManager-backed persistence replaces the in-process queue in M2.)
 */
interface TranscriptionCoordinator {

    val engineState: StateFlow<EngineState>

    /** Per-note transcription progress (0..1) for jobs in flight. */
    val progressByNote: StateFlow<Map<Long, Float>>

    /** Starts (or resumes) downloading the default model + VAD. No-op when ready. */
    fun ensureModel()

    /** Queues a note for transcription; no-op if the engine isn't ready. */
    fun requestTranscription(noteId: Long)
}
