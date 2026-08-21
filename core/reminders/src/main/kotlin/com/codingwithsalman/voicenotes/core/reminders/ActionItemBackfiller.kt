package com.codingwithsalman.voicenotes.core.reminders

import android.util.Log
import com.codingwithsalman.voicenotes.core.common.tasks.TaskExtractor
import com.codingwithsalman.voicenotes.core.common.tasks.TaskSourceLine
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** What a backfill pass produced, for the UI to report back to the user. */
data class BackfillResult(val itemCount: Int, val noteCount: Int)

/**
 * Runs action-item extraction over notes that were **already transcribed before the feature
 * existed**.
 *
 * Extraction normally happens once, inside `TranscriptionWorker`, at the moment a transcript is
 * produced. That is the right place for it — and it means an existing user who updates to 2.3.1
 * sees nothing change until they record a *new* meeting, because every note they already own was
 * transcribed under a build that had no extractor. Their whole library, which is exactly the
 * content that would demonstrate the feature, stays empty.
 *
 * This closes that gap on demand — in bulk from the library highlight card ([run]), or for a single
 * note from its overflow menu ([runForNote]).
 *
 * The per-note entry point is not a convenience. Extraction otherwise happens **only** at
 * transcription time and **only** if the setting was on at that moment, and the bulk card is
 * one-shot and permanently dismissible. Without [runForNote], a note transcribed while the feature
 * was off could never acquire action items by any means short of re-running Whisper over the whole
 * recording — minutes of inference to repeat a regex pass.
 *
 * **Dates resolve against each note's own recording time**, never against now — a note from July
 * saying "by Friday" means *that* Friday. Those deadlines are therefore in the past, and
 * [ReminderScheduler.schedule] drops past deadlines rather than firing them, so a backfill over a
 * long history cannot produce a burst of notifications. Only genuinely future deadlines from recent
 * notes arm anything.
 */
@Singleton
class ActionItemBackfiller @Inject constructor(
    private val repository: NotesRepository,
    private val reminderScheduler: ReminderScheduler,
) {

    /**
     * Extract from every transcribed note, skipping text already present as an action item, so
     * running it twice is a no-op rather than a duplicate.
     */
    suspend fun run(): BackfillResult {
        var items = 0
        var notes = 0
        for (note in repository.notesByStatuses(TranscriptionStatus.DONE)) {
            val found = extractInto(note.id, note.createdAtMs)
            if (found > 0) {
                items += found
                notes++
            }
        }
        return BackfillResult(itemCount = items, noteCount = notes)
    }

    /** Extract from one note's existing transcript. Returns what it added. */
    suspend fun runForNote(noteId: Long): BackfillResult {
        val note = repository.note(noteId) ?: return BackfillResult(0, 0)
        val found = extractInto(note.id, note.createdAtMs)
        return BackfillResult(itemCount = found, noteCount = if (found > 0) 1 else 0)
    }

    private suspend fun extractInto(noteId: Long, recordedAtMs: Long): Int {
        val segments = runCatching { repository.observeSegments(noteId).first() }.getOrNull()
        if (segments.isNullOrEmpty()) return 0

        val candidates = runCatching {
            TaskExtractor.extract(
                lines = segments.map { TaskSourceLine(it.text, it.startMs) },
                referenceMs = recordedAtMs,
                existingTexts = repository.actionItemTexts(noteId),
            )
        }.getOrElse {
            // One bad note must not abort a bulk pass.
            Log.w(TAG, "extraction failed for note=$noteId", it)
            emptyList()
        }

        var added = 0
        for (candidate in candidates) {
            val id = repository.addActionItem(
                noteId = noteId,
                text = candidate.text,
                dueAtMs = candidate.dueAtMs,
                sourceStartMs = candidate.sourceStartMs,
            )
            val due = candidate.dueAtMs
            if (id > 0 && due != null) reminderScheduler.schedule(id, due, noteId)
            added++
        }
        return added
    }

    private companion object {
        const val TAG = "ActionItemBackfiller"
    }
}
