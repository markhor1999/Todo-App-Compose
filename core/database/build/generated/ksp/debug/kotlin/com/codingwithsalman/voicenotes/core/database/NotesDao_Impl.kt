package com.codingwithsalman.voicenotes.core.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class NotesDao_Impl(
  __db: RoomDatabase,
) : NotesDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNoteEntity: EntityInsertAdapter<NoteEntity>

  private val __insertAdapterOfTranscriptSegmentEntity: EntityInsertAdapter<TranscriptSegmentEntity>

  private val __insertAdapterOfActionItemEntity: EntityInsertAdapter<ActionItemEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNoteEntity = object : EntityInsertAdapter<NoteEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `notes` (`id`,`title`,`createdAtMs`,`durationMs`,`audioPath`,`sizeBytes`,`status`,`languageCode`,`summary`,`waveform`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NoteEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindLong(3, entity.createdAtMs)
        statement.bindLong(4, entity.durationMs)
        statement.bindText(5, entity.audioPath)
        statement.bindLong(6, entity.sizeBytes)
        statement.bindText(7, entity.status)
        val _tmpLanguageCode: String? = entity.languageCode
        if (_tmpLanguageCode == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpLanguageCode)
        }
        val _tmpSummary: String? = entity.summary
        if (_tmpSummary == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpSummary)
        }
        val _tmpWaveform: String? = entity.waveform
        if (_tmpWaveform == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpWaveform)
        }
      }
    }
    this.__insertAdapterOfTranscriptSegmentEntity = object :
        EntityInsertAdapter<TranscriptSegmentEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `transcript_segments` (`id`,`noteId`,`idx`,`startMs`,`endMs`,`text`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TranscriptSegmentEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.noteId)
        statement.bindLong(3, entity.idx.toLong())
        statement.bindLong(4, entity.startMs)
        statement.bindLong(5, entity.endMs)
        statement.bindText(6, entity.text)
      }
    }
    this.__insertAdapterOfActionItemEntity = object : EntityInsertAdapter<ActionItemEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `action_items` (`id`,`noteId`,`text`,`done`,`createdAtMs`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ActionItemEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.noteId)
        statement.bindText(3, entity.text)
        val _tmp: Int = if (entity.done) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.createdAtMs)
      }
    }
  }

  public override suspend fun insertNote(note: NoteEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfNoteEntity.insertAndReturnId(_connection, note)
    _result
  }

  public override suspend fun insertSegments(segments: List<TranscriptSegmentEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTranscriptSegmentEntity.insert(_connection, segments)
  }

  public override suspend fun insertActionItem(item: ActionItemEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfActionItemEntity.insertAndReturnId(_connection, item)
    _result
  }

  public override fun observeNotes(): Flow<List<NoteEntity>> {
    val _sql: String = "SELECT * FROM notes ORDER BY createdAtMs DESC"
    return createFlow(__db, false, arrayOf("notes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfAudioPath: Int = getColumnIndexOrThrow(_stmt, "audioPath")
        val _columnIndexOfSizeBytes: Int = getColumnIndexOrThrow(_stmt, "sizeBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfLanguageCode: Int = getColumnIndexOrThrow(_stmt, "languageCode")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfWaveform: Int = getColumnIndexOrThrow(_stmt, "waveform")
        val _result: MutableList<NoteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NoteEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpAudioPath: String
          _tmpAudioPath = _stmt.getText(_columnIndexOfAudioPath)
          val _tmpSizeBytes: Long
          _tmpSizeBytes = _stmt.getLong(_columnIndexOfSizeBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpLanguageCode: String?
          if (_stmt.isNull(_columnIndexOfLanguageCode)) {
            _tmpLanguageCode = null
          } else {
            _tmpLanguageCode = _stmt.getText(_columnIndexOfLanguageCode)
          }
          val _tmpSummary: String?
          if (_stmt.isNull(_columnIndexOfSummary)) {
            _tmpSummary = null
          } else {
            _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          }
          val _tmpWaveform: String?
          if (_stmt.isNull(_columnIndexOfWaveform)) {
            _tmpWaveform = null
          } else {
            _tmpWaveform = _stmt.getText(_columnIndexOfWaveform)
          }
          _item =
              NoteEntity(_tmpId,_tmpTitle,_tmpCreatedAtMs,_tmpDurationMs,_tmpAudioPath,_tmpSizeBytes,_tmpStatus,_tmpLanguageCode,_tmpSummary,_tmpWaveform)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeNote(id: Long): Flow<NoteEntity?> {
    val _sql: String = "SELECT * FROM notes WHERE id = ?"
    return createFlow(__db, false, arrayOf("notes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfAudioPath: Int = getColumnIndexOrThrow(_stmt, "audioPath")
        val _columnIndexOfSizeBytes: Int = getColumnIndexOrThrow(_stmt, "sizeBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfLanguageCode: Int = getColumnIndexOrThrow(_stmt, "languageCode")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfWaveform: Int = getColumnIndexOrThrow(_stmt, "waveform")
        val _result: NoteEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpAudioPath: String
          _tmpAudioPath = _stmt.getText(_columnIndexOfAudioPath)
          val _tmpSizeBytes: Long
          _tmpSizeBytes = _stmt.getLong(_columnIndexOfSizeBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpLanguageCode: String?
          if (_stmt.isNull(_columnIndexOfLanguageCode)) {
            _tmpLanguageCode = null
          } else {
            _tmpLanguageCode = _stmt.getText(_columnIndexOfLanguageCode)
          }
          val _tmpSummary: String?
          if (_stmt.isNull(_columnIndexOfSummary)) {
            _tmpSummary = null
          } else {
            _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          }
          val _tmpWaveform: String?
          if (_stmt.isNull(_columnIndexOfWaveform)) {
            _tmpWaveform = null
          } else {
            _tmpWaveform = _stmt.getText(_columnIndexOfWaveform)
          }
          _result =
              NoteEntity(_tmpId,_tmpTitle,_tmpCreatedAtMs,_tmpDurationMs,_tmpAudioPath,_tmpSizeBytes,_tmpStatus,_tmpLanguageCode,_tmpSummary,_tmpWaveform)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun note(id: Long): NoteEntity? {
    val _sql: String = "SELECT * FROM notes WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfAudioPath: Int = getColumnIndexOrThrow(_stmt, "audioPath")
        val _columnIndexOfSizeBytes: Int = getColumnIndexOrThrow(_stmt, "sizeBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfLanguageCode: Int = getColumnIndexOrThrow(_stmt, "languageCode")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfWaveform: Int = getColumnIndexOrThrow(_stmt, "waveform")
        val _result: NoteEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpAudioPath: String
          _tmpAudioPath = _stmt.getText(_columnIndexOfAudioPath)
          val _tmpSizeBytes: Long
          _tmpSizeBytes = _stmt.getLong(_columnIndexOfSizeBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpLanguageCode: String?
          if (_stmt.isNull(_columnIndexOfLanguageCode)) {
            _tmpLanguageCode = null
          } else {
            _tmpLanguageCode = _stmt.getText(_columnIndexOfLanguageCode)
          }
          val _tmpSummary: String?
          if (_stmt.isNull(_columnIndexOfSummary)) {
            _tmpSummary = null
          } else {
            _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          }
          val _tmpWaveform: String?
          if (_stmt.isNull(_columnIndexOfWaveform)) {
            _tmpWaveform = null
          } else {
            _tmpWaveform = _stmt.getText(_columnIndexOfWaveform)
          }
          _result =
              NoteEntity(_tmpId,_tmpTitle,_tmpCreatedAtMs,_tmpDurationMs,_tmpAudioPath,_tmpSizeBytes,_tmpStatus,_tmpLanguageCode,_tmpSummary,_tmpWaveform)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun notesByStatuses(statuses: List<String>): List<NoteEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM notes WHERE status IN (")
    val _inputSize: Int = statuses.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in statuses) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfAudioPath: Int = getColumnIndexOrThrow(_stmt, "audioPath")
        val _columnIndexOfSizeBytes: Int = getColumnIndexOrThrow(_stmt, "sizeBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfLanguageCode: Int = getColumnIndexOrThrow(_stmt, "languageCode")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfWaveform: Int = getColumnIndexOrThrow(_stmt, "waveform")
        val _result: MutableList<NoteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: NoteEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpAudioPath: String
          _tmpAudioPath = _stmt.getText(_columnIndexOfAudioPath)
          val _tmpSizeBytes: Long
          _tmpSizeBytes = _stmt.getLong(_columnIndexOfSizeBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpLanguageCode: String?
          if (_stmt.isNull(_columnIndexOfLanguageCode)) {
            _tmpLanguageCode = null
          } else {
            _tmpLanguageCode = _stmt.getText(_columnIndexOfLanguageCode)
          }
          val _tmpSummary: String?
          if (_stmt.isNull(_columnIndexOfSummary)) {
            _tmpSummary = null
          } else {
            _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          }
          val _tmpWaveform: String?
          if (_stmt.isNull(_columnIndexOfWaveform)) {
            _tmpWaveform = null
          } else {
            _tmpWaveform = _stmt.getText(_columnIndexOfWaveform)
          }
          _item_1 =
              NoteEntity(_tmpId,_tmpTitle,_tmpCreatedAtMs,_tmpDurationMs,_tmpAudioPath,_tmpSizeBytes,_tmpStatus,_tmpLanguageCode,_tmpSummary,_tmpWaveform)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun searchNotes(raw: String, match: String): Flow<List<NoteEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM notes WHERE
        |            title LIKE '%' || ? || '%'
        |            OR id IN (
        |                SELECT noteId FROM transcript_segments WHERE id IN (
        |                    SELECT rowid FROM transcript_fts WHERE transcript_fts MATCH ?
        |                )
        |            )
        |        ORDER BY createdAtMs DESC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("notes", "transcript_segments", "transcript_fts")) {
        _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, raw)
        _argIndex = 2
        _stmt.bindText(_argIndex, match)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfAudioPath: Int = getColumnIndexOrThrow(_stmt, "audioPath")
        val _columnIndexOfSizeBytes: Int = getColumnIndexOrThrow(_stmt, "sizeBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfLanguageCode: Int = getColumnIndexOrThrow(_stmt, "languageCode")
        val _columnIndexOfSummary: Int = getColumnIndexOrThrow(_stmt, "summary")
        val _columnIndexOfWaveform: Int = getColumnIndexOrThrow(_stmt, "waveform")
        val _result: MutableList<NoteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NoteEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpAudioPath: String
          _tmpAudioPath = _stmt.getText(_columnIndexOfAudioPath)
          val _tmpSizeBytes: Long
          _tmpSizeBytes = _stmt.getLong(_columnIndexOfSizeBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpLanguageCode: String?
          if (_stmt.isNull(_columnIndexOfLanguageCode)) {
            _tmpLanguageCode = null
          } else {
            _tmpLanguageCode = _stmt.getText(_columnIndexOfLanguageCode)
          }
          val _tmpSummary: String?
          if (_stmt.isNull(_columnIndexOfSummary)) {
            _tmpSummary = null
          } else {
            _tmpSummary = _stmt.getText(_columnIndexOfSummary)
          }
          val _tmpWaveform: String?
          if (_stmt.isNull(_columnIndexOfWaveform)) {
            _tmpWaveform = null
          } else {
            _tmpWaveform = _stmt.getText(_columnIndexOfWaveform)
          }
          _item =
              NoteEntity(_tmpId,_tmpTitle,_tmpCreatedAtMs,_tmpDurationMs,_tmpAudioPath,_tmpSizeBytes,_tmpStatus,_tmpLanguageCode,_tmpSummary,_tmpWaveform)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSegments(noteId: Long): Flow<List<TranscriptSegmentEntity>> {
    val _sql: String = "SELECT * FROM transcript_segments WHERE noteId = ? ORDER BY idx"
    return createFlow(__db, false, arrayOf("transcript_segments")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, noteId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfNoteId: Int = getColumnIndexOrThrow(_stmt, "noteId")
        val _columnIndexOfIdx: Int = getColumnIndexOrThrow(_stmt, "idx")
        val _columnIndexOfStartMs: Int = getColumnIndexOrThrow(_stmt, "startMs")
        val _columnIndexOfEndMs: Int = getColumnIndexOrThrow(_stmt, "endMs")
        val _columnIndexOfText: Int = getColumnIndexOrThrow(_stmt, "text")
        val _result: MutableList<TranscriptSegmentEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TranscriptSegmentEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpNoteId: Long
          _tmpNoteId = _stmt.getLong(_columnIndexOfNoteId)
          val _tmpIdx: Int
          _tmpIdx = _stmt.getLong(_columnIndexOfIdx).toInt()
          val _tmpStartMs: Long
          _tmpStartMs = _stmt.getLong(_columnIndexOfStartMs)
          val _tmpEndMs: Long
          _tmpEndMs = _stmt.getLong(_columnIndexOfEndMs)
          val _tmpText: String
          _tmpText = _stmt.getText(_columnIndexOfText)
          _item = TranscriptSegmentEntity(_tmpId,_tmpNoteId,_tmpIdx,_tmpStartMs,_tmpEndMs,_tmpText)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeActionItems(noteId: Long): Flow<List<ActionItemEntity>> {
    val _sql: String =
        "SELECT * FROM action_items WHERE noteId = ? ORDER BY done ASC, createdAtMs ASC"
    return createFlow(__db, false, arrayOf("action_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, noteId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfNoteId: Int = getColumnIndexOrThrow(_stmt, "noteId")
        val _columnIndexOfText: Int = getColumnIndexOrThrow(_stmt, "text")
        val _columnIndexOfDone: Int = getColumnIndexOrThrow(_stmt, "done")
        val _columnIndexOfCreatedAtMs: Int = getColumnIndexOrThrow(_stmt, "createdAtMs")
        val _result: MutableList<ActionItemEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ActionItemEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpNoteId: Long
          _tmpNoteId = _stmt.getLong(_columnIndexOfNoteId)
          val _tmpText: String
          _tmpText = _stmt.getText(_columnIndexOfText)
          val _tmpDone: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDone).toInt()
          _tmpDone = _tmp != 0
          val _tmpCreatedAtMs: Long
          _tmpCreatedAtMs = _stmt.getLong(_columnIndexOfCreatedAtMs)
          _item = ActionItemEntity(_tmpId,_tmpNoteId,_tmpText,_tmpDone,_tmpCreatedAtMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateTitle(id: Long, title: String) {
    val _sql: String = "UPDATE notes SET title = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, title)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(id: Long, status: String) {
    val _sql: String = "UPDATE notes SET status = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateWaveform(id: Long, waveform: String) {
    val _sql: String = "UPDATE notes SET waveform = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, waveform)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteNote(id: Long) {
    val _sql: String = "DELETE FROM notes WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSegments(noteId: Long) {
    val _sql: String = "DELETE FROM transcript_segments WHERE noteId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, noteId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setActionItemDone(id: Long, done: Boolean) {
    val _sql: String = "UPDATE action_items SET done = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (done) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteActionItem(id: Long) {
    val _sql: String = "DELETE FROM action_items WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteActionItemsForNote(noteId: Long) {
    val _sql: String = "DELETE FROM action_items WHERE noteId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, noteId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
