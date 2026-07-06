package com.codingwithsalman.voicenotes.feature.capture

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.codingwithsalman.voicenotes.asr.api.LiveSession
import com.codingwithsalman.voicenotes.asr.api.LiveTranscriptionManager
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.common.util.formatNoteDate
import com.codingwithsalman.voicenotes.core.database.NotesRepository
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.codingwithsalman.voicenotes.core.media.AudioRecorderController
import com.codingwithsalman.voicenotes.core.media.PcmAudioRecorder
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
import kotlinx.coroutines.withContext
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
    /** Provisional live-transcription text (v2.1 #2) — English-only preview, empty unless the
     *  live path is active. Thrown away on stop; the saved transcript comes from the offline pass. */
    val liveText: String = "",
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
 *
 * Two capture backends (chosen per recording at [startRecording]): the proven [AudioRecorderController]
 * (MediaRecorder) by default, or [PcmAudioRecorder] + a [LiveSession] when live transcription is
 * enabled and its model is installed — everyone else is unaffected by the v2.1 #2 feature.
 */
@Singleton
class RecordingSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: AudioRecorderController,
    private val pcmRecorder: PcmAudioRecorder,
    private val storage: RecordingsStorage,
    private val repository: NotesRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator,
    private val settings: SettingsRepository,
    private val liveManager: LiveTranscriptionManager,
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

    // Live-transcription path state (null/false unless the user opted in and the model is present).
    @Volatile private var liveEnabled = false
    private var usingLive = false
    private var liveSession: LiveSession? = null
    private var liveTextJob: Job? = null

    init {
        scope.launch { settings.liveTranscriptionEnabled.collect { liveEnabled = it } }
    }

    fun startRecording(): Boolean {
        if (_state.value.isRecording) return true
        val file = storage.newRecordingFile()

        val session = if (liveEnabled && liveManager.isAvailable()) liveManager.newSession() else null
        usingLive = session != null

        val started = if (usingLive) {
            runCatching { pcmRecorder.start(file) { samples -> session!!.accept(samples) } }.isSuccess
        } else {
            runCatching { recorder.start(file) }.isSuccess
        }
        if (!started) {
            session?.release()
            usingLive = false
            return false
        }
        liveSession = session

        val now = System.currentTimeMillis()
        fullHistory.clear()
        accumulatedActiveMs = 0L
        lastResumedAtMs = now
        _state.value = RecordingSessionState(isRecording = true, startedAtMs = now)

        context.startForegroundService(Intent(context, RecordingService::class.java))

        liveTextJob = session?.let { s ->
            scope.launch { s.text.collect { t -> _state.value = _state.value.copy(liveText = t) } }
        }

        meterJob = scope.launch {
            while (true) {
                if (!_state.value.isPaused) {
                    val amplitude = if (usingLive) pcmRecorder.amplitude() else recorder.amplitude()
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
        if (usingLive) pcmRecorder.pause() else recorder.pause()
        accumulatedActiveMs += System.currentTimeMillis() - lastResumedAtMs
        _state.value = _state.value.copy(isPaused = true, elapsedMs = accumulatedActiveMs)
        updateNotification()
    }

    fun resumeRecording() {
        if (!_state.value.isRecording || !_state.value.isPaused) return
        if (usingLive) pcmRecorder.resume() else recorder.resume()
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
        val wasLive = usingLive
        val session = liveSession
        val historySnapshot = fullHistory.toList()
        // Reset the UI immediately (screen navigates away); tear down native resources off-thread —
        // the PCM recorder's stop() joins its capture thread to flush the encoder.
        resetToIdle()
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                val i = if (wasLive) pcmRecorder.stop() else recorder.stop()
                session?.release()
                i
            }
            if (info == null) {
                _events.tryEmit(RecordingSessionEvent.Discarded)
                return@launch
            }
            val waveform = WaveformExtractor.downsamplePeaks(historySnapshot)
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
        val wasLive = usingLive
        val session = liveSession
        resetToIdle()
        scope.launch {
            withContext(Dispatchers.IO) {
                if (wasLive) pcmRecorder.cancel() else recorder.cancel()
                session?.release()
            }
        }
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

    /** Clear session UI state + stop the foreground service. Native teardown happens separately. */
    private fun resetToIdle() {
        liveTextJob?.cancel()
        liveTextJob = null
        liveSession = null
        usingLive = false
        _state.value = RecordingSessionState()
        context.stopService(Intent(context, RecordingService::class.java))
    }

    private companion object {
        const val METER_PERIOD_MS = 50L
        const val LIVE_BARS = 160
    }
}
