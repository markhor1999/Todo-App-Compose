package com.codingwithsalman.voicenotes.asr.api

import kotlinx.coroutines.flow.StateFlow

/**
 * State of the live-preview streaming model — separate from the offline [EngineState] because the
 * two models are independent (offline Whisper produces the SAVED transcript; the streaming model
 * only drives the on-screen preview while recording).
 */
sealed interface LiveModelState {
    data object NotInstalled : LiveModelState
    data class Downloading(val progress: Float) : LiveModelState
    data object Ready : LiveModelState
    data class Failed(val message: String) : LiveModelState
}

/**
 * Live transcription (v2.1 #2) — words-as-you-speak while recording. English-only in v2.1 (no
 * viable streaming model for ur/hi/ar yet); the saved transcript is unaffected. Ships OFF BY
 * DEFAULT ([com.codingwithsalman.voicenotes.core.datastore.SettingsRepository.liveTranscriptionEnabled]);
 * the streaming model downloads on-demand when the toggle is enabled.
 */
interface LiveTranscriptionManager {
    /** Install/download state of the streaming model. */
    val modelState: StateFlow<LiveModelState>

    /** True when the streaming model files are present (checked synchronously). */
    fun isModelInstalled(): Boolean

    /** Kick a download of the streaming model; progress is reflected in [modelState]. No-op if ready. */
    fun ensureModelDownloaded()

    /**
     * A fresh streaming session for one recording, or null if the model isn't installed. The caller
     * feeds it 16 kHz mono float PCM via [LiveSession.accept] and observes [LiveSession.text].
     */
    fun newSession(): LiveSession?
}

/**
 * One recording's live-preview session. The recognizer loads and decodes on its own background
 * thread, so [accept] is non-blocking and safe to call from the audio-capture thread.
 */
interface LiveSession {
    /** Running preview text: endpoint-committed segments + the current partial hypothesis. */
    val text: StateFlow<String>

    /** Accept a chunk of 16 kHz mono normalized-float samples. Non-blocking. */
    fun accept(samples: FloatArray)

    /** Stop feeding and release the native recognizer/stream. Idempotent. */
    fun release()
}
