package com.codingwithsalman.voicenotes.asr.sherpa

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.codingwithsalman.voicenotes.asr.api.ModelCatalog
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/**
 * Downloads a speech model (+ shared VAD) under WorkManager so it survives process death — a killed
 * download is re-run automatically and resumes from the `.part` files, with no user re-tap. Reports
 * byte progress via setProgress; the coordinator maps it to EngineState.Downloading.
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val modelStore: ModelStore,
    private val settings: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val spec = ModelCatalog.byId(inputData.getString(KEY_MODEL_ID) ?: settings.modelId.first())
        if (modelStore.isInstalled(spec)) return Result.success()
        return try {
            modelStore.download(spec).collect { progress ->
                setProgress(workDataOf(KEY_PROGRESS to progress))
            }
            if (modelStore.isInstalled(spec)) Result.success()
            else Result.failure(workDataOf(KEY_ERROR to "Files incomplete — try again"))
        } catch (e: CancellationException) {
            throw e // interrupted (process death / model switch): let WorkManager reschedule
        } catch (e: Exception) {
            Log.e(TAG, "model download failed for ${spec.id}", e)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
        }
    }

    companion object {
        const val KEY_MODEL_ID = "modelId"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val UNIQUE_WORK = "model_download"
        const val TAG = "VnModelDownload"
    }
}
