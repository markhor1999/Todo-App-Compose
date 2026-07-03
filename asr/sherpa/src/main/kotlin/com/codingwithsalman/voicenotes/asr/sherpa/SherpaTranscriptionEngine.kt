package com.codingwithsalman.voicenotes.asr.sherpa

import com.codingwithsalman.voicenotes.asr.api.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.ModelFileRole
import com.codingwithsalman.voicenotes.asr.api.SegmentResult
import com.codingwithsalman.voicenotes.asr.api.TranscriptionResult
import com.codingwithsalman.voicenotes.core.common.di.DefaultDispatcher
import com.codingwithsalman.voicenotes.core.media.AudioDecoder
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Whisper via sherpa-onnx: decode → 16 kHz mono floats → silero-VAD
 * speech segments (capped under Whisper's 30 s window) → one OfflineRecognizer
 * decode per segment → segments with absolute timestamps. Fully offline.
 */
@Singleton
class SherpaTranscriptionEngine @Inject constructor(
    private val modelStore: ModelStore,
    private val audioDecoder: AudioDecoder,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    fun isReady(spec: AsrModelSpec): Boolean = modelStore.isInstalled(spec)

    // One recognizer at a time, process-wide: two concurrent Whisper instances
    // roughly double native heap (~1 GB) and thrash the CPU. Parallel workers
    // simply queue here.
    private val transcribeMutex = Mutex()

    suspend fun transcribe(
        audioFile: File,
        spec: AsrModelSpec,
        onProgress: (Float) -> Unit = {},
    ): TranscriptionResult = transcribeMutex.withLock {
        transcribeLocked(audioFile, spec, onProgress)
    }

    private suspend fun transcribeLocked(
        audioFile: File,
        spec: AsrModelSpec,
        onProgress: (Float) -> Unit,
    ): TranscriptionResult = withContext(defaultDispatcher) {
        // Phase 1 — decode (0.00..0.10 of overall progress).
        val samples = audioDecoder.decodeToMono16k(audioFile) { p ->
            onProgress(p * DECODE_SHARE)
        }
        onProgress(DECODE_SHARE)
        if (samples.isEmpty()) return@withContext TranscriptionResult(emptyList(), spec.languageParam.ifEmpty { null })

        val recognizer = OfflineRecognizer(
            assetManager = null,
            config = OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = modelStore.localFile(spec, spec.file(ModelFileRole.ENCODER)).absolutePath,
                        decoder = modelStore.localFile(spec, spec.file(ModelFileRole.DECODER)).absolutePath,
                        language = spec.languageParam,
                        task = "transcribe",
                    ),
                    tokens = modelStore.localFile(spec, spec.file(ModelFileRole.TOKENS)).absolutePath,
                    modelType = "whisper",
                    numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
                ),
            ),
        )
        val vad = Vad(
            assetManager = null,
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = modelStore.vadLocalFile().absolutePath,
                    threshold = 0.5f,
                    minSilenceDuration = 0.35f,
                    minSpeechDuration = 0.25f,
                    windowSize = 512,
                    // Keep segments under Whisper's 30 s context window.
                    maxSpeechDuration = 28f,
                ),
                sampleRate = SAMPLE_RATE,
            ),
        )

        try {
            val segments = mutableListOf<SegmentResult>()

            fun drainVad() {
                while (!vad.empty()) {
                    val speech = vad.front()
                    vad.pop()
                    val stream = recognizer.createStream()
                    try {
                        stream.acceptWaveform(speech.samples, SAMPLE_RATE)
                        recognizer.decode(stream)
                        val text = recognizer.getResult(stream).text.trim()
                        if (text.isNotEmpty()) {
                            val startMs = speech.start * 1000L / SAMPLE_RATE
                            val endMs = startMs + speech.samples.size * 1000L / SAMPLE_RATE
                            segments += SegmentResult(startMs = startMs, endMs = endMs, text = text)
                        }
                    } finally {
                        stream.release()
                    }
                }
            }

            var offset = 0
            while (offset < samples.size) {
                val window = samples.copyOfRange(
                    offset,
                    (offset + VAD_WINDOW).coerceAtMost(samples.size),
                )
                vad.acceptWaveform(window)
                offset += VAD_WINDOW
                drainVad()
                onProgress(
                    DECODE_SHARE +
                        (1f - DECODE_SHARE) * (offset.toFloat() / samples.size).coerceIn(0f, 1f)
                )
            }
            vad.flush()
            drainVad()
            onProgress(1f)

            TranscriptionResult(
                segments = segments,
                languageCode = spec.languageParam.ifEmpty { null },
            )
        } finally {
            vad.release()
            recognizer.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val VAD_WINDOW = 512
        const val DECODE_SHARE = 0.10f
    }
}
