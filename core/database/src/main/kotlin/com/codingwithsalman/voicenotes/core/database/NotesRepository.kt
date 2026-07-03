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

    /** Deletes the row, its segments, action items, and the audio file on disk. */
    suspend fun deleteNote(note: Note) {
        withContext(ioDispatcher) {
            dao.deleteSegments(note.id)
            dao.deleteActionItemsForNote(note.id)
            dao.deleteNote(note.id)
            runCatching { File(note.audioPath).delete() }
        }
    }

    fun observeActionItems(noteId: Long): Flow<List<ActionItem>> =
        dao.observeActionItems(noteId).map { entities -> entities.map(ActionItemEntity::asModel) }

    suspend fun addActionItem(noteId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertActionItem(
            ActionItemEntity(
                noteId = noteId,
                text = trimmed,
                done = false,
                createdAtMs = System.currentTimeMillis(),
            )
        )
    }

    suspend fun setActionItemDone(id: Long, done: Boolean) = dao.setActionItemDone(id, done)

    suspend fun deleteActionItem(id: Long) = dao.deleteActionItem(id)
}
