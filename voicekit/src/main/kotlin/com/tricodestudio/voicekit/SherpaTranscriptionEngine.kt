package com.tricodestudio.voicekit

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device Whisper via sherpa-onnx: decode → 16 kHz mono floats → silero-VAD
 * speech segments (capped under Whisper's 30 s window) → one OfflineRecognizer
 * decode per segment → segments with absolute timestamps. Fully offline.
 */
@VoiceKitInternalApi
public class SherpaTranscriptionEngine constructor(
    private val modelStore: ModelStore,
    private val audioDecoder: AudioDecoder,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /**
     * Gate on what this hardware can survive. Defaults to [DeviceCapabilities.UNRESTRICTED] so JVM
     * tests and non-Android callers are unaffected; the app's DI passes the real probe.
     */
    private val capabilities: DeviceCapabilities = DeviceCapabilities.UNRESTRICTED,
) {

    /**
     * Installed **and** loadable here.
     *
     * The capability half is not belt-and-braces: on a device with no 64-bit ABI the native load
     * raises `SIGBUS`, which no `try`/`catch` can contain, so refusing before the call is the only
     * available defence. See [DeviceCapabilities].
     */
    fun isReady(spec: AsrModelSpec): Boolean =
        capabilities.fits(spec) && modelStore.isInstalled(spec)

    /** Why [isReady] said no on capability grounds, for callers that need to explain themselves. */
    fun unsupportedReason(spec: AsrModelSpec): String? = capabilities.refusalReason(spec)

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
        // Refuse before anything native happens: a bad load is a SIGBUS, not an exception, and
        // would take the whole process with it (2026-09-01 crash cluster).
        unsupportedReason(spec)?.let { throw UnsupportedDeviceException(it) }

        // The VAD is small; the recogniser is roughly a gigabyte of native heap, so it is built on
        // first speech instead of up front — a silent or empty recording never pays for one.
        var recognizer: OfflineRecognizer? = null
        val vad = Vad(
            assetManager = null,
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = modelStore.vadLocalFile().absolutePath,
                    threshold = 0.5f,
                    minSilenceDuration = 0.35f,
                    minSpeechDuration = 0.25f,
                    windowSize = VAD_WINDOW,
                    // Keep segments under Whisper's 30 s context window.
                    maxSpeechDuration = 28f,
                ),
                sampleRate = SAMPLE_RATE,
            ),
        )

        try {
            val segments = mutableListOf<Segment>()

            fun drainVad() {
                while (!vad.empty()) {
                    val speech = vad.front()
                    vad.pop()
                    val r = recognizer
                        ?: newRecognizer(spec).also { recognizer = it }
                    val stream = r.createStream()
                    try {
                        stream.acceptWaveform(speech.samples, SAMPLE_RATE)
                        r.decode(stream)
                        val text = r.getResult(stream).text.trim()
                        if (text.isNotEmpty()) {
                            val startMs = speech.start * 1000L / SAMPLE_RATE
                            val endMs = startMs + speech.samples.size * 1000L / SAMPLE_RATE
                            segments += Segment(startMs = startMs, endMs = endMs, text = text)
                        }
                    } finally {
                        stream.release()
                    }
                }
            }

            // Decode and recognise in one pass. The decoder hands back 16 kHz mono chunks as the
            // codec produces them, so peak memory is one codec buffer rather than the whole
            // recording twice over; `pending` carries the sub-window remainder between chunks.
            var pending = FloatArray(0)
            val totalSamples = audioDecoder.decodeStreamingMono16k(
                file = audioFile,
                onProgress = { p -> onProgress(p.coerceIn(0f, 1f)) },
            ) { chunk ->
                val buf = if (pending.isEmpty()) chunk else pending + chunk
                var offset = 0
                while (offset + VAD_WINDOW <= buf.size) {
                    vad.acceptWaveform(buf.copyOfRange(offset, offset + VAD_WINDOW))
                    offset += VAD_WINDOW
                    drainVad()
                }
                pending = if (offset == 0) buf else buf.copyOfRange(offset, buf.size)
            }

            if (totalSamples == 0L) {
                return@withContext TranscriptionResult(
                    segments = emptyList(),
                    language = spec.languageParam.ifEmpty { null },
                    durationMs = 0,
                )
            }

            // Whatever did not fill a window still has to reach the VAD, exactly as the batch
            // implementation's short final window did.
            if (pending.isNotEmpty()) {
                vad.acceptWaveform(pending)
                drainVad()
            }
            vad.flush()
            drainVad()
            onProgress(1f)

            TranscriptionResult(
                segments = segments,
                language = spec.languageParam.ifEmpty { null },
                // Length of what was actually decoded, not what the container claims.
                durationMs = totalSamples * 1000L / AudioDecoder.TARGET_SAMPLE_RATE,
            )
        } finally {
            vad.release()
            recognizer?.release()
        }
    }

    // --- Live-preview support (v2.1 #2, chunked multilingual path) -------------------------------
    // The live session runs its own recognizer+VAD against the mic feed. It must respect the same
    // one-native-instance invariant as transcribe(), so it try-locks the same mutex for its whole
    // session: an in-flight background job wins (no live preview that take, MediaRecorder fallback);
    // a job arriving mid-recording simply suspends in withLock until the session releases.

    /**
     * Hold the inference slot for the duration of [block].
     *
     * Exists for the case where the actual inference happens in **another process** (Murmur runs
     * the saved-transcript pass in `:asr`). The mutex below only serialises this process, so once
     * transcription moved out, nothing stopped a live preview here and a transcription there from
     * running at the same moment — roughly a gigabyte of native heap each, on the 2 GB phones this
     * whole exercise is about. Wrapping the remote call in the local slot restores the invariant:
     * whoever holds it, wherever the work runs, there is one at a time.
     */
    public suspend fun <T> withInferenceSlot(block: suspend () -> T): T =
        transcribeMutex.withLock { block() }

    /** Non-blocking claim of the process-wide inference slot for a live session. */
    public fun tryLockForLiveSession(): Boolean = transcribeMutex.tryLock()

    /** Release the live session's claim. Safe to call once after a successful [tryLockForLiveSession]. */
    public fun unlockLiveSession() {
        runCatching { transcribeMutex.unlock() }
    }

    /** A recognizer for [spec] built exactly like the offline pass builds one. Caller releases. */
    public fun newRecognizer(spec: AsrModelSpec): OfflineRecognizer {
        unsupportedReason(spec)?.let { throw UnsupportedDeviceException(it) }
        return OfflineRecognizer(
            assetManager = null,
            config = OfflineRecognizerConfig(modelConfig = buildModelConfig(spec)),
        )
    }

    /** A silero VAD tuned for live preview: shorter max segment so text commits frequently. */
    public fun newLiveVad(): Vad = Vad(
        assetManager = null,
        config = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = modelStore.vadLocalFile().absolutePath,
                threshold = 0.5f,
                minSilenceDuration = 0.4f,
                minSpeechDuration = 0.25f,
                windowSize = VAD_WINDOW,
                // Latency cap: even continuous speech commits a preview line every ~8 s.
                maxSpeechDuration = 8f,
            ),
            sampleRate = SAMPLE_RATE,
        ),
    )

    /** Build the recognizer's model config for the spec's family (Whisper today; Moonshine is the
     *  scaffolded low-RAM fallback, dormant until device-validated — see ModelCatalog.moonshineBaseEn). */
    private fun buildModelConfig(spec: AsrModelSpec): OfflineModelConfig {
        fun path(role: ModelFileRole) = modelStore.localFile(spec, spec.file(role)).absolutePath
        val threads = capabilities.asrThreadCount
        return when (spec.family) {
            ModelFamily.WHISPER -> OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = path(ModelFileRole.ENCODER),
                    decoder = path(ModelFileRole.DECODER),
                    language = spec.languageParam,
                    task = "transcribe",
                ),
                tokens = path(ModelFileRole.TOKENS),
                modelType = "whisper",
                numThreads = threads,
            )
            ModelFamily.MOONSHINE -> OfflineModelConfig(
                moonshine = OfflineMoonshineModelConfig(
                    preprocessor = path(ModelFileRole.PREPROCESSOR),
                    encoder = path(ModelFileRole.ENCODER),
                    uncachedDecoder = path(ModelFileRole.UNCACHED_DECODER),
                    cachedDecoder = path(ModelFileRole.CACHED_DECODER),
                ),
                tokens = path(ModelFileRole.TOKENS),
                modelType = "moonshine",
                numThreads = threads,
            )
            ModelFamily.STREAMING_ZIPFORMER ->
                // Online transducer — used only by the live-preview path (LiveTranscriber), never
                // the offline pass. Reaching here means a streaming spec was mis-routed as the model.
                error("streaming model ${spec.id} is live-preview only, not for the offline pass")
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val VAD_WINDOW = 512
        const val DECODE_SHARE = 0.10f
    }
}
