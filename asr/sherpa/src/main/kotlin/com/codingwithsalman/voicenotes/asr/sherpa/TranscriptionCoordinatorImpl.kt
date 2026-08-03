@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.asr.sherpa

import com.tricodestudio.voicekit.ModelStore
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tricodestudio.voicekit.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.tricodestudio.voicekit.ModelCatalog
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

    // Observes the WorkManager download for the current not-installed model. Re-scoped on model
    // change; also picks up a download already running after a process restart.
    private var downloadWatchJob: Job? = null

    init {
        scope.launch {
            settings.modelId.map(ModelCatalog::byId).collect { spec ->
                downloadWatchJob?.cancel()
                if (modelStore.isInstalled(spec)) {
                    _engineState.value = EngineState.Ready(spec)
                    requeueUnfinished()
                } else {
                    _engineState.value = EngineState.NotInstalled(spec)
                    downloadWatchJob = scope.launch { watchDownload(spec) }
                }
            }
        }
    }

    private suspend fun requeueUnfinished() {
        repository
            .notesByStatuses(TranscriptionStatus.QUEUED, TranscriptionStatus.TRANSCRIBING)
            .forEach { note -> enqueue(note.id) }
    }

    /** Mirror the download worker's WorkInfo into EngineState. isInstalled is the source of truth
     *  for terminal states, so a stale SUCCEEDED for a different model can't fake readiness. */
    private suspend fun watchDownload(spec: AsrModelSpec) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.UNIQUE_WORK)
            .collect { infos ->
                when (infos.firstOrNull()?.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                        val info = infos.first()
                        val p = info.progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                        _engineState.value = EngineState.Downloading(spec, p)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        if (modelStore.isInstalled(spec)) {
                            _engineState.value = EngineState.Ready(spec)
                            requeueUnfinished()
                        } else {
                            _engineState.value = EngineState.NotInstalled(spec)
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val msg = infos.first().outputData
                            .getString(ModelDownloadWorker.KEY_ERROR) ?: "Download failed"
                        if (modelStore.isInstalled(spec)) _engineState.value = EngineState.Ready(spec)
                        else _engineState.value = EngineState.DownloadFailed(spec, msg)
                    }
                    WorkInfo.State.CANCELLED, null -> Unit // no active download; keep NotInstalled
                }
            }
    }

    override fun ensureModel() {
        if (_engineState.value is EngineState.Ready) return
        scope.launch {
            val spec = ModelCatalog.byId(settings.modelId.first())
            if (modelStore.isInstalled(spec)) {
                _engineState.value = EngineState.Ready(spec)
                requeueUnfinished()
                return@launch
            }
            _engineState.value = EngineState.Downloading(spec, 0f)
            WorkManager.getInstance(context).enqueueUniqueWork(
                ModelDownloadWorker.UNIQUE_WORK,
                ExistingWorkPolicy.KEEP, // a download already running stays; finished record is replaced
                OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                    .setInputData(workDataOf(ModelDownloadWorker.KEY_MODEL_ID to spec.id))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build(),
            )
            downloadWatchJob?.cancel()
            downloadWatchJob = scope.launch { watchDownload(spec) }
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
