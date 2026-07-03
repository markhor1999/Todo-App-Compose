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
    version = 3, // v3 (pre-release): action_items
    exportSchema = false,
)
abstract class VoiceNotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}
