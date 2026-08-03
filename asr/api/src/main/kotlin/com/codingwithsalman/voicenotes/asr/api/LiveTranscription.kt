@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

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
 * Live transcription (v2.1 #2) — a transcript preview while recording. Two engines behind one
 * contract: **word-level English** (optional streaming-model download) and **phrase-level in any
 * language** (VAD-chunked decoding on the already-installed offline model — no extra download).
 * The saved transcript is unaffected either way. Ships OFF BY DEFAULT
 * ([com.codingwithsalman.voicenotes.core.datastore.SettingsRepository.liveTranscriptionEnabled]).
 */
interface LiveTranscriptionManager {
    /** Install/download state of the optional word-level (EN) streaming model only — the chunked
     *  path needs no download and is available whenever the offline model is installed. */
    val modelState: StateFlow<LiveModelState>

    /** True when ANY live path can run right now (offline model installed, or streaming model). */
    fun isAvailable(): Boolean

    /** Kick a download of the word-level streaming model; progress lands in [modelState]. */
    fun ensureModelDownloaded()

    /**
     * A fresh live session for one recording, or null when no path is usable (nothing installed, or
     * a background transcription job currently owns the inference slot). The caller feeds it 16 kHz
     * mono float PCM via [LiveSession.accept] and observes [LiveSession.text].
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
