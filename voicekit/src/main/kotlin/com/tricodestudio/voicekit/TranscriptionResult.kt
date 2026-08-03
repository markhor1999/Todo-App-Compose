package com.tricodestudio.voicekit

/** One transcribed span of speech, positioned against the start of the source audio. */
public data class Segment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
) {
    public val durationMs: Long get() = endMs - startMs
}

/**
 * The output of a transcription.
 *
 * [text] exists because most callers want only that, and joining segments by hand in every consuming
 * app is a papercut the SDK should absorb. [durationMs] is here because it is what teams meter their
 * own users on — it is not what *we* bill for, since the audio never reaches us.
 */
public data class TranscriptionResult(
    val segments: List<Segment>,
    /** BCP-47-ish code the model detected or was told to use. Null when unknown. */
    val language: String?,
    /** Length of the audio processed. */
    val durationMs: Long,
) {
    /** All segment text joined with single spaces. */
    public val text: String by lazy {
        segments.joinToString(" ") { it.text.trim() }.trim()
    }

    /** True when the model produced no speech at all — silence, or audio it could not decode. */
    public val isEmpty: Boolean get() = segments.isEmpty()
}
