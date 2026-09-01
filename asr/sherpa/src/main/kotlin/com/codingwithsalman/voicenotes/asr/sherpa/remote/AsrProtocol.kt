package com.codingwithsalman.voicenotes.asr.sherpa.remote

import android.os.Bundle
import com.tricodestudio.voicekit.Segment

/**
 * Wire format between the app process and the isolated ASR process.
 *
 * Deliberately a plain [android.os.Messenger] protocol rather than AIDL: there is exactly one call
 * here, and a hand-written `Bundle` keeps the whole contract readable in one file. Segments travel
 * as three parallel arrays because a `Parcelable` would have to live in `:voicekit`, which is
 * published as an SDK and should not grow Android-framework types for our convenience.
 *
 * Why a second process at all — see
 * brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md. sherpa-onnx can fail with a
 * `SIGBUS`, which is a signal rather than a throwable, so no amount of Kotlin error handling can
 * keep the app alive through one. Running inference somewhere else means the worst case is a dead
 * child process and a note marked FAILED, instead of the user's app disappearing.
 */
internal object AsrProtocol {

    // Requests (app → ASR process)
    const val MSG_TRANSCRIBE = 1

    // Replies (ASR process → app)
    const val MSG_PROGRESS = 100
    const val MSG_RESULT = 101
    const val MSG_ERROR = 102

    const val KEY_AUDIO_PATH = "audio_path"
    const val KEY_MODEL_ID = "model_id"
    const val KEY_PROGRESS = "progress"
    const val KEY_DURATION_MS = "duration_ms"
    const val KEY_LANGUAGE = "language"
    const val KEY_STARTS = "starts"
    const val KEY_ENDS = "ends"
    const val KEY_TEXTS = "texts"
    const val KEY_ERROR = "error"

    /** True when the failure is a permanent property of the device, so retrying is pointless. */
    const val KEY_UNSUPPORTED = "unsupported"

    fun encodeSegments(segments: List<Segment>): Bundle = Bundle().apply {
        putLongArray(KEY_STARTS, LongArray(segments.size) { segments[it].startMs })
        putLongArray(KEY_ENDS, LongArray(segments.size) { segments[it].endMs })
        putStringArray(KEY_TEXTS, Array(segments.size) { segments[it].text })
    }

    fun decodeSegments(data: Bundle): List<Segment> {
        val starts = data.getLongArray(KEY_STARTS) ?: return emptyList()
        val ends = data.getLongArray(KEY_ENDS) ?: return emptyList()
        val texts = data.getStringArray(KEY_TEXTS) ?: return emptyList()
        val n = minOf(starts.size, ends.size, texts.size)
        return (0 until n).map { Segment(starts[it], ends[it], texts[it] ?: "") }
    }
}

/** The ASR process died mid-transcription. Never retry this in the app process. */
internal class AsrProcessDiedException(message: String) : RuntimeException(message)
