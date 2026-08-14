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
    version = 5, // v5: action_items.dueAtMs + sourceStartMs (deadlines & reminders, 2.3.0)
    exportSchema = false,
)
abstract class VoiceNotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}
