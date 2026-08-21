package com.codingwithsalman.voicenotes.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE deletedAtMs IS NULL ORDER BY createdAtMs DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun note(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE status IN (:statuses)")
    suspend fun notesByStatuses(statuses: List<String>): List<NoteEntity>

    /**
     * Title substring match OR full-text match over transcript segments.
     * [match] is the sanitized FTS query (prefix form, e.g. `falsif*`).
     */
    @Query(
        """
        SELECT * FROM notes WHERE
            deletedAtMs IS NULL
            AND (
                title LIKE '%' || :raw || '%'
                OR id IN (
                    SELECT noteId FROM transcript_segments WHERE id IN (
                        SELECT rowid FROM transcript_fts WHERE transcript_fts MATCH :match
                    )
                )
            )
        ORDER BY createdAtMs DESC
        """
    )
    fun searchNotes(raw: String, match: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE notes SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE notes SET waveform = :waveform WHERE id = :id")
    suspend fun updateWaveform(id: Long, waveform: String)

    /** Soft delete: tombstone the row so it drops out of the lists but Undo can restore it. */
    @Query("UPDATE notes SET deletedAtMs = :ts WHERE id = :id")
    suspend fun softDeleteNote(id: Long, ts: Long)

    @Query("UPDATE notes SET deletedAtMs = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)

    /** Rows awaiting purge (undo window elapsed or lost to process death) — swept on library load. */
    @Query("SELECT * FROM notes WHERE deletedAtMs IS NOT NULL")
    suspend fun softDeletedNotes(): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Insert
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    @Query("SELECT * FROM transcript_segments WHERE noteId = :noteId ORDER BY idx")
    fun observeSegments(noteId: Long): Flow<List<TranscriptSegmentEntity>>

    @Query("DELETE FROM transcript_segments WHERE noteId = :noteId")
    suspend fun deleteSegments(noteId: Long)

    @Insert
    suspend fun insertActionItem(item: ActionItemEntity): Long

    @Query("SELECT * FROM action_items WHERE noteId = :noteId ORDER BY done ASC, createdAtMs ASC")
    fun observeActionItems(noteId: Long): Flow<List<ActionItemEntity>>

    @Query("UPDATE action_items SET done = :done WHERE id = :id")
    suspend fun setActionItemDone(id: Long, done: Boolean)

    @Query("UPDATE action_items SET dueAtMs = :dueAtMs WHERE id = :id")
    suspend fun setActionItemDue(id: Long, dueAtMs: Long?)

    @Query("SELECT * FROM action_items WHERE id = :id")
    suspend fun actionItem(id: Long): ActionItemEntity?

    /** Every outstanding deadline, soonest first — the set the scheduler re-arms from. */
    @Query("SELECT * FROM action_items WHERE dueAtMs IS NOT NULL AND done = 0 ORDER BY dueAtMs ASC")
    suspend fun pendingReminders(): List<ActionItemEntity>

    /** Existing item texts for a note, used to keep re-extraction from re-suggesting known items. */
    @Query("SELECT text FROM action_items WHERE noteId = :noteId")
    suspend fun actionItemTexts(noteId: Long): List<String>

    /** Extracted items carry a source position; hand-typed ones don't. */
    @Query("SELECT COUNT(*) FROM action_items WHERE sourceStartMs IS NOT NULL")
    suspend fun extractedActionItemCount(): Int

    @Query("DELETE FROM action_items WHERE id = :id")
    suspend fun deleteActionItem(id: Long)

    @Query("DELETE FROM action_items WHERE noteId = :noteId")
    suspend fun deleteActionItemsForNote(noteId: Long)
}
