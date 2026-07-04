package com.codingwithsalman.voicenotes.feature.capture

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.common.util.formatNoteDate
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.media.AudioRecorderController
import com.codingwithsalman.voicenotes.core.media.RecordingsStorage
import com.codingwithsalman.voicenotes.core.media.WaveformExtractor
import com.codingwithsalman.voicenotes.core.model.Note
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RecordingSessionState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val startedAtMs: Long = 0L,
    /** Active (un-paused) recording time — what actually lands in the file. */
    val elapsedMs: Long = 0L,
    /** Rolling window for the live waveform (full history kept separately for the note). */
    val amplitudes: List<Float> = emptyList(),
)

sealed interface RecordingSessionEvent {
    data class Saved(val noteId: Long) : RecordingSessionEvent
    data object Discarded : RecordingSessionEvent
}

/**
 * Process-scoped owner of the active recording. Screens observe it; the
 * foreground service keeps the process (and microphone access) alive while the
 * user is elsewhere. Recording therefore survives navigation and backgrounding —
 * it ends only via Stop (screen or notification) or Cancel.
 */
@Singleton
class RecordingSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: AudioRecorderController,
    private val storage: RecordingsStorage,
    private val repository: NotesRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(RecordingSessionState())
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RecordingSessionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<RecordingSessionEvent> = _events.asSharedFlow()

    private var meterJob: Job? = null
    private val fullHistory = ArrayList<Float>(4096)

    // Pause-aware elapsed accounting: activeMs accumulates on pause; the live tail
    // is (now - lastResumedAt) while recording.
    private var accumulatedActiveMs = 0L
    private var lastResumedAtMs = 0L

    fun startRecording(): Boolean {
        if (_state.value.isRecording) return true
        val started = runCatching { recorder.start(storage.newRecordingFile()) }.isSuccess
        if (!started) return false

        val now = System.currentTimeMillis()
        fullHistory.clear()
        accumulatedActiveMs = 0L
        lastResumedAtMs = now
        _state.value = RecordingSessionState(isRecording = true, startedAtMs = now)

        context.startForegroundService(Intent(context, RecordingService::class.java))

        meterJob = scope.launch {
            while (true) {
                if (!_state.value.isPaused) {
                    val amplitude = recorder.amplitude()
                    fullHistory.add(amplitude)
                    _state.value = _state.value.copy(
                        elapsedMs = accumulatedActiveMs + (System.currentTimeMillis() - lastResumedAtMs),
                        amplitudes = (_state.value.amplitudes + amplitude).takeLast(LIVE_BARS),
                    )
                }
                delay(METER_PERIOD_MS)
            }
        }
        return true
    }

    fun pauseRecording() {
        if (!_state.value.isRecording || _state.value.isPaused) return
        recorder.pause()
        accumulatedActiveMs += System.currentTimeMillis() - lastResumedAtMs
        _state.value = _state.value.copy(isPaused = true, elapsedMs = accumulatedActiveMs)
        updateNotification()
    }

    fun resumeRecording() {
        if (!_state.value.isRecording || !_state.value.isPaused) return
        recorder.resume()
        lastResumedAtMs = System.currentTimeMillis()
        _state.value = _state.value.copy(isPaused = false)
        updateNotification()
    }

    fun stopAndSave() {
        if (!_state.value.isRecording) return
        meterJob?.cancel()
        val startedAt = _state.value.startedAtMs
        val activeMs = if (_state.value.isPaused) {
            accumulatedActiveMs
        } else {
            accumulatedActiveMs + (System.currentTimeMillis() - lastResumedAtMs)
        }
        val info = recorder.stop()
        val waveform = WaveformExtractor.downsamplePeaks(fullHistory.toList())
        endSession()
        if (info == null) {
            _events.tryEmit(RecordingSessionEvent.Discarded)
            return
        }
        scope.launch {
            val id = repository.createNote(
                Note(
                    title = context.getString(
                        com.codingwithsalman.voicenotes.core.designsystem.R.string.vn_note_default_title,
                        formatNoteDate(context, startedAt),
                    ),
                    createdAtMs = startedAt,
                    durationMs = activeMs,
                    audioPath = info.file.absolutePath,
                    sizeBytes = info.sizeBytes,
                    status = TranscriptionStatus.RECORDED,
                    waveform = waveform.takeIf(List<Float>::isNotEmpty),
                )
            )
            // No-op while the model isn't installed; the detail screen offers the download.
            transcriptionCoordinator.requestTranscription(id)
            _events.tryEmit(RecordingSessionEvent.Saved(id))
        }
    }

    fun discard() {
        if (!_state.value.isRecording) return
        meterJob?.cancel()
        recorder.cancel()
        endSession()
        _events.tryEmit(RecordingSessionEvent.Discarded)
    }

    private fun updateNotification() {
        val state = _state.value
        // Chronometer can't freeze, so the paused variant swaps to static text.
        // Best-effort: silently skipped if notifications are denied.
        runCatching {
            NotificationManagerCompat.from(context).notify(
                RecordingNotifications.NOTIFICATION_ID,
                RecordingNotifications.build(context, state.startedAtMs, state.isPaused),
            )
        }
    }

    private fun endSession() {
        _state.value = RecordingSessionState()
        context.stopService(Intent(context, RecordingService::class.java))
    }

    private companion object {
        const val METER_PERIOD_MS = 50L
        const val LIVE_BARS = 160
    }
}
