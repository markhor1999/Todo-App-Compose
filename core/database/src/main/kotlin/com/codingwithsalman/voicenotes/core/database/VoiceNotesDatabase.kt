package com.codingwithsalman.voicenotes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        TranscriptSegmentEntity::class,
        TranscriptFtsEntity::class,
        ActionItemEntity::class,
    ],
    version = 4, // v4 (first shipped migration): notes.deletedAtMs soft-delete tombstone
    exportSchema = false,
)
abstract class VoiceNotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}
