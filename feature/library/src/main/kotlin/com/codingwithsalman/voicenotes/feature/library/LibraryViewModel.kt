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

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun delete(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
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
                                    formatNoteDate(now),
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
