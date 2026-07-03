package com.codingwithsalman.voicenotes.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass
import androidx.room.util.FtsTableInfo.Companion.read as ftsTableInfoRead
import androidx.room.util.TableInfo.Companion.read as tableInfoRead

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class VoiceNotesDatabase_Impl : VoiceNotesDatabase() {
  private val _notesDao: Lazy<NotesDao> = lazy {
    NotesDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3,
        "aec3a2e7361fb856aefb936c2bb9ca7b", "e90d32f86a1aa4008a347ddb3aef7084") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAtMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `audioPath` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `languageCode` TEXT, `summary` TEXT, `waveform` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transcript_segments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `noteId` INTEGER NOT NULL, `idx` INTEGER NOT NULL, `startMs` INTEGER NOT NULL, `endMs` INTEGER NOT NULL, `text` TEXT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_segments_noteId` ON `transcript_segments` (`noteId`)")
        connection.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `transcript_fts` USING FTS4(`text` TEXT NOT NULL, content=`transcript_segments`)")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_BEFORE_UPDATE BEFORE UPDATE ON `transcript_segments` BEGIN DELETE FROM `transcript_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_BEFORE_DELETE BEFORE DELETE ON `transcript_segments` BEGIN DELETE FROM `transcript_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_AFTER_UPDATE AFTER UPDATE ON `transcript_segments` BEGIN INSERT INTO `transcript_fts`(`docid`, `text`) VALUES (NEW.`rowid`, NEW.`text`); END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_AFTER_INSERT AFTER INSERT ON `transcript_segments` BEGIN INSERT INTO `transcript_fts`(`docid`, `text`) VALUES (NEW.`rowid`, NEW.`text`); END")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `action_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `noteId` INTEGER NOT NULL, `text` TEXT NOT NULL, `done` INTEGER NOT NULL, `createdAtMs` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_action_items_noteId` ON `action_items` (`noteId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'aec3a2e7361fb856aefb936c2bb9ca7b')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `notes`")
        connection.execSQL("DROP TABLE IF EXISTS `transcript_segments`")
        connection.execSQL("DROP TABLE IF EXISTS `transcript_fts`")
        connection.execSQL("DROP TABLE IF EXISTS `action_items`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_BEFORE_UPDATE BEFORE UPDATE ON `transcript_segments` BEGIN DELETE FROM `transcript_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_BEFORE_DELETE BEFORE DELETE ON `transcript_segments` BEGIN DELETE FROM `transcript_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_AFTER_UPDATE AFTER UPDATE ON `transcript_segments` BEGIN INSERT INTO `transcript_fts`(`docid`, `text`) VALUES (NEW.`rowid`, NEW.`text`); END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_transcript_fts_AFTER_INSERT AFTER INSERT ON `transcript_segments` BEGIN INSERT INTO `transcript_fts`(`docid`, `text`) VALUES (NEW.`rowid`, NEW.`text`); END")
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsNotes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNotes.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("createdAtMs", TableInfo.Column("createdAtMs", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("audioPath", TableInfo.Column("audioPath", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("sizeBytes", TableInfo.Column("sizeBytes", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("languageCode", TableInfo.Column("languageCode", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("summary", TableInfo.Column("summary", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("waveform", TableInfo.Column("waveform", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNotes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNotes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNotes: TableInfo = TableInfo("notes", _columnsNotes, _foreignKeysNotes,
            _indicesNotes)
        val _existingNotes: TableInfo = tableInfoRead(connection, "notes")
        if (!_infoNotes.equals(_existingNotes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |notes(com.codingwithsalman.voicenotes.core.database.NoteEntity).
              | Expected:
              |""".trimMargin() + _infoNotes + """
              |
              | Found:
              |""".trimMargin() + _existingNotes)
        }
        val _columnsTranscriptSegments: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTranscriptSegments.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTranscriptSegments.put("noteId", TableInfo.Column("noteId", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTranscriptSegments.put("idx", TableInfo.Column("idx", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTranscriptSegments.put("startMs", TableInfo.Column("startMs", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTranscriptSegments.put("endMs", TableInfo.Column("endMs", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTranscriptSegments.put("text", TableInfo.Column("text", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTranscriptSegments: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTranscriptSegments: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTranscriptSegments.add(TableInfo.Index("index_transcript_segments_noteId", false,
            listOf("noteId"), listOf("ASC")))
        val _infoTranscriptSegments: TableInfo = TableInfo("transcript_segments",
            _columnsTranscriptSegments, _foreignKeysTranscriptSegments, _indicesTranscriptSegments)
        val _existingTranscriptSegments: TableInfo = tableInfoRead(connection,
            "transcript_segments")
        if (!_infoTranscriptSegments.equals(_existingTranscriptSegments)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transcript_segments(com.codingwithsalman.voicenotes.core.database.TranscriptSegmentEntity).
              | Expected:
              |""".trimMargin() + _infoTranscriptSegments + """
              |
              | Found:
              |""".trimMargin() + _existingTranscriptSegments)
        }
        val _columnsTranscriptFts: MutableSet<String> = mutableSetOf()
        _columnsTranscriptFts.add("text")
        val _infoTranscriptFts: FtsTableInfo = FtsTableInfo("transcript_fts", _columnsTranscriptFts,
            "CREATE VIRTUAL TABLE IF NOT EXISTS `transcript_fts` USING FTS4(`text` TEXT NOT NULL, content=`transcript_segments`)")
        val _existingTranscriptFts: FtsTableInfo = ftsTableInfoRead(connection, "transcript_fts")
        if (!_infoTranscriptFts.equals(_existingTranscriptFts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transcript_fts(com.codingwithsalman.voicenotes.core.database.TranscriptFtsEntity).
              | Expected:
              |""".trimMargin() + _infoTranscriptFts + """
              |
              | Found:
              |""".trimMargin() + _existingTranscriptFts)
        }
        val _columnsActionItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsActionItems.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsActionItems.put("noteId", TableInfo.Column("noteId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsActionItems.put("text", TableInfo.Column("text", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsActionItems.put("done", TableInfo.Column("done", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsActionItems.put("createdAtMs", TableInfo.Column("createdAtMs", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysActionItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesActionItems: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesActionItems.add(TableInfo.Index("index_action_items_noteId", false,
            listOf("noteId"), listOf("ASC")))
        val _infoActionItems: TableInfo = TableInfo("action_items", _columnsActionItems,
            _foreignKeysActionItems, _indicesActionItems)
        val _existingActionItems: TableInfo = tableInfoRead(connection, "action_items")
        if (!_infoActionItems.equals(_existingActionItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |action_items(com.codingwithsalman.voicenotes.core.database.ActionItemEntity).
              | Expected:
              |""".trimMargin() + _infoActionItems + """
              |
              | Found:
              |""".trimMargin() + _existingActionItems)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    _shadowTablesMap.put("transcript_fts", "transcript_segments")
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "notes", "transcript_segments",
        "transcript_fts", "action_items")
  }

  public override fun clearAllTables() {
    super.performClear(false, "notes", "transcript_segments", "transcript_fts", "action_items")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(NotesDao::class, NotesDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun notesDao(): NotesDao = _notesDao.value
}
