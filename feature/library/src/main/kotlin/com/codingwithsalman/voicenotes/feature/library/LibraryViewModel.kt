package com.codingwithsalman.voicenotes.feature.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.common.di.IoDispatcher
import com.codingwithsalman.voicenotes.core.common.util.formatNoteDate
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.codingwithsalman.voicenotes.core.reminders.ActionItemBackfiller
import com.codingwithsalman.voicenotes.core.reminders.BackfillResult
import com.codingwithsalman.voicenotes.core.media.AudioProbe
import com.codingwithsalman.voicenotes.core.media.RecordingsStorage
import com.codingwithsalman.voicenotes.core.media.WaveformExtractor
import com.codingwithsalman.voicenotes.core.model.Note
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NotesRepository,
    private val storage: RecordingsStorage,
    private val audioProbe: AudioProbe,
    private val waveformExtractor: WaveformExtractor,
    private val transcriptionCoordinator: TranscriptionCoordinator,
    private val settings: SettingsRepository,
    private val backfiller: ActionItemBackfiller,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val notes: StateFlow<List<Note>> = _query
        .debounce { if (it.isBlank()) 0L else 150L }
        .flatMapLatest(repository::searchNotes)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * State of the one-time "action items are new" card.
     *
     * Shown only to someone who **has notes but has never had one extracted from** — that is
     * precisely the user who upgraded into 2.3.1 and whose library predates the feature. A fresh
     * install extracts on its first transcript and so never qualifies, which is why the gate is
     * "no extracted items exist" rather than a version check.
     */
    sealed interface Highlight {
        data object Hidden : Highlight
        data object Offer : Highlight
        data object Running : Highlight
        data class Done(val result: BackfillResult) : Highlight
    }

    private val _highlight = MutableStateFlow<Highlight>(Highlight.Hidden)
    val highlight: StateFlow<Highlight> = _highlight.asStateFlow()

    init {
        // Purge notes whose undo window elapsed (or was lost to process death) before we list.
        viewModelScope.launch { repository.purgeDeleted() }
        viewModelScope.launch {
            if (settings.autoTasksHighlightSeen.first()) return@launch
            if (!settings.autoTasksEnabled.first()) return@launch
            val hasHistory = repository.notesByStatuses(TranscriptionStatus.DONE).isNotEmpty()
            if (hasHistory && repository.extractedActionItemCount() == 0) {
                _highlight.value = Highlight.Offer
            }
        }
    }

    /** Runs extraction across the existing library, then reports what it found in place. */
    fun runBackfill() {
        if (_highlight.value != Highlight.Offer) return
        _highlight.value = Highlight.Running
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { backfiller.run() }
            settings.setAutoTasksHighlightSeen()
            _highlight.value = Highlight.Done(result)
        }
    }

    fun dismissHighlight() {
        viewModelScope.launch { settings.setAutoTasksHighlightSeen() }
        _highlight.value = Highlight.Hidden
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Soft delete — the note vanishes from the list immediately but Undo (or timeout) decides
     *  its fate. The screen shows a snackbar and calls [undoDelete] or [finalizeDelete]. */
    fun delete(note: Note) {
        viewModelScope.launch { repository.softDelete(note.id) }
    }

    fun undoDelete(noteId: Long) {
        viewModelScope.launch { repository.restore(noteId) }
    }

    fun finalizeDelete(noteId: Long) {
        viewModelScope.launch { repository.purge(noteId) }
    }

    /** Copies a SAF-picked audio file into app storage and creates a note for it. */
    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            val noteId = withContext(ioDispatcher) {
                runCatching {
                    val displayName = queryDisplayName(uri)
                    val extension = displayName?.substringAfterLast('.', "")
                        ?.takeIf(String::isNotEmpty)
                        ?: MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(context.contentResolver.getType(uri))
                        ?: "m4a"
                    val destination = storage.newImportFile(extension)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@runCatching null
                    val now = System.currentTimeMillis()
                    repository.createNote(
                        Note(
                            title = displayName?.substringBeforeLast('.')
                                ?.takeIf(String::isNotEmpty)
                                ?: context.getString(
                                    com.codingwithsalman.voicenotes.core.designsystem.R.string.vn_imported_default_title,
                                    formatNoteDate(context, now),
                                ),
                            createdAtMs = now,
                            durationMs = audioProbe.durationMs(destination),
                            audioPath = destination.absolutePath,
                            sizeBytes = destination.length(),
                            status = TranscriptionStatus.RECORDED,
                        )
                    )
                }.getOrNull()
            }
            noteId?.let { id ->
                transcriptionCoordinator.requestTranscription(id)
                // Envelope extraction decodes the file — do it off the critical path.
                viewModelScope.launch(ioDispatcher) {
                    repository.note(id)?.let { note ->
                        repository.setWaveform(id, waveformExtractor.extract(File(note.audioPath)))
                    }
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
}
