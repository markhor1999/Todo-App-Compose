package com.codingwithsalman.voicenotes.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import kotlin.math.sqrt

data class RecordingInfo(
    val file: File,
    val durationMs: Long,
    val sizeBytes: Long,
)

/**
 * Thin MediaRecorder wrapper producing mono AAC .m4a voice recordings.
 * One recording at a time; owned by the capture ViewModel (survives rotation there).
 */
class AudioRecorderController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    var isPaused: Boolean = false
        private set

    fun start(output: File) {
        check(recorder == null) { "Recording already in progress" }
        outputFile = output
        recorder = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(96_000)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }
        startedAtMs = System.currentTimeMillis()
        isPaused = false
        _isRecording.value = true
    }

    fun pause() {
        val r = recorder ?: return
        if (isPaused) return
        runCatching { r.pause() }.onSuccess { isPaused = true }
    }

    fun resume() {
        val r = recorder ?: return
        if (!isPaused) return
        runCatching { r.resume() }.onSuccess { isPaused = false }
    }

    /** Normalized 0..1 microphone level; sqrt curve so quiet speech still moves the UI. */
    fun amplitude(): Float {
        val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return sqrt(raw.coerceIn(0, 32767) / 32767f)
    }

    fun stop(): RecordingInfo? {
        val file = outputFile ?: return null
        val durationMs = System.currentTimeMillis() - startedAtMs
        releaseRecorder()
        _isRecording.value = false
        outputFile = null
        return RecordingInfo(file = file, durationMs = durationMs, sizeBytes = file.length())
    }

    /** Stops and deletes the file — the user discarded the take. */
    fun cancel() {
        val file = outputFile
        releaseRecorder()
        _isRecording.value = false
        outputFile = null
        file?.delete()
    }

    private fun releaseRecorder() {
        recorder?.let { r ->
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        recorder = null
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
