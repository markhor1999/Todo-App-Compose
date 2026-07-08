package com.codingwithsalman.voicenotes.asr.api

/** One transcribed span of speech with absolute positions in the recording. */
data class SegmentResult(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class TranscriptionResult(
    val segments: List<SegmentResult>,
    val languageCode: String?,
)
