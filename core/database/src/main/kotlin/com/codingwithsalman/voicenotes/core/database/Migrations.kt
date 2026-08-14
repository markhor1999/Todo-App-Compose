package com.codingwithsalman.voicenotes.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Real Room migrations. v3 was declared the shipping baseline (destructive fallback removed), so
 * every schema change from here ships with a Migration listed in [ALL_MIGRATIONS] and a test.
 *
 * 3 → 4: add the nullable `deletedAtMs` tombstone to `notes` for Undo-able soft delete.
 * 4 → 5: add nullable `dueAtMs` + `sourceStartMs` to `action_items` (deadlines & reminders, 2.3.0).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Nullable, no default: existing rows become live notes (deletedAtMs = NULL).
        db.execSQL("ALTER TABLE notes ADD COLUMN deletedAtMs INTEGER")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Both nullable with no default: every existing action item becomes an undated item with
        // no audio anchor, which is exactly what it was before this release.
        db.execSQL("ALTER TABLE action_items ADD COLUMN dueAtMs INTEGER")
        db.execSQL("ALTER TABLE action_items ADD COLUMN sourceStartMs INTEGER")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4, MIGRATION_4_5)
