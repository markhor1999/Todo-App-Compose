package com.codingwithsalman.voicenotes.feature.note

import com.codingwithsalman.voicenotes.core.model.Note
import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    MARKDOWN("md", "text/markdown"),
    SRT("srt", "application/x-subrip"),
}

object TranscriptFormats {

    fun fileName(note: Note, format: ExportFormat): String =
        note.title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(60)
            .trim()
            .ifEmpty { "transcript" } + "." + format.extension

    fun render(note: Note, segments: List<TranscriptSegment>, format: ExportFormat): String =
        when (format) {
            ExportFormat.TXT -> plainText(segments)
            ExportFormat.MARKDOWN -> markdown(note, segments)
            ExportFormat.SRT -> srt(segments)
        }

    /** Share-sheet body: plain text, no timestamps clutter. */
    fun plainText(segments: List<TranscriptSegment>): String =
        segments.joinToString("\n\n") { it.text }

    private fun markdown(note: Note, segments: List<TranscriptSegment>): String = buildString {
        appendLine("# ${note.title}")
        appendLine()
        segments.forEach { segment ->
            appendLine("**[${clock(segment.startMs)}]** ${segment.text}")
            appendLine()
        }
    }.trimEnd() + "\n"

    private fun srt(segments: List<TranscriptSegment>): String = buildString {
        segments.forEachIndexed { index, segment ->
            appendLine(index + 1)
            appendLine("${srtTime(segment.startMs)} --> ${srtTime(segment.endMs)}")
            appendLine(segment.text)
            appendLine()
        }
    }.trimEnd() + "\n"

    private fun clock(ms: Long): String {
        val total = ms / 1000
        return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
    }

    private fun srtTime(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, millis)
    }
}
