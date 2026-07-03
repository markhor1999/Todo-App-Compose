package com.codingwithsalman.voicenotes.core.model

enum class TranscriptionStatus {
    /** Audio saved; transcription not yet requested (or engine not yet installed). */
    RECORDED,
    QUEUED,
    TRANSCRIBING,
    DONE,
    FAILED,
}

data class Note(
    val id: Long = 0L,
    val title: String,
    val createdAtMs: Long,
    val durationMs: Long,
    val audioPath: String,
    val sizeBytes: Long,
    val status: TranscriptionStatus,
    val languageCode: String? = null,
    val summary: String? = null,
    /** Normalized 0..1 amplitude envelope for waveform rendering; null until extracted. */
    val waveform: List<Float>? = null,
)

data class TranscriptSegment(
    val id: Long = 0L,
    val noteId: Long,
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class ActionItem(
    val id: Long = 0L,
    val noteId: Long,
    val text: String,
    val done: Boolean,
    val createdAtMs: Long,
)

enum class ThemeMode { SYSTEM, DARK, LIGHT }
