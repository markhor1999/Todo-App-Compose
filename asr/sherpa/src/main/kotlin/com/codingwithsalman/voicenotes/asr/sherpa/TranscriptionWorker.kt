@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.asr.sherpa

import com.codingwithsalman.voicenotes.asr.sherpa.remote.AsrProcessDiedException
import com.codingwithsalman.voicenotes.asr.sherpa.remote.RemoteTranscriber
import com.tricodestudio.voicekit.SherpaTranscriptionEngine
import com.tricodestudio.voicekit.UnsupportedDeviceException
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codingwithsalman.voicenotes.asr.sherpa.R
import com.codingwithsalman.voicenotes.core.designsystem.R as DsR
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tricodestudio.voicekit.ModelCatalog
import com.codingwithsalman.voicenotes.core.common.tasks.TaskExtractor
import com.codingwithsalman.voicenotes.core.common.tasks.TaskSourceLine
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.codingwithsalman.voicenotes.core.reminders.ReminderScheduler
import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * One transcription job per note, owned by WorkManager so it survives process
 * death (a killed job is re-run; segment writes are replace-all = idempotent).
 */
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: SherpaTranscriptionEngine,
    private val remoteTranscriber: RemoteTranscriber,
    private val repository: NotesRepository,
    private val settings: SettingsRepository,
    private val entitlementStore: EntitlementStore,
    private val progressBus: TranscriptionProgressBus,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (noteId <= 0) return Result.failure()
        val note = repository.note(noteId) ?: return Result.failure()
        val spec = ModelCatalog.byId(settings.modelId.first())
        if (!engine.isReady(spec)) {
            // Covers both "model not downloaded yet" and "this device cannot load it at all".
            // The second case used to reach native code and die on a SIGBUS; now it fails the note.
            engine.unsupportedReason(spec)?.let { Log.w(TAG, "transcription unsupported: $it") }
            repository.updateStatus(noteId, TranscriptionStatus.FAILED)
            return Result.failure()
        }
        if (!entitlementStore.canStartTranscription()) {
            // Meter ran out between enqueue and execution; back to RECORDED for the paywall.
            repository.updateStatus(noteId, TranscriptionStatus.RECORDED)
            return Result.success()
        }

        Log.i(TAG, "transcribing note=$noteId (${note.durationMs} ms)")
        repository.updateStatus(noteId, TranscriptionStatus.TRANSCRIBING)
        progressBus.update(noteId, 0f)
        notifyProgress(note.title, 0)

        return try {
            var lastNotified = 0
            // Runs in the isolated `:asr` process — see RemoteTranscriber for the failure policy.
            val result = remoteTranscriber.transcribe(File(note.audioPath), spec) { progress ->
                progressBus.update(noteId, progress)
                val percent = (progress * 100).toInt()
                if (percent >= lastNotified + 10) { // keep notification churn low
                    lastNotified = percent
                    notifyProgress(note.title, percent)
                }
            }
            repository.saveSegments(
                noteId = noteId,
                segments = result.segments.map { segment ->
                    TranscriptSegment(
                        noteId = noteId,
                        index = 0, // repository assigns ordered indices on insert
                        startMs = segment.startMs,
                        endMs = segment.endMs,
                        text = segment.text,
                    )
                },
            )
            // Auto-title: default titles ("Note — …" / import filenames like rec_/imp_)
            // get replaced by the note's own first words. User-set titles are kept.
            val firstWords = result.segments.firstOrNull()?.text
                ?.split(Regex("\\s+"))?.take(6)?.joinToString(" ")
                ?.trim()?.trimEnd('.', ',', ';')
            val localizedNotePrefix = applicationContext
                .getString(DsR.string.vn_note_default_title).substringBefore("%1").trim()
            val localizedImportPrefix = applicationContext
                .getString(DsR.string.vn_imported_default_title).substringBefore("%1").trim()
            val isDefaultTitle = note.title.startsWith(localizedNotePrefix) ||
                note.title.startsWith(localizedImportPrefix) ||
                note.title.startsWith("Note — ") || note.title.startsWith("Imported — ") ||
                note.title.startsWith("rec_") || note.title.startsWith("imp_") ||
                note.title.startsWith("spike")
            if (!firstWords.isNullOrBlank() && isDefaultTitle) {
                repository.updateTitle(noteId, firstWords)
            }
            repository.updateStatus(noteId, TranscriptionStatus.DONE)
            entitlementStore.consume(note.durationMs)
            entitlementStore.recordTranscriptionSuccess()
            extractActionItems(noteId, note.createdAtMs, result.segments)
            Result.success()
        } catch (e: AsrProcessDiedException) {
            // The isolated process was killed — a native SIGBUS or the low-memory killer. In the
            // old in-process design this exact event took the whole app with it; now it is one
            // failed note. Deliberately NOT retried here: whatever killed that process would kill
            // this one, and this one is the app.
            Log.e(TAG, "ASR process died for note=$noteId", e)
            repository.updateStatus(noteId, TranscriptionStatus.FAILED)
            Result.failure()
        } catch (e: UnsupportedDeviceException) {
            Log.w(TAG, "device cannot transcribe note=$noteId: ${e.message}")
            repository.updateStatus(noteId, TranscriptionStatus.FAILED)
            Result.failure()
        } catch (e: OutOfMemoryError) {
            // Deliberately catching an Error, not an Exception. Transcription is the one place this
            // app can plausibly exhaust the heap — a whole recording is decoded into a single
            // FloatArray and a Whisper recognizer costs ~1 GB of native heap — and on the 2-3 GB
            // hardware Murmur is actually installed on that happened often enough to produce 15
            // separate Play issues, all landing on innocent bystanders because an OOM kills the
            // process at whatever allocated next. `catch (e: Exception)` never saw them.
            Log.e(TAG, "transcription ran out of memory for note=$noteId", e)
            repository.updateStatus(noteId, TranscriptionStatus.FAILED)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "transcription failed for note=$noteId", e)
            repository.updateStatus(noteId, TranscriptionStatus.FAILED)
            Result.failure()
        } finally {
            progressBus.clear(noteId)
            notificationManager.cancel(notificationId(noteId))
        }
    }

    /**
     * Opt-in (Settings → Action items): pull commitments out of the finished transcript, store them
     * as action items, and arm a reminder for any that carry a deadline.
     *
     * Dates resolve against the note's **recording** time, not now — "tomorrow" said last Tuesday
     * means last Wednesday, and a stale deadline is dropped by the scheduler rather than fired late.
     *
     * Failures here never fail the job: the transcript is the product, action items are a bonus, and
     * a note that transcribed fine must not be marked FAILED because a regex misbehaved.
     */
    private suspend fun extractActionItems(
        noteId: Long,
        recordedAtMs: Long,
        segments: List<com.tricodestudio.voicekit.Segment>,
    ) {
        if (!settings.autoTasksEnabled.first()) return
        runCatching {
            val candidates = TaskExtractor.extract(
                lines = segments.map { TaskSourceLine(it.text, it.startMs) },
                referenceMs = recordedAtMs,
                existingTexts = repository.actionItemTexts(noteId),
            )
            for (candidate in candidates) {
                val id = repository.addActionItem(
                    noteId = noteId,
                    text = candidate.text,
                    dueAtMs = candidate.dueAtMs,
                    sourceStartMs = candidate.sourceStartMs,
                )
                val due = candidate.dueAtMs
                if (id > 0 && due != null) reminderScheduler.schedule(id, due, noteId)
            }
        }.onFailure { Log.w(TAG, "action-item extraction failed for note=$noteId", it) }
    }

    private val notificationManager: NotificationManager
        get() = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun notifyProgress(title: String, percent: Int) {
        // Best-effort: silently invisible if POST_NOTIFICATIONS is denied.
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.vn_ic_notif_transcribe)
            .setContentTitle(applicationContext.getString(DsR.string.vn_notif_transcribing_title))
            .setContentText(title)
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
        runCatching {
            notificationManager.notify(notificationId(inputData.getLong(KEY_NOTE_ID, 0L)), notification)
        }
    }

    companion object {
        const val KEY_NOTE_ID = "noteId"
        const val CHANNEL_ID = "transcription"
        const val TAG = "VnTranscription"

        fun uniqueName(noteId: Long) = "transcribe_$noteId"
        private fun notificationId(noteId: Long) = 41_000 + (noteId % 1000).toInt()
    }
}
