package com.codingwithsalman.voicenotes.core.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codingwithsalman.voicenotes.core.model.ActionItem
import com.codingwithsalman.voicenotes.core.model.Note
import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val createdAtMs: Long,
    val durationMs: Long,
    val audioPath: String,
    val sizeBytes: Long,
    val status: String,
    val languageCode: String?,
    val summary: String?,
    /** Comma-joined 0..1 floats; cheap to parse, avoids a TypeConverter dependency. */
    val waveform: String?,
    /** Soft-delete tombstone: null = live; set to now() when the user deletes, so an Undo can
     *  restore it. A purge sweep (on library load) and the snackbar's timeout hard-delete it. */
    val deletedAtMs: Long? = null,
)

@Entity(
    tableName = "transcript_segments",
    indices = [Index("noteId")],
)
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long,
    val idx: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/** FTS mirror of segment text; Room keeps it in sync with the content entity via triggers. */
@Fts4(contentEntity = TranscriptSegmentEntity::class)
@Entity(tableName = "transcript_fts")
data class TranscriptFtsEntity(
    val text: String,
)

@Entity(
    tableName = "action_items",
    indices = [Index("noteId")],
)
data class ActionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long,
    val text: String,
    val done: Boolean,
    val createdAtMs: Long,
)

fun ActionItemEntity.asModel(): ActionItem = ActionItem(
    id = id,
    noteId = noteId,
    text = text,
    done = done,
    createdAtMs = createdAtMs,
)

fun NoteEntity.asModel(): Note = Note(
    id = id,
    title = title,
    createdAtMs = createdAtMs,
    durationMs = durationMs,
    audioPath = audioPath,
    sizeBytes = sizeBytes,
    status = TranscriptionStatus.entries.firstOrNull { it.name == status }
        ?: TranscriptionStatus.RECORDED,
    languageCode = languageCode,
    summary = summary,
    waveform = waveform
        ?.split(',')
        ?.mapNotNull(String::toFloatOrNull)
        ?.takeIf(List<Float>::isNotEmpty),
)

fun Note.asEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    createdAtMs = createdAtMs,
    durationMs = durationMs,
    audioPath = audioPath,
    sizeBytes = sizeBytes,
    status = status.name,
    languageCode = languageCode,
    summary = summary,
    waveform = waveform?.joinToString(",") { "%.3f".format(it) },
)

fun TranscriptSegmentEntity.asModel(): TranscriptSegment = TranscriptSegment(
    id = id,
    noteId = noteId,
    index = idx,
    startMs = startMs,
    endMs = endMs,
    text = text,
)
