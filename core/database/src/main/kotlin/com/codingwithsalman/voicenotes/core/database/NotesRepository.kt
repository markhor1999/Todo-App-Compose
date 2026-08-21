package com.codingwithsalman.voicenotes.core.database

import com.codingwithsalman.voicenotes.core.common.di.IoDispatcher
import com.codingwithsalman.voicenotes.core.model.ActionItem
import com.codingwithsalman.voicenotes.core.model.Note
import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val dao: NotesDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun observeNotes(): Flow<List<Note>> =
        dao.observeNotes().map { entities -> entities.map(NoteEntity::asModel) }

    fun observeNote(id: Long): Flow<Note?> =
        dao.observeNote(id).map { it?.asModel() }

    suspend fun note(id: Long): Note? = dao.note(id)?.asModel()

    suspend fun notesByStatuses(vararg statuses: TranscriptionStatus): List<Note> =
        dao.notesByStatuses(statuses.map(TranscriptionStatus::name)).map(NoteEntity::asModel)

    /** Blank query returns everything; otherwise title-substring OR transcript full-text. */
    fun searchNotes(query: String): Flow<List<Note>> {
        val raw = query.trim()
        if (raw.isEmpty()) return observeNotes()
        // FTS MATCH syntax is picky: strip operators, keep word chars, prefix-match each term.
        val match = raw.split(Regex("\\s+"))
            .map { term -> term.filter(Char::isLetterOrDigit) }
            .filter(String::isNotEmpty)
            .joinToString(" ") { "$it*" }
        return if (match.isEmpty()) observeNotes()
        else dao.searchNotes(raw, match).map { entities -> entities.map(NoteEntity::asModel) }
    }

    suspend fun setWaveform(id: Long, waveform: List<Float>) {
        if (waveform.isEmpty()) return
        dao.updateWaveform(id, waveform.joinToString(",") { "%.3f".format(it) })
    }

    fun observeSegments(noteId: Long): Flow<List<TranscriptSegment>> =
        dao.observeSegments(noteId).map { entities -> entities.map(TranscriptSegmentEntity::asModel) }

    suspend fun createNote(note: Note): Long = dao.insertNote(note.asEntity())

    suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)

    suspend fun updateStatus(id: Long, status: TranscriptionStatus) =
        dao.updateStatus(id, status.name)

    suspend fun saveSegments(noteId: Long, segments: List<TranscriptSegment>) {
        dao.deleteSegments(noteId)
        dao.insertSegments(
            segments.mapIndexed { index, segment ->
                TranscriptSegmentEntity(
                    noteId = noteId,
                    idx = index,
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                    text = segment.text,
                )
            }
        )
    }

    /** Soft delete: hide the note but keep everything on disk so [restore] can bring it back. */
    suspend fun softDelete(id: Long) =
        dao.softDeleteNote(id, System.currentTimeMillis())

    /** Undo a [softDelete]. */
    suspend fun restore(id: Long) = dao.restoreNote(id)

    /** Permanently delete one note: its row, segments, action items, and the audio file. */
    suspend fun purge(id: Long) {
        withContext(ioDispatcher) {
            val audioPath = dao.note(id)?.audioPath
            dao.deleteSegments(id)
            dao.deleteActionItemsForNote(id)
            dao.deleteNote(id)
            audioPath?.let { runCatching { File(it).delete() } }
        }
    }

    /** Sweep any tombstoned notes whose undo window is gone (called on library load). */
    suspend fun purgeDeleted() {
        withContext(ioDispatcher) {
            dao.softDeletedNotes().forEach { purge(it.id) }
        }
    }

    fun observeActionItems(noteId: Long): Flow<List<ActionItem>> =
        dao.observeActionItems(noteId).map { entities -> entities.map(ActionItemEntity::asModel) }

    /** Returns the new row id, or -1 when the text was blank and nothing was inserted. */
    suspend fun addActionItem(
        noteId: Long,
        text: String,
        dueAtMs: Long? = null,
        sourceStartMs: Long? = null,
    ): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return -1L
        return dao.insertActionItem(
            ActionItemEntity(
                noteId = noteId,
                text = trimmed,
                done = false,
                createdAtMs = System.currentTimeMillis(),
                dueAtMs = dueAtMs,
                sourceStartMs = sourceStartMs,
            )
        )
    }

    suspend fun setActionItemDone(id: Long, done: Boolean) = dao.setActionItemDone(id, done)

    suspend fun setActionItemDue(id: Long, dueAtMs: Long?) = dao.setActionItemDue(id, dueAtMs)

    suspend fun actionItem(id: Long): ActionItem? = dao.actionItem(id)?.asModel()

    suspend fun pendingReminders(): List<ActionItem> =
        dao.pendingReminders().map(ActionItemEntity::asModel)

    suspend fun actionItemTexts(noteId: Long): List<String> = dao.actionItemTexts(noteId)

    /** How many action items were lifted from a transcript rather than typed by the user. */
    suspend fun extractedActionItemCount(): Int = dao.extractedActionItemCount()

    suspend fun deleteActionItem(id: Long) = dao.deleteActionItem(id)
}
