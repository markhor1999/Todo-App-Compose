package com.codingwithsalman.voicenotes.feature.note

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.common.di.IoDispatcher
import com.codingwithsalman.voicenotes.core.billing.BillingRepository
import com.codingwithsalman.voicenotes.core.designsystem.R as DsR
import com.codingwithsalman.voicenotes.core.billing.ProPricing
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import com.codingwithsalman.voicenotes.core.media.AudioPlayerController
import com.codingwithsalman.voicenotes.core.model.ActionItem
import com.codingwithsalman.voicenotes.core.model.Note
import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: NotesRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator,
    private val entitlementStore: EntitlementStore,
    private val billing: BillingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val player: AudioPlayerController,
) : ViewModel() {

    private val noteId: Long = checkNotNull(savedStateHandle["noteId"])

    val note: StateFlow<Note?> = repository.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val segments: StateFlow<List<TranscriptSegment>> = repository.observeSegments(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val actionItems: StateFlow<List<ActionItem>> = repository.observeActionItems(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val engineState: StateFlow<EngineState> = transcriptionCoordinator.engineState

    val transcriptionProgress: StateFlow<Float?> = transcriptionCoordinator.progressByNote
        .map { it[noteId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    val isPro: StateFlow<Boolean> = entitlementStore.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val remainingTodayMs: StateFlow<Long> = entitlementStore.remainingTodayMs
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EntitlementStore.FREE_DAILY_MS,
        )

    val pricing: StateFlow<ProPricing> = billing.pricing

    fun launchWeekly(activity: android.app.Activity) = billing.launchWeekly(activity)

    fun launchMonthly(activity: android.app.Activity) = billing.launchMonthly(activity)

    fun launchLifetime(activity: android.app.Activity) = billing.launchLifetime(activity)

    fun restorePurchases() = billing.restorePurchases()

    init {
        viewModelScope.launch {
            val loaded = note.filterNotNull().first()
            player.load(loaded.audioPath)
        }
    }

    fun playPause() = player.playPause()

    fun seekToFraction(fraction: Float) = player.seekToFraction(fraction)

    fun seekToMs(positionMs: Long) = player.seekToMs(positionMs)

    fun cyclePlaybackSpeed() {
        val next = when (_playbackSpeed.value) {
            1f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
        _playbackSpeed.value = next
        player.setSpeed(next)
    }

    fun downloadModel() = transcriptionCoordinator.ensureModel()

    fun transcribe() = transcriptionCoordinator.requestTranscription(noteId)

    fun rename(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.updateTitle(noteId, trimmed) }
    }

    fun addActionItem(text: String) {
        viewModelScope.launch { repository.addActionItem(noteId, text) }
    }

    fun toggleActionItem(item: ActionItem) {
        viewModelScope.launch { repository.setActionItemDone(item.id, !item.done) }
    }

    fun deleteActionItem(item: ActionItem) {
        viewModelScope.launch { repository.deleteActionItem(item.id) }
    }

    /** Share the transcript text via the system sheet. */
    fun shareTranscript() {
        val current = note.value ?: return
        val transcript = TranscriptFormats.plainText(segments.value)
        if (transcript.isBlank()) return
        // Tasteful attribution turns every shared transcript into a small, on-brand pointer back to
        // Murmur — a cheap discovery loop. Share only; exports (exportTo) stay clean.
        val body = "$transcript\n\n${context.getString(DsR.string.vn_share_attribution)}"
        startChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, current.title)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        )
    }

    /** Share the original audio file via FileProvider. */
    fun shareAudio() {
        val current = note.value ?: return
        val uri: Uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(current.audioPath),
            )
        }.getOrNull() ?: return
        startChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    /** Write the transcript to a user-picked SAF document. */
    fun exportTo(uri: Uri, format: ExportFormat) {
        val current = note.value ?: return
        val content = TranscriptFormats.render(current, segments.value, format)
        viewModelScope.launch {
            withContext(ioDispatcher) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(content.toByteArray())
                    }
                }
            }
        }
    }

    fun exportFileName(format: ExportFormat): String =
        note.value?.let { TranscriptFormats.fileName(it, format) } ?: "transcript.${format.extension}"

    private fun startChooser(intent: Intent) {
        context.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
