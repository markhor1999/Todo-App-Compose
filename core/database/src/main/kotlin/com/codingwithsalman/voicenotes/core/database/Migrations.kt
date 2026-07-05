package com.codingwithsalman.voicenotes.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Real Room migrations. v3 was declared the shipping baseline (destructive fallback removed), so
 * every schema change from here ships with a Migration listed in [ALL_MIGRATIONS] and a test.
 *
 * 3 → 4: add the nullable `deletedAtMs` tombstone to `notes` for Undo-able soft delete.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Nullable, no default: existing rows become live notes (deletedAtMs = NULL).
        db.execSQL("ALTER TABLE notes ADD COLUMN deletedAtMs INTEGER")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4)
