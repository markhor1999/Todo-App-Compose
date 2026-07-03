package com.codingwithsalman.voicenotes.asr.sherpa

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.asr.api.ModelCatalog
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Front door for transcription: owns model download state (for the user-selected
 * model) and hands jobs to WorkManager (unique work per note → survives process
 * death, no double-runs). Selecting a different model in settings re-derives the
 * engine state reactively.
 */
@Singleton
class TranscriptionCoordinatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelStore: ModelStore,
    private val repository: NotesRepository,
    private val settings: SettingsRepository,
    private val entitlementStore: EntitlementStore,
    progressBus: TranscriptionProgressBus,
) : TranscriptionCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _engineState =
        MutableStateFlow<EngineState>(EngineState.NotInstalled(ModelCatalog.default))
    override val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    override val progressByNote: StateFlow<Map<Long, Float>> = progressBus.progressByNote

    private var downloadJob: Job? = null

    init {
        scope.launch {
            settings.modelId.map(ModelCatalog::byId).collect { spec ->
                downloadJob?.cancel()
                _engineState.value =
                    if (modelStore.isInstalled(spec)) EngineState.Ready(spec)
                    else EngineState.NotInstalled(spec)
                if (_engineState.value is EngineState.Ready) requeueUnfinished()
            }
        }
    }

    private suspend fun requeueUnfinished() {
        repository
            .notesByStatuses(TranscriptionStatus.QUEUED, TranscriptionStatus.TRANSCRIBING)
            .forEach { note -> enqueue(note.id) }
    }

    override fun ensureModel() {
        if (_engineState.value is EngineState.Ready || downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            val spec = ModelCatalog.byId(settings.modelId.first())
            _engineState.value = EngineState.Downloading(spec, 0f)
            runCatching {
                modelStore.download(spec).collect { progress ->
                    _engineState.value = EngineState.Downloading(spec, progress)
                }
            }.onSuccess {
                if (modelStore.isInstalled(spec)) {
                    _engineState.value = EngineState.Ready(spec)
                    // Notes recorded/imported while the model was still downloading.
                    requeueUnfinished()
                } else {
                    _engineState.value =
                        EngineState.DownloadFailed(spec, "Files incomplete — try again")
                }
            }.onFailure { e ->
                _engineState.value =
                    EngineState.DownloadFailed(spec, e.message ?: "Download failed")
            }
        }
    }

    override fun requestTranscription(noteId: Long) {
        if (_engineState.value !is EngineState.Ready) return
        scope.launch {
            // Free-tier gate: note stays RECORDED when the daily meter is spent;
            // the note screen surfaces the paywall.
            if (!entitlementStore.canStartTranscription()) return@launch
            repository.updateStatus(noteId, TranscriptionStatus.QUEUED)
            enqueue(noteId)
        }
    }

    private fun enqueue(noteId: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            TranscriptionWorker.uniqueName(noteId),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(workDataOf(TranscriptionWorker.KEY_NOTE_ID to noteId))
                .build(),
        )
    }
}
